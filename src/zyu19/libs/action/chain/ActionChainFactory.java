package zyu19.libs.action.chain;

import zyu19.libs.action.chain.config.ErrorHolder;
import zyu19.libs.action.chain.config.NiceConsumer;
import zyu19.libs.action.chain.config.ThreadPolicy;

import java.rmi.activation.ActivationID;

/**
 * I have created a Factory for ActionChain for you.
 * @author Zhongzhi Yu
 * Created by Zhongzhi Yu on 3/4/16.
 */
public class ActionChainFactory {
    final ThreadPolicy threadPolicy;
    public ActionChainFactory(ThreadPolicy threadPolicy) {
        this.threadPolicy = threadPolicy;
    }

    public ActionChain get() {
        return new ActionChain(threadPolicy);
    }

    public ActionChain get(NiceConsumer<ErrorHolder> onFailure) {
        return new ActionChain(threadPolicy, onFailure);
    }

    public ActionChain get(NiceConsumer<ActionChain> chainTemplate, Object argument) {
        return new ActionChain(threadPolicy, chainTemplate, argument);
    }
}
