package zyu19.libs.action.chain.config;

/**
 * This interface's instances are meant to be used as callbacks, in the form of
 * lambda expressions or anonymous classes.
 * <p>
 * Created on 7/20/2015.
 * @author Zhongzhi Yu 
 * 
 * @version 0.1
 */
public interface PureAction<In, Out> {
	Out process(In input) throws Exception;
}
