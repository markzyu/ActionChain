package zyu19.libs.action.chain;

import java.util.*;

import zyu19.libs.action.chain.config.DotAll;
import zyu19.libs.action.chain.config.NiceConsumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ThreadPolicy;

/**
 * This class is a helper class used by AbstractActionChain
 * <p>
 * It is actually the core of this library.
 * <p>
 * Separated as a helper class on 8/8/2015.
 *
 * @author Zhongzhi Yu
 * @version 0.3
 */
public class ReadOnlyChain implements ErrorHolder {

    //---------------------- ErrorHolder interface -------------------------
    private Exception mCause = null;
    private int mCauseLink = -1;

    @Override
    public final Exception getCause() {
        return mCause;
    }

    @Override
    public final void retry() {
        iterate();
    }

    //---------------------- Executor functions -------------------------
    private int mNextAction = 0;
    private Object mLastActionOutput = null;
    private boolean isOnSuccessCalled = false;
    private boolean executionFinished = false;

    private final ArrayList<ChainLink<?, ?>> mActionSequence = new ArrayList<>();
    private final ThreadPolicy mThreadPolicy;
    private NiceConsumer mOnSuccess;

    private Integer numPendingSubChains = null;

    /**
     * Constructor of ReadOnlyChain.
     *
     * @param actionSequence The array of action configurations to execute. The constructor will make a copy of this array.
     * @param onSuccess      The callback to notify when all actions finished without Exception.
     * @param threadPolicy   For the usages of this object, please refer to the javadoc of threadPolicy.
     */
    public ReadOnlyChain(ArrayList<ChainLink<?, ?>> actionSequence, NiceConsumer<?> onSuccess, ThreadPolicy threadPolicy) {
        mActionSequence.addAll(actionSequence);
        mThreadPolicy = threadPolicy;
        mOnSuccess = onSuccess;
    }

    /**
     * This function must be called within a <strong>synchronzied</strong> block!!!
     *
     * @return true if this ReadOnlyChain has reached its end and should exit.
     */
    private final boolean isIterationOver() {
        if (isOnSuccessCalled)
            return true;
        else if (mNextAction >= mActionSequence.size()) {
            mThreadPolicy.switchAndRun(mOnSuccess, mLastActionOutput);
            isOnSuccessCalled = true;
            return true;
        } else return false;
    }

    private final NiceConsumer<ThreadPolicy> mIterator = threadPolicy -> {
        synchronized (ReadOnlyChain.this) {
            if (isIterationOver()) {
                executionFinished = true;
                return;
            }
            ChainLink action = mActionSequence.get(mNextAction);
            try {
                mLastActionOutput = action.pureAction.process(mLastActionOutput);
                Set<ReadOnlyChain> filteredTargets = new HashSet<>();
                HashMap<ReadOnlyChain, Integer> positions = new HashMap<>();

                // If our outer handler is not null, we use errHandlersToRun otherwise we use errorsNotHandled
                List<Runnable> errHandlersToRun = new ArrayList<>();
                List<Exception> errorsNotHandled = new ArrayList<>();

                boolean replaceOutputWithTarget = false;
                final boolean shouldUnpackTargetsList[] = new boolean[]{false};
                List<Object> targets;

                if (mLastActionOutput != null && DotAll.class.isAssignableFrom(mLastActionOutput.getClass())) {
                    // Version 0.4: support waiting for ActionChain.all() (this is the point of using .all()...)
                    targets = ((DotAll) mLastActionOutput).objects;
                    for(int i = 0; i < targets.size(); i++) {
                        Object obj = targets.get(i);
                        if(obj instanceof ReadOnlyChain) {
                            filteredTargets.add((ReadOnlyChain) obj);
                            positions.put((ReadOnlyChain) obj, i);
                        }
                    }

                    filteredTargets.remove(this);
                    replaceOutputWithTarget = true;
                }
                else if (mLastActionOutput != null && ReadOnlyChain.class.isAssignableFrom(mLastActionOutput.getClass()) && mLastActionOutput != this) {
                    // Version 0.3: support waiting for inner ActionChains
                    // The returned ReadOnlyChain is detected here
                    replaceOutputWithTarget = true;
                    shouldUnpackTargetsList[0] = true;
                    targets = Arrays.asList((ReadOnlyChain) mLastActionOutput);
                    filteredTargets.add((ReadOnlyChain) mLastActionOutput);
                    positions.put((ReadOnlyChain) mLastActionOutput, 0);
                } else targets = Arrays.asList();

                numPendingSubChains = filteredTargets.size();

                // wait for all ReadOnlyChains in filteredTargets
                for(ReadOnlyChain that : filteredTargets) {
                    synchronized (that) {
                        // Pending, Success, Failed w/ handler in progress, Failed & finished
                        boolean discardThatChain = false;
                        boolean runErrorHolderOnThat = false;
                        if (that.executionFinished) {
                            if (that.mCause != null) {
                                // in this case, that chain has been stuck in error handling

                                runErrorHolderOnThat = that.mActionSequence.get(that.mCauseLink).errorHandler == null;
                            } else {
                                // that chain has successfully finished
                                mLastActionOutput = that.mLastActionOutput;
                                discardThatChain = true;
                            }
                        } else {
                            // it's not possible to have a non-null mCause here.
                            // thus we can PAUSE this chain
                        }
                        if (!discardThatChain) {
                            // If not discarding that chain, we PAUSE this chain, add callback into that chain
                            //   which will eventually restart our chain.

                            // Firstly set UNCAUGHT error holders to current error holder
                            if (action.errorHandler != null) for (int i = 0; i < that.mActionSequence.size(); i++)
                                if (that.mActionSequence.get(i).errorHandler == null)
                                    that.mActionSequence.get(i).errorHandler = action.errorHandler;

                            // Add the callback
                            final NiceConsumer wrapped = that.mOnSuccess;
                            final int resumePoint = mNextAction + 1;        // the resume point is the same for all subChains
                            NiceConsumer wrapper = input -> {
                                if(wrapped != null)
                                    wrapped.consume(input);

                                // resume current chain. Detect count of finished actions here.
                                synchronized (ReadOnlyChain.this) {
                                    int pos = positions.get(that);
                                    targets.set(pos, input);
                                    ReadOnlyChain.this.numPendingSubChains --;
                                    if(ReadOnlyChain.this.numPendingSubChains == 0) {
                                        if(shouldUnpackTargetsList[0])
                                            ReadOnlyChain.this.mLastActionOutput = targets.get(0);
                                        else ReadOnlyChain.this.mLastActionOutput = targets;
                                        ReadOnlyChain.this.mNextAction = resumePoint;
                                        ReadOnlyChain.this.iterateNoLock();
                                    }
                                }
                            };
                            that.mOnSuccess = wrapper;

                            // Call error holder to restart that chain if necessary (and if wanted)
                            if (runErrorHolderOnThat) {
                                that.mActionSequence.get(that.mCauseLink).errorHandler = action.errorHandler;
                                if(action.errorHandler != null)
                                    errHandlersToRun.add(() -> action.errorHandler.consume(that));
                                else errorsNotHandled.add(that.mCause);
                            }

                        } else {
                            // We discard that chain only if it's successful (see code above)
                            // so we can directly replace the element in list
                            int pos = positions.get(that);
                            targets.set(pos, that.mLastActionOutput);
                            // If we discard that chain, we immediately decrease the numPendingSubChains counter here
                            numPendingSubChains --;
                        }
                    }
                }

                if(errorsNotHandled.size() > 0)
                    throw new ExceptionList(errorsNotHandled);

                // run all error handlers to determine whether to resume the subChain
                threadPolicy.switchAndRun(() -> {
                    // if both chains do not have error handlers, we can't even reach this line.
                    for(Runnable runnable : errHandlersToRun)
                        runnable.run();
                });

                if(numPendingSubChains != null && numPendingSubChains > 0) {
                    // Finally PAUSE this chain.
                    mNextAction = Integer.MAX_VALUE;
                    return;
                } else {
                    if(replaceOutputWithTarget) {
                        if(shouldUnpackTargetsList[0])
                            mLastActionOutput = targets.get(0);
                        else mLastActionOutput = targets;
                    }
                }

            } catch (Exception err) {
                executionFinished = true;
                mCause = err;
                mCauseLink = mNextAction;
                if(action.errorHandler == null) {
                    printUncaughtEx(err);
                }
                threadPolicy.switchAndRun(action.errorHandler, ReadOnlyChain.this);
                return;
            }
            mNextAction++;
            iterate();
        }
    };

    public static void printUncaughtEx(Exception exception) {
        // Because some platforms do not allow throwing Exceptions to Main Thread,
        //      This is all we could do to help with your debugging.
        System.err.print("UNHANDLED Exception in ActionChain:");
        exception.printStackTrace();
    }

    private final void iterate() {
        synchronized (ReadOnlyChain.this) {
            iterateNoLock();
        }
    }

    private final void iterateNoLock() {
        mCause = null;
        mCauseLink = -1;
        executionFinished = false;
        if (isIterationOver()) {
            executionFinished = true;
            return;
        }
        ChainLink action = mActionSequence.get(mNextAction);
        callIteratorOnProperThread(action);
    }

    protected void callIteratorOnProperThread(ChainLink actionConfig) {
        if (actionConfig.runOnWorkerThread)
            mThreadPolicy.runWorker(() -> mIterator.consume(mThreadPolicy));
        else mThreadPolicy.switchAndRun(mIterator, mThreadPolicy);
    }

    final void start() {
        iterate();
    }
}
