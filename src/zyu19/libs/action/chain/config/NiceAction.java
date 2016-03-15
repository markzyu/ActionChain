package zyu19.libs.action.chain.config;

/**
 * Created by zyu on 3/15/16.
 */
public interface NiceAction<In, Out> {
    Out process(In input);
}
