package zyu19.libs.action.chain.config;

/**
 * @author Zhongzhi Yu
 *         Created on 5/15/2015.
 */
public interface ThreadChanger {
	void runCallbackOnWantedThread(Runnable runnable);
}
