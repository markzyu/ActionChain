package zyu19.libs.action.chain.config;

import java.util.concurrent.ExecutorService;

/**
 * This class encapsulates Java.Util's ExecutorService to simplify the process to
 * run an asynchronous call on specified thread.
 * <p>
 * As a user of this library, you only have to know how to
 * get an instance of this class through its public constructors.
 * <p>
 * Created on 5/15/2015. <br>
 * Separated as a single file by Zhongzhi Yu on 5/17/2015,<br>
 * Hugely modified on 6/28/2015.
 * @author Zhongzhi Yu 
 * 
 * @version 0.1
 */
public class ThreadPolicy {
	private final ThreadChanger mThreadChanger;
	private final ExecutorService mExecutorService;

	/**
	 * One type of constructor.
	 * @param threadChanger A functor that tell the OS to run a Runnable on the main/UI thread.
	 * @param multiThreadConfig There are many different types of configuration.
	 * Please refer to java.util.concurrent.Executors.
	 * 
	 * @see java.util.concurrent.Executors
	 */
	public ThreadPolicy(ThreadChanger threadChanger, ExecutorService multiThreadConfig) {
		mExecutorService = multiThreadConfig;
		mThreadChanger = threadChanger;
	}

	/**
	 * Another type of constructor, where it's not necessary to provide a 'threadChanger'
	 * <p>
	 * If you use this constructor, FakePromise, and thus ActionChain, will always run all "actions"
	 * on the worker thread created by 'multiThreadConfig'
	 * @param multiThreadConfig There are many different types of configuration.
	 * Please refer to java.util.concurrent.Executors.
	 * 
	 * @see java.util.concurrent.Executors
	 */
	public ThreadPolicy(ExecutorService multiThreadConfig) {
		mExecutorService = multiThreadConfig;
		mThreadChanger = null;
	}

	public void runWorker(Runnable operation) {
		mExecutorService.submit(operation);
	}


	public <ArgType> void switchAndRun(final Consumer<ArgType> consumer, final ArgType arg) {
		if (consumer == null)
			return;
		if (mThreadChanger == null)
			consumer.consume(arg);
		else {
			mThreadChanger.runCallbackOnMainThread(new Runnable(){
				@Override
				public void run() {
					consumer.consume(arg);
				}
			});
		}
	}

	public void switchAndRun(Runnable runnable) {
		if (runnable == null)
			return;
		if (mThreadChanger == null)
			runnable.run();
		else {
			mThreadChanger.runCallbackOnMainThread(runnable);
		}
	}
}