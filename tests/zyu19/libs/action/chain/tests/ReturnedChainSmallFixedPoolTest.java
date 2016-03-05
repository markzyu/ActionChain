package zyu19.libs.action.chain.tests;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zyu on 3/4/16.
 */
public class ReturnedChainSmallFixedPoolTest extends ReturnedChain_Template{
    @Override
    ExecutorService getExecutors() {
        return Executors.newFixedThreadPool(1);
    }
}
