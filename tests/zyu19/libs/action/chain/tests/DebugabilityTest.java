package zyu19.libs.action.chain.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.ThreadPolicy;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @version 0.4
 */
public class DebugabilityTest {
    // These tests make sure that exceptions from NiceConsumers, and from ActionChain systems will still crash the system,
    //  rather than becoming un-detectable bugs, which might cause the system to freeze for seemingly unknown reasons.

    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    ThreadPolicy threadPolicy = new ThreadPolicy(runnable -> queue.add(runnable), Executors.newCachedThreadPool());
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

    @Test(timeout = 2000, expected = NullPointerException.class)
    public void ExceptionInErrorHandler() {
        final Integer nullInt = null;

        chain.clear(errorHolder -> System.out.println(nullInt + 1)
        ).thenConsume(random.nextBoolean(), obj -> System.out.println(nullInt + 1)
        ).start(obj -> Assert.fail("should crash"));

        StartTests();
    }

    @Test(timeout = 2000, expected = NullPointerException.class)
    public void ExceptionInSuccessHandler() {
        final Integer nullInt = null;

        chain.clear(errorHolder -> Assert.fail("should not have caught any exception inside pureActions")
        ).then(random.nextBoolean(), obj -> 123).start((Integer obj) -> {
            Assert.assertTrue(obj == 123);
            System.out.println(nullInt + 1);
        });

        StartTests();
    }

    @Test(timeout = 2000, expected = NullPointerException.class)
    public void ExceptionInSubChainSuccessHandler() {
        final Integer nullInt = null;

        chain.clear(errorHolder -> Assert.fail("should not have caught any exception inside pureActions")
        ).then(random.nextBoolean(), obj -> {
            return new ActionChain(threadPolicy, error -> {
                Assert.fail("should not have caught any exception inside pureActions");
            }).then(random.nextBoolean(), () -> 456
            ).start(innerans -> System.out.println(nullInt + 1));
        }).then(random.nextBoolean(), obj -> 123
        ).start((Integer obj) -> Assert.assertTrue(obj == 123));

        StartTests();
    }

    @Test(timeout = 2000, expected = NullPointerException.class)
    public void ExceptionInSubChainFailureHandler() {
        final Integer nullInt = null;

        chain.clear(errorHolder -> {
            Assert.fail("should not have caught any exception inside pureActions");
        }).then(random.nextBoolean(), obj -> {
            return new ActionChain(threadPolicy, error -> {
                System.out.println(nullInt + 1);
            }).thenConsume(random.nextBoolean(), xxx -> System.out.println(nullInt + 1)
            ).start(innerans -> Assert.fail("should not have succeeded"));
        }).then(random.nextBoolean(), obj -> 123
        ).start((Integer obj) -> Assert.assertTrue(obj == 123));

        StartTests();
    }

    @Test(timeout = 2000, expected = NullPointerException.class)
    public void ExceptionInFailureHandlerForSubChainErrors() {
        final Integer nullInt = null;

        chain.clear(errorHolder -> {
            System.out.println(nullInt + 1);
        }).then(random.nextBoolean(), obj -> {
            return new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start(innerans -> Assert.fail("should not have succeeded"));
        }).then(random.nextBoolean(), obj -> 123).start((Integer obj) -> {
            Assert.fail("should not have succeeded");
        });

        StartTests();
    }

    @Test(timeout = 2000)
    public void TestTypeConversion() {
        // This test should prove that Type conversion errors will be caught by ".fail()" error handlers
        chain.clear(errorHolder -> finished.set(true)
        ).then(random.nextBoolean(), obj -> true
        ).then(random.nextBoolean(), (Integer obj) -> {
            return new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(obj + 1);
            }).start(innerans -> Assert.fail("should not have succeeded"));
        }).then(random.nextBoolean(), obj -> 123).start((Integer obj) -> {
            Assert.fail("should not have succeeded");
        });

        StartTests();
    }


    // TODO: (v0.4) add tests about error handler targeted at specific exception types

    @Test(timeout = 2000)
    public void TestErrorHandlerWithFilter() {
        Integer nullInt = null;

        chain.clear(errorHolder -> {
            Assert.assertTrue(errorHolder.getCause() instanceof NullPointerException);
            finished.set(true);
        }).fail(IOException.class, errorHolder -> {
            Assert.fail("should not have caught IOException.");
        }).then(random.nextBoolean(), obj -> {
            return new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start(innerans -> Assert.fail("should not have succeeded"));
        }).then(random.nextBoolean(), obj -> 123).start((Integer obj) -> {
            Assert.fail("should not have succeeded");
        });

        StartTests();
    }

    @Test(timeout = 2000)
    public void TestErrorHandlerWithFilter2() {
        Integer nullInt = null;

        chain.clear(errorHolder -> {
            Assert.fail("should not have caught other exceptions.");
        }).fail(NullPointerException.class, errorHolder -> {
            // this is enough for an "assert" for success
            finished.set(true);
        }).then(random.nextBoolean(), obj -> {
            return new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start(innerans -> Assert.fail("should not have succeeded"));
        }).then(random.nextBoolean(), obj -> 123).start((Integer obj) -> {
            Assert.fail("should not have succeeded");
        });

        StartTests();
    }
}