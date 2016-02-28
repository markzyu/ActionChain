package zyu19.libs.action.chain.callbacks;

/**
 * This interface's instances are meant to be used as callbacks, in the form of
 * lambda expressions or anonymous classes.
 * <p>
 * Implementation of this interface should run the runnable on the main/UI thread
 * specified by the platform you are using.
 * <p>
 * For example, on Android:<br>
 * <code>ThreadChanger threadChanger = runnable -&gt; XXXActivity.this.runOnUiThread(runnable);</code>
 * <p>
 * Created on 5/15/2015.
 * @author Zhongzhi Yu 
 * 
 * @version 0.1
 */
public interface ThreadChanger {
	void runCallbackOnMainThread(Runnable runnable);
}
