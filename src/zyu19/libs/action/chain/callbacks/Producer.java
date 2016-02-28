package zyu19.libs.action.chain.callbacks;

/**
 * This interface's instances are meant to be used as callbacks, in the form of
 * lambda expressions or anonymous classes.
 * <p>
 * Created on 2/28/2016.
 * @author Zhongzhi Yu
 *
 * @version 0.3
 */
public interface Producer <Out> {
    Out produce() throws Exception;
}
