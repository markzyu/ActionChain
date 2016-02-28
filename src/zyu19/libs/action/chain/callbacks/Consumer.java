package zyu19.libs.action.chain.callbacks;

/**
 * This interface's instances are meant to be used as callbacks, in the form of
 * lambda expressions or anonymous classes.
 * <br>
 * From v0.3 on, there are two NiceConsumer callbacks:
 * <br>
 * 1. NiceConsumer: these callbacks are not allowed to throw any Exception, which <b>used to be called NiceConsumer</b> before v0.2
 * <br>
 * 2. Consumer: these callbacks are allowed to throw Exceptions
 * <br>
 * Created on 6/28/2015.
 * Modified on 2/28/2016.
 * @author Zhongzhi Yu 
 * 
 * @version 0.3
 */
public interface Consumer<In> {
	void consume(In arg) throws Exception;
}
