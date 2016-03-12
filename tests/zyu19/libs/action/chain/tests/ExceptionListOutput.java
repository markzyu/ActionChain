package zyu19.libs.action.chain.tests;

import org.junit.Assert;
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
public class ExceptionListOutput {

    static Random random = new Random();
    static BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    static ThreadPolicy threadPolicy = new ThreadPolicy(runnable -> queue.add(runnable), Executors.newCachedThreadPool());
    static ActionChain chain = new ActionChain(threadPolicy);
    public static void main(String[] s) {

        PrintStream stderr = System.err;

        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // Test: more than 2 types
        Integer nullInt = null;
        int[] stage = new int[]{0};
        int[] numEx = new int[]{0};

        chain.clear(null
        ).fail(IOException.class, errorHolder -> {
            numEx[0]++;
            errorHolder.retry();
        }).then(random.nextBoolean(), obj -> {
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

        // Simulate the Android Looper class
        while (!queue.isEmpty())
            try {
                System.out.print(errContent.toString());
                if(errContent.toString().contains(ExceptionList.messageTag)) {
                    break;
                }
                queue.take().run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }
}
