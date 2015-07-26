package zyu19.libs.action.chain.config;

/**
 * @author Zhongzhi Yu
 *         Created on 7/20/2015.
 */
public interface PureAction<In, Out> {
	Out process(In input) throws Exception;
}
