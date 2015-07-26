package zyu19.libs.action.chain.config;

/**
 * @author Zhongzhi Yu
 *         Created on 7/21/2015.
 */
public class ActionConfig<In, Out> {
	final public PureAction<In, Out> pureAction;
	final public Consumer<ErrorHolder> errorHandler;
	final public boolean runOnWorkerThread;

	public ActionConfig(PureAction<In, Out> pureAction, Consumer<ErrorHolder> errorHandler, boolean runOnWorkerThread) {
		this.pureAction = pureAction;
		this.errorHandler = errorHandler;
		this.runOnWorkerThread = runOnWorkerThread;
	}
}
