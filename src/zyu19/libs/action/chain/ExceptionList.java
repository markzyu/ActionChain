package zyu19.libs.action.chain;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhongzhi Yu
 * @version 0.4
 * This is used to wrap a List a Exceptions.
 * It's useful when an outer ActionChain decides to restart a bunch of failed, inner ActionChain,
 * <em>but cannot find a handler for that</em>
 * Created by zyu on 3/11/16.
 */
public class ExceptionList extends Exception {
    final ArrayList<Throwable> mCause;

    public final static String messageTag = "ActionChain.all reported a list of Exceptions. ";

    ExceptionList(String message, List<? extends Throwable> cause) {
        super(messageTag + message);
        mCause = new ArrayList<>(cause);
    }

    ExceptionList(List<? extends Throwable> cause) {
        super(messageTag);
        mCause = new ArrayList<>(cause);
    }

    /**
     * @deprecated Use getAllCauses() instead
     * @return null in all cases
     */
    @Override
    public synchronized Throwable getCause() {
        return null;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        s.print(stackTraceString());
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        s.print(stackTraceString());
    }

    String stackTraceString() {
        StringBuilder sb = new StringBuilder(super.getLocalizedMessage()).append("\n");
        if(mCause.size() == 0)
            sb.append(" -- List of Causes: None\n");
        else {
            int i = 0;
            sb.append(" -- List of Causes: ").append(mCause.size()).append(" items.\n");
            for(Throwable throwable : mCause) {
                sb.append(" ----- Cause No.").append(i).append("\n").append(throwable.getLocalizedMessage());
                i++;
            }
        }
        return sb.toString();
    }

    public List<Throwable> getAllCauses() {
        return mCause;
    }

    @Override
    public String toString() {
        return stackTraceString();
    }
}
