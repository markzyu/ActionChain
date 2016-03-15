package zyu19.libs.action.chain.tests;

import org.junit.Assert;
import org.junit.Test;
import zyu19.libs.action.chain.ActionChain;
import zyu19.libs.action.chain.ExceptionList;
import zyu19.libs.action.chain.config.ThreadPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is just a simple test to illustrate ExceptionList
 * Created by zyu on 3/11/16.
 */
public class ExceptionOutputTest {

    static Random random = new Random();
    static BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    static ThreadPolicy threadPolicy = new ThreadPolicy(runnable -> queue.add(runnable), Executors.newCachedThreadPool());
    static ActionChain chain = new ActionChain(threadPolicy);

    @Test(timeout =  2000)
    public void Main() {

        PrintStream stderr = System.err;

        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // Test: more than 2 types
        Integer nullInt = null;
        int[] stage = new int[]{0};
        int[] numEx = new int[]{0};

        chain.clear(null
        ).then(random.nextBoolean(), obj -> {
            Object ret = ActionChain.all(new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start(), new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start(), new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start(), new ActionChain(threadPolicy).thenConsume(random.nextBoolean(), xxx -> {
                System.out.println(nullInt + 1);
            }).start());
            Thread.sleep(500);
            return ret;
        }).start(obj -> {
            Assert.fail("Should not reach this line.");
        });

        StringBuilder builder = new StringBuilder();

        // Simulate the Android Looper class
        while (!queue.isEmpty())
            try {
                System.err.flush();
                builder.append(errContent.toString());
                if(builder.toString().contains(ExceptionList.messageTag)) {
                    System.out.println("Finished");
                    break;
                }
                else queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }
}
