package zyu19.libs.action.chain.tests;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;

import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.config.*;

/**
 * @version 0.4
 */
public class BasicTest {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
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
        final StringBuilder ansBuilder = new StringBuilder();
        String correctAns = "";
        Random random = new Random();

        chain.clear(errorHolder -> Assert.fail(errorHolder.getCause().toString())
        ).netThen(() -> 0);

        for (int i = 0; i < testLength; i++) {
            final int temp = random.nextInt();
            correctAns += String.valueOf(temp);
            chain.then(random.nextBoolean(), (Integer input) -> {
                ansBuilder.append(String.valueOf(temp));
                return input + 1;
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
            ansBuilder.append(String.valueOf(lastTest));
            returnedPipeOutput[0] = arg;
            finished.set(true);
        });

        // Simulate the Android Looper class
        while (!finished.get() || !queue.isEmpty())
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        Assert.assertTrue(ansBuilder.toString() + " != " + correctAns, ansBuilder.toString().equals(correctAns));
        Assert.assertTrue("The IO pipe of Action Chain was messed up: " + (returnedPipeOutput[0] == null ? "null" : returnedPipeOutput[0])
                , returnedPipeOutput[0] != null && returnedPipeOutput[0] == testLength);
    }

    @Test(timeout = 2000)
    public void TestNoException_ClearFunctionWorks() {
        final AtomicBoolean shouldFail = new AtomicBoolean(false);
        final AtomicBoolean finished = new AtomicBoolean(false);

        chain = new ActionChain(threadPolicy, failure -> Assert.fail(failure.getCause().toString()));
        chain.netThen(input -> {
            if (shouldFail.get())
                queue.add(() -> Assert.fail("chain.clear() did not remove all previous configurations!"));
            return null;
        }).start(arg -> finished.set(true));

        // Simulate the Android Looper class
        while (!finished.get() || !queue.isEmpty())
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        // ----------- clear chain and start a new one --------------
        shouldFail.set(true);
        finished.set(false);

        updateMainThread(Thread.currentThread());

        chain.clear(arg -> Assert.fail(arg.getCause().toString()));
        chain.start(arg -> finished.set(true));

        // Simulate the Android Looper class
        while (!finished.get() || !queue.isEmpty())
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    @Test(timeout = 2000)
    public void TestNoException_SwitchThreadCorrectly() {
        Random rand = new Random();

        // onSuccess should break the loop (see the while loop afterwards)
        final AtomicBoolean finished = new AtomicBoolean(false);

        chain.clear(arg -> Assert.fail(arg.getCause().toString()));

        for (int i = 0; i < 100; i++) {
            if (rand.nextBoolean())
                chain.uiThen(input -> {
                    final boolean testResult = isMainThread();
                    queue.add(() -> Assert.assertTrue(testResult));
                });
            else chain.netThen(input -> {
                final boolean testResult = !isMainThread();
                queue.add(() -> Assert.assertTrue(testResult));
            });
        }
        chain.start(arg -> finished.set(true));

        updateMainThread(Thread.currentThread());

        while (!finished.get() || !queue.isEmpty())
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    @Test(timeout = 2000)
    public void TestNoException_HighlySynchronized() {
        final StringBuilder ansBuilder = new StringBuilder();
        final Integer[] notAtomicBlock = new Integer[]{0};
        StringBuilder correctAns = new StringBuilder();
        Random random = new Random();
        int current = 0;

        chain.clear(errorHolder -> Assert.fail(errorHolder.getCause().toString()));

        for (int i = 0; i < 100; i++) {
            final boolean temp = random.nextBoolean();
            for (int j = 0; j < 100; j++)
                correctAns.append(current);
            if (temp)
                current++;
            else current--;
            chain.then(random.nextBoolean(), input -> {
                int read = notAtomicBlock[0];
                for (int k = 0; k < 100; k++)
                    ansBuilder.append(read);
                if (temp)
                    read++;
                else read--;
                notAtomicBlock[0] = read;
            });
        }

        updateMainThread(Thread.currentThread());

        // onSuccess should break the loop (see the while loop afterwards)
        final AtomicBoolean finished = new AtomicBoolean(false);
        chain.start(arg -> finished.set(true));

        // Simulate the Android Looper class
        while (!finished.get() || !queue.isEmpty())
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        Assert.assertTrue(ansBuilder.toString() + " != " + correctAns.toString(), ansBuilder.toString().equals(correctAns.toString()));
    }

}