package zyu19.libs.action.chain;

import java.util.ArrayList;

import zyu19.libs.action.chain.config.Consumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ThreadPolicy;

/**
 * This class is a helper class used by AbstractActionChain 
 * <p>
 * It is actually the core of this library.
 * <p>
 * Separated as a helper class on 8/8/2015.
 * @author Zhongzhi Yu 
 * 
 * @version 0.2
 */
class ReadOnlyChain implements ErrorHolder {
	
	//---------------------- ErrorHolder interface -------------------------
	private Exception mCause;

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

	private final ArrayList<ChainLink<?,?>> mActionSequence;
	private final Consumer mOnSuccess;
	private final ThreadPolicy mThreadPolicy;

	/**
	 * 
	 * @param actionSequence
	 * @param onSuccess
	 * @param threadPolicy
	 */
	public ReadOnlyChain(ArrayList<ChainLink<?,?>> actionSequence, Consumer<?> onSuccess, ThreadPolicy threadPolicy) {
		mActionSequence = new ArrayList<ChainLink<?,?>>(actionSequence);
		mThreadPolicy = threadPolicy;
		mOnSuccess = onSuccess;
	}

	private final boolean isIterationOver() {
		if (isOnSuccessCalled)
			return true;
		else if (mNextAction >= mActionSequence.size()) {
			mThreadPolicy.switchAndRun(mOnSuccess, mLastActionOutput);
			isOnSuccessCalled = true;
			return true;
		} else return false;
	}

	private final Consumer<ThreadPolicy> mIterator = new Consumer<ThreadPolicy>() {
		public void consume(ThreadPolicy threadPolicy) {
			synchronized (ReadOnlyChain.this) {
				if (isIterationOver())
					return;
				ChainLink action = mActionSequence.get(mNextAction);
				try {
					mLastActionOutput = action.pureAction.process(mLastActionOutput);
				} catch (Exception err) {
					mCause = err;
					threadPolicy.switchAndRun(action.errorHandler, ReadOnlyChain.this);
					return;
				}
				mNextAction++;
				iterate();
			}
		}
	};

	private final void iterate() {
		synchronized (ReadOnlyChain.this) {
			if (isIterationOver())
				return;
			ChainLink action = mActionSequence.get(mNextAction);
			callIteratorOnProperThread(action);
		}
	}
	
	protected void callIteratorOnProperThread(ChainLink actionConfig) {
		if (actionConfig.runOnWorkerThread)
			mThreadPolicy.runWorker(new Runnable() {
				@Override
				public void run() {
					mIterator.consume(mThreadPolicy);
				}
			});
		else mThreadPolicy.switchAndRun(mIterator, mThreadPolicy);
	}
	
	public final void start() {
		iterate();
	}
}
