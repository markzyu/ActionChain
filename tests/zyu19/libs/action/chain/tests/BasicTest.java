package zyu19.libs.action.chain.tests;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.config.*;

public class BasicTest {
	BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	ThreadPolicy threadPolicy = new ThreadPolicy(new ThreadChanger() {
		@Override
		public void runCallbackOnMainThread(Runnable runnable) {
			queue.add(runnable);
		}
	}, Executors.newCachedThreadPool());
	ActionChain chain = new ActionChain(threadPolicy);
	
	final Thread mainThread = Thread.currentThread();
	
	public boolean isMainThread() {
		return Thread.currentThread() == mainThread;
	}
	
	@Test
	public void TestNoException_EveryCallbackIsInvokedInCorrectOrder() {
		final StringBuilder ansBuilder = new StringBuilder();
		Random random = new Random();
		
		class TestAction implements PureAction<Integer, Integer> {
			public int name;
			
			public TestAction(int name) {
				this.name = name;
			}
			
			public Integer process(Integer input) throws Exception {
				ansBuilder.append(String.valueOf(name));
				return input + 1;
			}
		}
		
		chain.clear(new Consumer<ErrorHolder>() {
			public void consume(ErrorHolder arg) {
				Assert.fail(arg.getCause().toString());
			}
		});
		
		//----------------------------------
		
		String correctAns = "";
		int temp;

		chain.then(false, new PureAction<Object, Integer>() {
			public Integer process(Object input) throws Exception {
				return 0;
			}
		});

		final int testLength = 100;
		
		for (int i = 0; i < testLength; i++) {
			temp = random.nextInt();
			correctAns += String.valueOf(temp);
			chain.then(random.nextBoolean(), new TestAction(temp));
		}
		
		// onSuccess should break the loop (see the while loop afterwards)
		final AtomicBoolean finished = new AtomicBoolean(false);
		final Integer[] returnedPipeOutput = new Integer[1];
		final int lastTest = random.nextInt();
		correctAns += String.valueOf(lastTest);
		chain.start(new Consumer<Integer>() {
			public void consume(Integer arg) {
				ansBuilder.append(String.valueOf(lastTest));
				returnedPipeOutput[0] = arg;
				finished.set(true);
			}
		});
		
		// Simulate the Android Looper class
		while (!finished.get())
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		Assert.assertTrue(ansBuilder.toString() + " != " + correctAns, ansBuilder.toString().equals(correctAns));
		Assert.assertTrue("The IO pipe of Action Chain was messed up: " + (returnedPipeOutput[0]==null ? "null" : returnedPipeOutput[0])
				, returnedPipeOutput[0] != null && returnedPipeOutput[0] == testLength);
	}
	
	@Test
	public void TestNoException_ClearFunctionWorks() {
		chain = new ActionChain(threadPolicy, new Consumer<ErrorHolder>() {
			public void consume(ErrorHolder arg) {
				Assert.fail(arg.getCause().toString());
			}
		});
		final AtomicBoolean shouldFail = new AtomicBoolean(false);
		final AtomicBoolean finished = new AtomicBoolean(false);
		chain.then(false, new PureAction<Object, Object>(){
			public Object process(Object input) throws Exception {
				if(shouldFail.get())
					Assert.fail("chain.clear() did not remove all previous configurations!");
				return null;
			}
		}).start(new Consumer<Object>() {
			public void consume(Object arg) {
				finished.set(true);
			}
		});
		
		// Simulate the Android Looper class
		while (!finished.get())
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		// ----------- clear chain and start a new one --------------
		
		chain.clear(new Consumer<ErrorHolder>() {
			public void consume(ErrorHolder arg) {
				Assert.fail(arg.getCause().toString());
			}
		});
		shouldFail.set(true);
		finished.set(false);
		chain.start(new Consumer<Object>() {
			public void consume(Object arg) {
				finished.set(true);
			}
		});
		
		// Simulate the Android Looper class
		while (!finished.get())
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	@Test
	public void TestNoException_SwitchThreadCorrectly() {
		Random rand = new Random();
		
		chain.clear(new Consumer<ErrorHolder>() {
			public void consume(ErrorHolder arg) {
				Assert.fail(arg.getCause().toString());
			}
		});
		
		PureAction<Object, Object> shouldRunOnMainThread = new PureAction<Object, Object>() {
			public Object process(Object input) throws Exception {
				Assert.assertTrue(isMainThread());
				return null;
			}
		};
		
		PureAction<Object, Object> shouldRunOnWorkerThread = new PureAction<Object, Object>() {
			public Object process(Object input) throws Exception {
				Assert.assertTrue(!isMainThread());
				return null;
			}
		};
		
		// onSuccess should break the loop (see the while loop afterwards)
		final AtomicBoolean finished = new AtomicBoolean(false);
		Consumer<Object> onSuccess = new Consumer<Object>() {
			public void consume(Object arg) {
				finished.set(true);
			}
		};
		
		for(int i=0; i<100; i++) {
			if(rand.nextBoolean()) {
				chain.then(true, shouldRunOnWorkerThread);
			} else chain.then(false, shouldRunOnMainThread);
		}
		chain.start(onSuccess);
		
		while (!finished.get())
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	@Test
	public void TestNoException_HighlySynchronized() {
		final StringBuilder ansBuilder = new StringBuilder();
		final Integer[] notAtomicBlock = new Integer[] {0};
		Random random = new Random();
		
		class TestAction implements PureAction<Object, Object> {
			public boolean plus;
			
			public TestAction(boolean plus) {
				this.plus = plus;
			}
			
			public Object process(Object input) throws Exception {
				int read = notAtomicBlock[0];
				for(int i=0; i<100; i++)
					ansBuilder.append(read);
				if(plus)
					read++;
				else read--;
				notAtomicBlock[0] = read;
				return null;
			}
		}
		
		chain.clear(new Consumer<ErrorHolder>() {
			public void consume(ErrorHolder arg) {
				Assert.fail(arg.getCause().toString());
			}
		});
		
		//----------------------------------
		
		StringBuilder correctAns = new StringBuilder();
		boolean temp;
		int current = 0;
		
		for (int i = 0; i < 100; i++) {
			temp = random.nextBoolean();
			for(int j=0; j<100; j++)
				correctAns.append(current);
			if(temp)
				current++;
			else current--;
			chain.then(random.nextBoolean(), new TestAction(temp));
		}
		
		// onSuccess should break the loop (see the while loop afterwards)
		final AtomicBoolean finished = new AtomicBoolean(false);
		chain.start(new Consumer<Object>() {
			public void consume(Object arg) {
				finished.set(true);
			}
		});
		
		// Simulate the Android Looper class
		while (!finished.get())
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		Assert.assertTrue(ansBuilder.toString() + " != " + correctAns.toString(), ansBuilder.toString().equals(correctAns.toString()));
	}
	
}