package zyu19.libs.action.chain;

import zyu19.libs.action.chain.callbacks.NiceConsumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.callbacks.ChainEditor;
import zyu19.libs.action.chain.config.ChainStyle;
import zyu19.libs.action.chain.config.ThreadPolicy;

/**
 * For usages of this class, please refer to javadoc of ChainStyle and ErrorHolder.
 * <p>
 * This class provides the major functions of this library. It helps programmers
 * organize network "actions" in Android and other similar platforms with restrictions
 * about network operations on UI thread.
 * <p>
 * This class implements ChainStyle, so that programmers can choose the correct
 * thread for every "action" they add to the ActionChain, and so that they can avoid
 * callback hell.
 * <p>
 * When an "action" throws any Exception, this class will provide a ErrorHolder through the onFailure callback,
 * so that programmers can recover a set of "actions" from an Exception efficiently.
 * @author Zhongzhi Yu
 * @see AbstractActionChain
 * @see ChainStyle
 * @see ErrorHolder 
 * 
 * @version 0.3
 *
 */
public class ActionChain extends AbstractActionChain<ActionChain> {

	public ActionChain(ThreadPolicy threadPolicy) {
		super(threadPolicy);
	}
	
	public ActionChain(ThreadPolicy threadPolicy, NiceConsumer<ErrorHolder> onFailure) {
		super(threadPolicy, onFailure);
	}

	/**
	 * @deprecated Use of this function is now discouraged. <br>
	 *   The editor of the chain might change the default exception handler, and has to change the i/o flow among
	 *   current chain's PureActions.
	 *   <br>
	 *
	 *   These problems can be avoid by returning a new ActionChain
	 *   inside a PureAction. This works exactly the same as the way a Promise wait for another promise if that second
	 *   promise was returned inside the first promise's actions.
     */
	@Deprecated
	public ActionChain use(ChainEditor editor) {
		editor.edit(this);
		return this;
	}
	
}
