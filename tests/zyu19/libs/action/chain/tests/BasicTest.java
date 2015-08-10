package zyu19.libs.action.chain.tests;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.*;

import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.config.*;

public class BasicTest {
	BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	ThreadPolicy threadPolicy = new ThreadPolicy(new ThreadChanger() {
		@Override public void runCallbackOnMainThread(Runnable runnable) {
			queue.add(runnable);
		}
	}, Executors.newCachedThreadPool());
	ActionChain chain = new ActionChain(threadPolicy);
	
	final Thread mainThread = Thread.currentThread();
	public boolean isMainThread() {
		return Thread.currentThread() == mainThread;
	}
	
	@Test
	public void TestSwitchThread() {
		PureAction shouldRunOnMainThread = new PureAction() {
			public Object process(Object input) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
		};
		//chain.fail(onFailure)
	}
}