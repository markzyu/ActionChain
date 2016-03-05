package zyu19.libs.action.chain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                List<Runnable> errHandlersToRun = new ArrayList<>();

                if (mLastActionOutput instanceof DotAll) {
                    // Version 0.4: support waiting for ActionChain.all() (this is the point of using .all()...)
                    List<Object> targets = ((DotAll) mLastActionOutput).objects;
                    for(Object obj : targets) {
                        if(obj instanceof ReadOnlyChain)
                            filteredTargets.add((ReadOnlyChain)obj);
                    }

                    filteredTargets.remove(this);
                }
                else if (mLastActionOutput instanceof ReadOnlyChain && mLastActionOutput != this) {
                    // Version 0.3: support waiting for inner ActionChains
                    // The returned ReadOnlyChain is detected here
                    filteredTargets.add((ReadOnlyChain) mLastActionOutput);
                }

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
                                    ReadOnlyChain.this.numPendingSubChains --;
                                    if(ReadOnlyChain.this.numPendingSubChains == 0) {
                                        ReadOnlyChain.this.mNextAction = resumePoint;
                                        ReadOnlyChain.this.iterateNoLock();
                                    }
                                }
                            };
                            that.mOnSuccess = wrapper;

                            // Call error holder to restart that chain if necessary (and if wanted)
                            if (runErrorHolderOnThat) {
                                that.mActionSequence.get(that.mCauseLink).errorHandler = action.errorHandler;
                                errHandlersToRun.add(() -> action.errorHandler.consume(that));
                            }

                        } else {
                            // If we discard that chain, we immediately decrease the numPendingSubChains counter here
                            numPendingSubChains --;
                        }
                    }
                }

                // run all error handlers to determine whether to resume the subChain
                threadPolicy.switchAndRun(() -> {
                    for(Runnable runnable : errHandlersToRun)
                        runnable.run();
                });

                if(numPendingSubChains != null && numPendingSubChains > 0) {
                    // Finally PAUSE this chain.
                    mNextAction = Integer.MAX_VALUE;
                    return;
                }

            } catch (Exception err) {
                executionFinished = true;
                mCause = err;
                mCauseLink = mNextAction;
                threadPolicy.switchAndRun(action.errorHandler, ReadOnlyChain.this);
                return;
            }
            mNextAction++;
            iterate();
        }
    };

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
