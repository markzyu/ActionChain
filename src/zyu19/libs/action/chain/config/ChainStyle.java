package zyu19.libs.action.chain.config;

/**
 * This interface restricts ActionChain to avoid callback hell, and to enable
 * the users of this library to choose a thread for every "action" (or 'PureAction'
 * object).
 * <p>
 * Although this looks like Promise, it does NOT follow the standard of Promise-Then.
 * <p>
 * Created on 7/24/2015.
 * @author Zhongzhi Yu 
 * 
 * @version 0.2
 */
public interface ChainStyle <ThisType extends ChainStyle<?>> {
	/**
	 *  Start running the 'PureAction' objects in FakePromise.
	 *  <p>
	 *  Your actions will be copied to another object so that
	 *  you can call clear() immediately after start().
	 * @param onSuccess if not null, it will be called after all actions finish
	 * without Exception.
	 * @return this object, thus enabling method chaining.
	 */
	ThisType start(Consumer<?> onSuccess);

	/**
	 * Clear all actions. You can call this function after start() so as to arrange a
	 * new sequence of actions.
	 * @param onFailure the new onFailure callback for actions added afterwards. It
	 * can also be set to null.
	 * @return this object, thus enabling method chaining.
	 */
	ThisType clear(Consumer<ErrorHolder> onFailure);

	/**
	 * set the onFailure callback for 'PureAction' objects added subsequently
	 * @param onFailure a callback to be invoked when any Exception is thrown from "actions"
	 * (or 'PureAction' objects).
	 * @return this object, thus enabling method chaining.
	 */
	ThisType fail(Consumer<ErrorHolder> onFailure);
	
	/**
	 * Add a 'PureAction' object, or an "action", in this FakePromise.
	 * @param runOnWorkerThread If set to false, the action will run on the main thread (UI thread)
	 * specified by ThreadChanger. Otherwise the task will run on any other thread (worker thread).
	 * @return this object, thus enabling method chaining.
	 */
	<In, Out> ThisType then(boolean runOnWorkerThread, PureAction<In, Out> action);

	
	/**
	 * Add a 'PureAction' object, or an "action", in this FakePromise on the <strong>worker</strong> thread.
	 * @return this object, thus enabling method chaining.
	 */
	<In, Out> ThisType netThen(PureAction<In, Out> action);


	/**
	 * Add a 'PureAction' object, or an "action", in this FakePromise on the <strong>main (UI)</strong> thread.
	 * @return this object, thus enabling method chaining.
	 */
	<In, Out> ThisType uiThen(PureAction<In, Out> action);
}
