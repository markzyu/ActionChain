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
 *
 * @author Zhongzhi Yu
 * @version 0.4
 */
public class ReturnedChainTest {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    ThreadPolicy threadPolicy = new ThreadPolicy(runnable -> queue.add(runnable), Executors.newCachedThreadPool());
    ActionChain chain = new ActionChain(threadPolicy);

    Thread mainThread = Thread.currentThread();

    public void updateMainThread(Thread thread) {
        mainThread = thread;
    }

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

        chain.clear(errorHolder -> Assert.fail(errorHolder.getCause().toString())
        ).netThen(() -> 0);

        for (int i = 0; i < testLength; i++) {
            final int thisI = i;
            final int temp = random.nextInt();
            correctAns += "Start" + String.valueOf(temp) + "Sub";
            for (int j = 0; j < subTestLength; j++)
                correctAns += i + ", " + j + ". ";
            correctAns += "End";

            final Integer[] inputRecord = new Integer[1];
            chain.then(random.nextBoolean(), (Integer input) -> {
                ansBuilder.append("Start");
                ansBuilder.append(temp);
                ansBuilder.append("Sub");
                ActionChain subChain = new ActionChain(threadPolicy);
                for (int j = 0; j < subTestLength; j++) {
                    final int thisJ = j;
                    subChain.then(random.nextBoolean(), obj -> {
                        ansBuilder.append(thisI);
                        ansBuilder.append(", ");
                        ansBuilder.append(thisJ);
                        ansBuilder.append(". ");
                    });
                }
                inputRecord[0] = input;
                return subChain.start(obj -> {
                });
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

    @Test(timeout = 9000)
    public void TestWITHException_EveryCallbackIsInvokedInCorrectOrder() {
        final int testLength = 100;
        final int subTestLength = 10;
        final StringBuilder ansBuilder = new StringBuilder();
        String correctAns = "";
        Random random = new Random();

        final AtomicBoolean finished = new AtomicBoolean(false);

        chain.clear(errorHolder -> Assert.fail(errorHolder.getCause().toString())
        ).netThen(() -> 0);

        for (int i = 0; i < testLength; i++) {
            final int thisI = i;
            final int temp = random.nextInt();
            final boolean subHasHandler = random.nextBoolean();
            final int subErrorJ = random.nextInt(subTestLength);
            final boolean retry = random.nextInt(50) <= 47;

            correctAns += "Start" + String.valueOf(temp) + "Sub";
            for (int j = 0; j < subTestLength; j++) {
                if (subErrorJ == j)
                    correctAns += "error! ";
                if(subErrorJ == j && !retry)
                    break;
                correctAns += i + ", " + j + ". ";
            }
            if(retry)
                correctAns += "End";

            chain.fail(error -> {
                Assert.assertTrue(!subHasHandler);
                ansBuilder.append("error! ");
                if(error.getCause() instanceof NullPointerException)
                    throw (NullPointerException)error.getCause();
                if (retry)
                    error.retry();
                else finished.set(true);
            }).then(random.nextBoolean(), (Integer input) -> {
                ansBuilder.append("Start");
                ansBuilder.append(temp);
                ansBuilder.append("Sub");
                ActionChain subChain = new ActionChain(threadPolicy, subHasHandler ? error -> {
                    final boolean testResult = isMainThread();
                    queue.add(() -> Assert.assertTrue(testResult));
                    Assert.assertTrue(subHasHandler);
                    ansBuilder.append("error! ");
                    if(error.getCause() instanceof NullPointerException)
                        throw (NullPointerException)error.getCause();
                    if (retry)
                        error.retry();
                    else finished.set(true);
                } : null);
                for (int j = 0; j < subTestLength; j++) {
                    final int thisJ = j;
                    final Boolean[] hasThrown = new Boolean[1];
                    hasThrown[0] = false;
                    subChain.then(random.nextBoolean(), obj -> {
                        if (thisJ == subErrorJ && !hasThrown[0]) {
                            hasThrown[0] = true;
                            throw new Exception();
                        }
                        ansBuilder.append(thisI);
                        ansBuilder.append(", ");
                        ansBuilder.append(thisJ);
                        ansBuilder.append(". ");
                    });
                }
                return subChain.start(obj -> {
                });
            }).uiThen(obj -> {
                ansBuilder.append("End");
            });

            if(!retry) {
                chain.then(random.nextBoolean(), obj -> {
                    queue.add(() -> Assert.fail("This line should not have been runned"));
                });
                break;
            }
        }

        // onSuccess should break the loop (see the while loop afterwards)

        updateMainThread(Thread.currentThread());

        // collect output
        chain.start((Integer arg) -> {
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
    }
}
