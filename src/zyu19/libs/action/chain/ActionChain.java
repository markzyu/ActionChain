package zyu19.libs.action.chain;

import zyu19.libs.action.chain.config.Consumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ThreadPolicy;

public class ActionChain extends AbstractActionChain<ActionChain> {

	public ActionChain(ThreadPolicy threadPolicy, Consumer<?> onSuccess) {
		super(threadPolicy, onSuccess);
	}
	
	public ActionChain(ThreadPolicy threadPolicy, Consumer<?> onSuccess, Consumer<ErrorHolder> onFailure) {
		super(threadPolicy, onSuccess, onFailure);
	}
	
}
