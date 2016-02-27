package zyu19.libs.action.chain;

import java.util.ArrayList;

import zyu19.libs.action.chain.config.Consumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ChainStyle;
import zyu19.libs.action.chain.config.PureAction;
import zyu19.libs.action.chain.config.ThreadPolicy;

/**
 * For usages of any implementation of this class, please refer to javadoc of ChainStyle and ErrorHolder.
 * <p>
 * Users of this library can extends this class to build their own versions of ActionChain.
 * <p>
 * This class contains every piece of fundamental code. Actually the ActionChain class is
 * just an empty shell that "extends AbstractActionChain&lt;ActionChain&gt;".
 * @author Zhongzhi Yu
 *
 * @param <ThisType> The non-abstract Class that eventually extends this Abstract Class.
 * This parameter is required in order to provide method chaining.
 *  
 * @see ActionChain 
 * 
 * @version 0.3
 */
public abstract class AbstractActionChain<ThisType extends AbstractActionChain<?>> implements ChainStyle<ThisType> {

	//------------- public functions (FakePromise Interface) ----------------

	@Override
	public final ReadOnlyChain start(Consumer<?> onSuccess) {
		ReadOnlyChain chain = new ReadOnlyChain(mActionSequence, onSuccess, mThreadPolicy);
		chain.start();
		return chain;
	}
	
	@Override
	public ThisType clear(Consumer<ErrorHolder> onFailure) {
		mCurrentOnFailure = onFailure;
		mActionSequence.clear();
		return (ThisType)this;
	}

	@Override
	public final ThisType fail(Consumer<ErrorHolder> onFailure) {
		mCurrentOnFailure = onFailure;
		return (ThisType)this;
	}

	@Override
	public <In, Out> ThisType then(boolean runOnWorkerThread, PureAction<In, Out> action) {
		mActionSequence.add(new ChainLink<In,Out>(action, mCurrentOnFailure, runOnWorkerThread));
		return (ThisType)this;
	}

	@Override
	public final <In, Out> ThisType netThen(PureAction<In, Out> action) {
		mActionSequence.add(new ChainLink<In,Out>(action, mCurrentOnFailure, true));
		return (ThisType)this;
	}

	@Override
	public final <In, Out> ThisType uiThen(PureAction<In, Out> action) {
		mActionSequence.add(new ChainLink<In,Out>(action, mCurrentOnFailure, false));
		return (ThisType)this;
	}

	//------------- Constructors ----------------

	private Consumer<ErrorHolder> mCurrentOnFailure;
	private ArrayList<ChainLink<?,?>> mActionSequence = new ArrayList<>();
	protected final ThreadPolicy mThreadPolicy;
	
	public AbstractActionChain(ThreadPolicy threadPolicy) {
		mThreadPolicy = threadPolicy;
	}

	public AbstractActionChain(ThreadPolicy threadPolicy, Consumer<ErrorHolder> onFailure) {
		mThreadPolicy = threadPolicy;
		mCurrentOnFailure = onFailure;
	}
}