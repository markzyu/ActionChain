package zyu19.libs.action.chain.config;

/**
 * ErrorHolder Interface.
 * <p>
 * Basically instances of this class is used like Exception. But the following instructions
 * are very important.
 * <p>
 * Instances of this type will be passed to the user when an error occurred,
 * to inform the user whether the error may be recoverable.
 * <p>
 * If possibly recoverable, the user should check the type of that error and
 * decide whether to use this ErrorHolder to recover the halted process.
 * <p>
 * If not recoverable at all, the user can record the error.
 * <p>
 * NOTE: In order to retry any calls to any AbstractClient, the user must
 * do so through this ErrorHolder within the 'Consumer' callback. Calling
 * the function again, from the outside, will NOT succeed sometimes.
 * <p>
 * WARNING: As soon as the user code leaves the 'Consumer' callback block,
 * the decision to recover is made up and cannot be changed. BY DEFAULT,
 * if the user code doesn't call ErrorHolder at all, the halted process
 * will be DISCARDED.
 * <p>
 * NOTE: ThreadPolicy will ensure that this class runs on the thread specified
 * by ThreadChanger.
 * <p>
 * WARNING: It should be avoided to pass ErrorHolder to threads other than
 * the thread of the 'Consumer' callback.
 *
 * @author Zhongzhi Yu
 *         Created on 7/2/2015.
 *         Modified on 7/21/2015.
 *         Isolated as Interface on 7/24/2015.
 */
public interface ErrorHolder {
	Exception getCause();
	void retry();
}
