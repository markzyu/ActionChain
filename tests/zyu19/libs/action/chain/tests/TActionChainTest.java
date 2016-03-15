package zyu19.libs.action.chain.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.TActionChain;
import zyu19.libs.action.chain.TActionChainFactory;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ThreadPolicy;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zyu on 3/15/16.
 * @version 0.4-beta.5
 */
public class TActionChainTest {
    // These tests make sure that exceptions from NiceConsumers, and from ActionChain systems will still crash the system,
    //  rather than becoming un-detectable bugs, which might cause the system to freeze for seemingly unknown reasons.

    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    ThreadPolicy threadPolicy = new ThreadPolicy(runnable -> queue.add(runnable), Executors.newCachedThreadPool());
    TActionChainFactory tChainFactory = new TActionChainFactory(threadPolicy);
    ActionChain chain = new ActionChain(threadPolicy);

    Thread mainThread = Thread.currentThread();

    Random random = new Random();
    AtomicBoolean finished = new AtomicBoolean(false);

    public void updateMainThread(Thread thread) {
        mainThread = thread;
    }

    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    @Before
    public void BeforeTests() {
        queue.clear();
        finished.set(false);
        updateMainThread(Thread.currentThread());
    }

    public void StartTests() {
        // Simulate the Android Looper class
        while (!finished.get() || !queue.isEmpty())
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    @Test(timeout = 2000)
    public void TestSimpleTActionChain() {
        // This is compiler time check
        tChainFactory.get(fail -> fail.getCause().printStackTrace()
                // ).netThen((Integer obj) -> 123); You cannot do this because obj is of type Void
        ).netThen(obj -> 123
        ).netThen(i -> Integer.toString(i + 3)
        ).uiConsume(System.out::println
        ).netThen(() -> Arrays.<String>asList("a", "b", "c")
        ).netThen(strs -> {
            return tChainFactory.get().netThen(() -> strs.stream().reduce("", (a, b) -> {
                return a + b;
            })).start();
        }).uiConsume(obj -> {
            System.out.println((String) obj);
        }).start(obj -> {
            finished.set(true);
        });

        StartTests();
    }

}
