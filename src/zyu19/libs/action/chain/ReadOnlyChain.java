package zyu19.libs.action.chain;

import java.util.ArrayList;

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
    private final NiceConsumer mOnSuccess;
    private final ThreadPolicy mThreadPolicy;

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
                if (mLastActionOutput instanceof ReadOnlyChain && mLastActionOutput != this) {
                    // Version 0.3: support waiting for inner ActionChains
                    synchronized (mLastActionOutput) {
                        // Pending, Success, Failed w/ handler in progress, Failed & finished
                        ReadOnlyChain that = (ReadOnlyChain) mLastActionOutput;
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
                            // thus we can directly discard this chain
                        }
                        if (!discardThatChain) {
                            // If not discarding that chain, we discard this chain, copy the remaining actions to that
                            //   chain, and RETURN.

                            // Firstly set UNCAUGHT error holders to current error holder
                            if (action.errorHandler != null) for (int i = 0; i < that.mActionSequence.size(); i++)
                                if (that.mActionSequence.get(i).errorHandler == null)
                                    that.mActionSequence.get(i).errorHandler = action.errorHandler;

                            // TODO: Then copy our actions to that chain
                            for (int i = mNextAction + 1; i < mActionSequence.size(); i++)
                                that.mActionSequence.add(mActionSequence.get(i));

                            // Finally discard this chain.
                            mNextAction = Integer.MAX_VALUE;

                            // Call error holder to restart that chain if necessary (and if wanted)
                            if (runErrorHolderOnThat) {
                                that.mActionSequence.get(that.mCauseLink).errorHandler = action.errorHandler;
                                that.mThreadPolicy.switchAndRun(action.errorHandler, that);
                            }
                            return;
                        }
                    }
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
