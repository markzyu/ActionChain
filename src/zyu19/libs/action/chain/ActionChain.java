package zyu19.libs.action.chain;

import zyu19.libs.action.chain.config.Consumer;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ChainEditor;
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
 * @see ChainStyle
 * @see ErrorHolder 
 * 
 * @version 0.1
 *
 */
public class ActionChain extends AbstractActionChain<ActionChain> {

	public ActionChain(ThreadPolicy threadPolicy) {
		super(threadPolicy);
	}
	
	public ActionChain(ThreadPolicy threadPolicy, Consumer<ErrorHolder> onFailure) {
		super(threadPolicy, onFailure);
	}
	
	public ActionChain use(ChainEditor editor) {
		editor.edit(this);
		return this;
	}
	
}
