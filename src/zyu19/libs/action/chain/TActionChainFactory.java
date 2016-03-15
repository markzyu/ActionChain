package zyu19.libs.action.chain;

import zyu19.libs.action.chain.config.*;

/**
 * I have created a Factory for TActionChain for you.
 * @author Zhongzhi Yu
 * Created by Zhongzhi Yu on 3/4/16.
 */
public class TActionChainFactory {
    final ThreadPolicy threadPolicy;
    public TActionChainFactory(ThreadPolicy threadPolicy) {
        this.threadPolicy = threadPolicy;
    }

    public TActionChain<Void> get() {
        return new TActionChain<Void>(threadPolicy);
    }

    public TActionChain<Void> get(NiceConsumer<ErrorHolder> onFailure) {
        return new TActionChain<Void>(threadPolicy, onFailure);
    }

    public <In, Out> TActionChain<Out> get(NiceAction<TActionChain<In>, TActionChain<Out>> chainTemplate, In argument) {
        TActionChain<In> newChain = new TActionChain<>(threadPolicy
        ).netThen(() -> argument);
        return chainTemplate.process(newChain);
    }
}
