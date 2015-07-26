package zyu19.libs.action.chain.config;

import java.util.concurrent.ExecutorService;

/**
 * This class encapsulates Guava's ListeningExecutorService to simply the process to
 * create an asynchronous call. Full lambda support for use with Java8 or Retro-lambda.
 *
 * @author ZhongzhiYu
 *         Created on 5/15/2015.
 *         Separated as a single file by Zhongzhi Yu on 5/17/2015,
 *         Largely modified on 6/28/2015.
 */
public class ThreadPolicy {
	private final ThreadChanger mThreadChanger;
	private final ExecutorService mExecutorService;

	public ThreadPolicy(ThreadChanger threadChanger, ExecutorService multiThreadConfig) {
		mExecutorService = multiThreadConfig;
		mThreadChanger = threadChanger;
	}

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
			mThreadChanger.runCallbackOnWantedThread(new Runnable(){
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
			mThreadChanger.runCallbackOnWantedThread(runnable);
		}
	}
}