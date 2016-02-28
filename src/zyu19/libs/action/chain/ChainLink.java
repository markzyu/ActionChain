package zyu19.libs.action.chain;

import zyu19.libs.action.chain.callbacks.NiceConsumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.callbacks.PureAction;


/**
 * This class is a helper class used by AbstractActionChain 
 * <p>
 * Created on 7/21/2015.
 * <p>
 * Changed to be a package class on 8/8/2015 (v0.2)
 * @author Zhongzhi Yu 
 * 
 * @version 0.3
 */
class ChainLink<In, Out> {
	final public PureAction<In, Out> pureAction;
	final public boolean runOnWorkerThread;
	public NiceConsumer<ErrorHolder> errorHandler;

	public ChainLink(PureAction<In, Out> pureAction, NiceConsumer<ErrorHolder> errorHandler, boolean runOnWorkerThread) {
		this.pureAction = pureAction;
		this.errorHandler = errorHandler;
		this.runOnWorkerThread = runOnWorkerThread;
	}
}
