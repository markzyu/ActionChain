package zyu19.libs.action.chain.tests;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.config.PureAction;
import zyu19.libs.action.chain.config.ThreadChanger;
import zyu19.libs.action.chain.config.*;
import zyu19.libs.action.chain.config.NiceConsumer;

/**
 * Created by Zhongzhi Yu on 2/28/16.
 * @author Zhongzhi Yu
 *
 * @version 0.4
 */
public class ReturnedChain {
	BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	ThreadPolicy threadPolicy = new ThreadPolicy(runnable -> queue.add(runnable), Executors.newCachedThreadPool());
	ActionChain chain = new ActionChain(threadPolicy);

	Thread mainThread = Thread.currentThread();

	public void updateMainThread(Thread thread) {mainThread = thread;}
	public boolean isMainThread() {
		return Thread.currentThread() == mainThread;
	}

	@Test(timeout = 2000)
	public void TestNoException_EveryCallbackIsInvokedInCorrectOrder() {
		final int testLength = 100;
		final int subTestLength = 10;
		final StringBuilder ansBuilder = new StringBuilder();
		String correctAns = "";
		Random random = new Random();

		chain.clear(errorHolder -> queue.add(() -> Assert.fail(errorHolder.getCause().toString()))
		).netThen(() -> 0);

		for (int i = 0; i < testLength; i++) {
			final int thisI = i;
			final int temp = random.nextInt();
			correctAns += "Start" + String.valueOf(temp) + "Sub";
			for(int j = 0; j <subTestLength; j++)
				correctAns += i + ", " + j + ". ";
			correctAns += "End";

			final Integer[] inputRecord = new Integer[1];
			chain.then(random.nextBoolean(), (Integer input) -> {
				System.out.println("!! In chain " + thisI);
				ansBuilder.append("Start");
				ansBuilder.append(temp);
				ansBuilder.append("Sub");
				ActionChain subChain = new ActionChain(threadPolicy);
				for (int j = 0; j < subTestLength; j++) {
					final int thisJ = j;
					subChain.then(random.nextBoolean(), obj -> {
						System.out.println("In subchain" + thisJ + ".");
						ansBuilder.append(thisI);
						ansBuilder.append(", ");
						ansBuilder.append(thisJ);
						ansBuilder.append(". ");
					});
				}
				inputRecord[0] = input;
				return subChain.start(obj -> System.out.println("INNER SUCCESS"));
			}).uiThen(obj -> {
				ansBuilder.append("End");
				return inputRecord[0] + 1;
			});
		}

		// onSuccess should break the loop (see the while loop afterwards)
		final AtomicBoolean finished = new AtomicBoolean(false);
		final Integer[] returnedPipeOutput = new Integer[1];
		final int lastTest = random.nextInt();
		correctAns += String.valueOf(lastTest);

		updateMainThread(Thread.currentThread());

		// collect output
		chain.start((Integer arg) -> {
			System.out.println("ON SUCCESS");
			ansBuilder.append(String.valueOf(lastTest));
			returnedPipeOutput[0] = arg;
			finished.set(true);
		});

		// Simulate the Android Looper class
		while (!finished.get())
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		Assert.assertTrue(ansBuilder.toString() + " != " + correctAns, ansBuilder.toString().equals(correctAns));
		Assert.assertTrue("The IO pipe of Action Chain was messed up: " + (returnedPipeOutput[0] == null ? "null" : returnedPipeOutput[0])
				, returnedPipeOutput[0] != null && returnedPipeOutput[0] == testLength);
	}
}
