package zyu19.libs.action.chain;

import zyu19.libs.action.chain.config.Consumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.PureAction;


/**
 * This class is a helper class used by AbstractActionChain 
 * <p>
 * Created on 7/21/2015.
 * <p>
 * Changed to be a package class on 8/8/2015 (v0.2)
 * @author Zhongzhi Yu 
 * 
 * @version 0.2
 */
class ChainLink<In, Out> {
	final public PureAction<In, Out> pureAction;
	final public Consumer<ErrorHolder> errorHandler;
	final public boolean runOnWorkerThread;

	public ChainLink(PureAction<In, Out> pureAction, Consumer<ErrorHolder> errorHandler, boolean runOnWorkerThread) {
		this.pureAction = pureAction;
		this.errorHandler = errorHandler;
		this.runOnWorkerThread = runOnWorkerThread;
	}
}
