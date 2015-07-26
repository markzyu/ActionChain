package zyu19.libs.action.chain.config;

/**
 * @author Zhongzhi Yu
 *         Created on 6/28/2015.
 */
public interface Consumer<ArgType> {
	void consume(ArgType arg);
}
