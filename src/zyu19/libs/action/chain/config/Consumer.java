package zyu19.libs.action.chain.config;

/**
 * This interface's instances are meant to be used as callbacks, in the form of
 * lambda expressions or anonymous classes.
 * <p>
 * Created on 6/28/2015.
 * @author Zhongzhi Yu 
 * 
 * @version 0.1
 */
public interface Consumer<ArgType> {
	void consume(ArgType arg);
}
