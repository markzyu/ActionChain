package zyu19.libs.action.chain;

import java.util.ArrayList;

import zyu19.libs.action.chain.config.ActionConfig;
import zyu19.libs.action.chain.config.Consumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.FakePromise;
import zyu19.libs.action.chain.config.PureAction;
import zyu19.libs.action.chain.config.ThreadPolicy;

public abstract class AbstractActionChain<ThisType extends AbstractActionChain<?>> implements ErrorHolder, FakePromise<ThisType> {

	//------------- public functions (ErrorHolder Interface) ----------------

	private Exception mCause;

	@Override
	public final Exception getCause() {
		return mCause;
	}

	@Override
	public final void retry() {
		iterate();
	}

	//------------- public functions (FakePromise Interface) ----------------

	@Override
	public final ThisType start() {
		clearIterationState();
		iterate();
		return (ThisType)this;
	}

	@Override
	public final ThisType fail(Consumer<ErrorHolder> onFailure) {
		mCurrentOnFailure = onFailure;
		return (ThisType)this;
	}

	@Override
	public <In, Out> ThisType then(boolean runOnWorkerThread, PureAction<In, Out> action) {
		mActionSequence.add(new ActionConfig(action, mCurrentOnFailure, runOnWorkerThread));
		return (ThisType)this;
	}

	@Override
	public final <In, Out> ThisType netThen(PureAction<In, Out> action) {
		mActionSequence.add(new ActionConfig(action, mCurrentOnFailure, true));
		return (ThisType)this;
	}

	@Override
	public final <In, Out> ThisType uiThen(PureAction<In, Out> action) {
		mActionSequence.add(new ActionConfig(action, mCurrentOnFailure, false));
		return (ThisType)this;
	}

	//------------- Constructors ----------------

	private final Consumer mOnSuccess;
	private Consumer<ErrorHolder> mCurrentOnFailure;

	private ArrayList<ActionConfig> mActionSequence = new ArrayList<>();

	protected final ThreadPolicy mThreadPolicy;
	public AbstractActionChain(ThreadPolicy threadPolicy, Consumer<?> onSuccess) {
		mThreadPolicy = threadPolicy;
		mOnSuccess = onSuccess;
	}

	public AbstractActionChain(ThreadPolicy threadPolicy, Consumer<?> onSuccess, Consumer<ErrorHolder> onFailure) {
		mThreadPolicy = threadPolicy;
		mOnSuccess = onSuccess;
		mCurrentOnFailure = onFailure;
	}

	//------------------------ [Private] Iteration State -------------------------------

	private int mNextAction;
	private Object mLastActionOutput;
	private boolean isOnSuccessCalled;

	private final void clearIterationState() {
		mNextAction = 0;
		mLastActionOutput = null;
		isOnSuccessCalled = false;
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

	//------------------------ [Private] iterate and iterator ---------------------------
	private final Consumer<ThreadPolicy> mIterator = new Consumer<ThreadPolicy>() {
		public void consume(ThreadPolicy threadPolicy) {
			synchronized (AbstractActionChain.this) {
				if (isIterationOver())
					return;
				ActionConfig action = mActionSequence.get(mNextAction);
				try {
					mLastActionOutput = action.pureAction.process(mLastActionOutput);
				} catch (Exception err) {
					mCause = err;
					threadPolicy.switchAndRun(action.errorHandler, AbstractActionChain.this);
					return;
				}
				mNextAction++;
				iterate();
			}
		}
	};

	private final void iterate() {
		synchronized (this) {
			if (isIterationOver())
				return;
			ActionConfig action = mActionSequence.get(mNextAction);
			callIteratorOnProperThread(action);
		}
	}

	//------------------------ policy ----------------------------


	/**
	 * This function decides which thread mIterator should run on. <br>
	 * Note: onSuccess and onFailure will always run on the thread selected by ThreadChanger, regardless
	 * of this function's configuration.
	 *
	 * @param task         the runnable to run. Directly run it on a proper thread.
	 * @param actionConfig the actionConfig you defined, which contains your customized clues.
	 */
	protected void callIteratorOnProperThread(ActionConfig actionConfig) {
		if (actionConfig.runOnWorkerThread)
			mThreadPolicy.runWorker(new Runnable() {
				@Override
				public void run() {
					mIterator.consume(mThreadPolicy);
				}
			});
		else mThreadPolicy.switchAndRun(mIterator, mThreadPolicy);
	}
}
