package zyu19.libs.action.chain.config;

/**
 * @author Zhongzhi Yu
 *         Created on 7/24/2015.
 */
public interface FakePromise <ThisType extends FakePromise<?>> {
	ThisType start();

	// The following function must not be declared.
	// Because onSuccess handler should be a final field in implementations.
	//		<AnsType> ThisType done(Consumer<AnsType> onSuccess);

	ThisType fail(Consumer<ErrorHolder> onFailure);
	<In, Out> ThisType then(boolean runOnWorkerThread, PureAction<In, Out> action);
	<In, Out> ThisType netThen(PureAction<In, Out> action);
	<In, Out> ThisType uiThen(PureAction<In, Out> action);
}
