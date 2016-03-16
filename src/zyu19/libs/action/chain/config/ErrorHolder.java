package zyu19.libs.action.chain.config;

/**
 * ActionChain will provide instances of this type through 'NiceConsumer&lt;ErrorHolder&gt;'
 * when an error occurred, to inform the user whether the error may be recoverable.
 * <p>
 * If possibly recoverable, the user should check the error (which
 * can be acquired through the getCause() method) and decide whether
 * to invoke the retry() method in this ErrorHolder to recover the
 * halted process.
 * <h1>Why is this useful?</h1>
 * <p>
 * The point of a ErrorHolder is that, when programmers can split a task in ActionChain
 * into multiple steps, if a recoverable Exception occurred, this interface will enable
 * them to retry only the problematic part so that it's not a must to use ActionChain.start()
 * to rerun all steps in ActionChain.
 * <p>
 * For instance, if a social app supports posting pictures with comments, with ActionChain
 *  the developer can divide the uploading process into
 *  <ol>
 *  <li>upload picture</li>
 *  <li>record the comment and the link to the uploaded picture</li>
 *  </ol>
 * <p>
 * Now consider the situation that, during the second step, network breaks down. Without ErrorHolder,
 * the app developer must retry the process either by starting from uploading the picture, or by
 * maintaining a variable to represent which parts of the task have been accomplished. Thus ErrorHolder
 * may help the developer save the user's time and data plan.    
 * <h1>NOTE:</h1>
 * <p>
 * In order to retry any task in any ActionChain, the user must call ErrorHolder.retry()
 * within the 'NiceConsumer&lt;ErrorHolder&gt;' callback. Calling the function from the outside will not work.
 * <p>
 * ThreadPolicy will ensure that this class runs on <b>UI (Main)</b> thread specified
 * by ThreadChanger.
 * <p>
 * BY DEFAULT, if the user code doesn't call ErrorHolder at all, the halted process will be DISCARDED.
 * <h1>WARNING:</h1>
 * <p>
 * It should be avoided to pass ErrorHolder to threads other than the
 * thread of the 'NiceConsumer&lt;ErrorHolder&gt;' callback.
 * <p>
 * Created on 7/2/2015.
 * <br>
 * Modified on 7/21/2015.
 * <br>
 * Isolated as Interface on 7/24/2015.
 *
 * @author Zhongzhi Yu 
 * 
 * @version 0.1
 */
public interface ErrorHolder <E extends Exception> {
	/**
	 * get the Exception that halted ActionChain
	 * @return the Exception that halted ActionChain
	 * @see ErrorHolder
	 */
	E getCause();
	
	/**
	 * prevent ActionChain from exiting and force it to rerun the last PureAction
	 * @see ErrorHolder
	 */
	void retry();

	/**
	 * Skip the piece of action causing trouble, by jumping to a next / previous action.
	 * @param offset by how much to jump... Note: if it jumps too far, it will simply stop executing.
	 */
	void jumpBy(int offset);


	/**
	 * Notice that this also counts the actions added by chainTemplates.
	 * @return an integer value that starts at 0
     */
	int getPosition();
}
