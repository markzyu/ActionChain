package zyu19.libs.action.chain;

import zyu19.libs.action.chain.config.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Type Safe version if ActionChain
 * <p>
 * For usages of any implementation of this class, please refer to javadoc of ChainStyle and ErrorHolder.
 * <p>
 * Users of this library can extends this class to build their own versions of ActionChain.
 * <p>
 * This class contains every piece of fundamental code. Actually the ActionChain class is
 * just an empty shell that "extends AbstractActionChain&lt;ActionChain&gt;".
 *
 * @param <T> The type that will be passed in the pipeline to next Consumer/PureAction/onSuccess
 * @author Zhongzhi Yu
 * @version 0.4
 * @see ActionChain
 */
public class TActionChain<T> {

    //------------- public functions (FakePromise Interface) ----------------

    /**
     * starts running this chain.
     *
     * @param onSuccess a callback to consume output in pipeline upon success
     * @return a Object, if you return this object in another chain, the action after that will
     * receive this chain's final output (Thus this type cannot be easily inferred using Java Generics).
     */
    public final Object start(NiceConsumer<T> onSuccess) {
        ReadOnlyChain chain = new ReadOnlyChain(mActionSequence, onSuccess, mThreadPolicy);
        chain.start();
        return chain;
    }

    public TActionChain<Void> clear(NiceConsumer<ErrorHolder> onFailure) {
        mCurrentOnFailure = onFailure;
        mActionSequence.clear();
        return (TActionChain<Void>) this;
    }

    public final TActionChain<T> fail(NiceConsumer<ErrorHolder> onFailure) {
        mCurrentOnFailure = onFailure;
        return this;
    }

    public <E extends Exception> TActionChain<T> fail(Class<E> claz, NiceConsumer<ErrorHolder<E>> onFailure) {
        final NiceConsumer<ErrorHolder> oldHandler = mCurrentOnFailure;
        mCurrentOnFailure = error -> {
            if (claz.isAssignableFrom(error.getCause().getClass()))
                onFailure.consume((ErrorHolder<E>) error);
            else if (oldHandler != null)
                oldHandler.consume(error);
        };
        return this;
    }

    public <Out> TActionChain<Out> then(boolean runOnWorkerThread, PureAction<T, Out> action) {
        mActionSequence.add(new ChainLink<T, Out>(action, mCurrentOnFailure, runOnWorkerThread));
        return (TActionChain<Out>) this;
    }

    public final <Out> TActionChain<Out> netThen(PureAction<T, Out> action) {
        mActionSequence.add(new ChainLink<T, Out>(action, mCurrentOnFailure, true));
        return (TActionChain<Out>) this;
    }

    public final <Out> TActionChain<Out> uiThen(PureAction<T, Out> action) {
        mActionSequence.add(new ChainLink<T, Out>(action, mCurrentOnFailure, false));
        return (TActionChain<Out>) this;
    }


    /**
     * starts running this chain.
     *
     * @return a Object, if you return this object in another chain, the action after that will
     * receive this chain's final output (Thus this type cannot be easily inferred using Java Generics).
     */
    public Object start() {
        return start(null);
    }

    public <Out> TActionChain<Out> then(boolean runOnWorkerThread, Producer<Out> action) {
        return this.<Out>then(runOnWorkerThread, in -> action.produce());
    }

    public TActionChain<Void> thenConsume(boolean runOnWorkerThread, Consumer<T> action) {
        return this.<Void>then(runOnWorkerThread, (T in) -> {
            action.consume(in);
            return null;
        });
    }

    public <Out> TActionChain<Out> netThen(Producer<Out> action) {
        return this.<Out>netThen(in -> action.produce());
    }

    public TActionChain<Void> netConsume(Consumer<T> action) {
        return this.<Void>netThen((T in) -> {
            action.consume(in);
            return null;
        });
    }

    public <Out> TActionChain<Out> uiThen(Producer<Out> action) {
        return uiThen(in -> action.produce());
    }

    public TActionChain<Void> uiConsume(Consumer<T> action) {
        return uiThen((T in) -> {
            action.consume(in);
            return null;
        });
    }

    //------------- Constructors ----------------
    //------------- NOT PUBLIC, please use Factory

    private NiceConsumer<ErrorHolder> mCurrentOnFailure;
    private ArrayList<ChainLink<?, ?>> mActionSequence = new ArrayList<>();
    protected final ThreadPolicy mThreadPolicy;

    /**
     * We mark the constructors as not public because one can only use new TActionChain&lt;Void&gt; to create a new chain,
     * so a generic constructor can be misleading for people.
     *
     * @param threadPolicy See ThreadPolicy
     */
    TActionChain(ThreadPolicy threadPolicy) {
        mThreadPolicy = threadPolicy;
    }

    /**
     * We mark the constructors as not public because one can only use new TActionChain&lt;Void&gt; to create a new chain,
     * so a generic constructor can be misleading for people.
     *
     * @param onFailure    the handler to be called when any of the then() actions eventually fails
     * @param threadPolicy See ThreadPolicy
     */
    TActionChain(ThreadPolicy threadPolicy, NiceConsumer<ErrorHolder> onFailure) {
        mThreadPolicy = threadPolicy;
        mCurrentOnFailure = onFailure;
    }


    // ------------ STATIC HELPERS ----------------

    public static Object all(Object... objects) {
        return all(Arrays.asList(objects));
    }

    /**
     * Usage:
     * <p>
     * chain.then(obj -&gt; ActionChain.all(1,2,new ActionChain(...).then(obj -&gt; 3).start())).start(ans -&gt; {
     * // here you will find out that ans = [1,2,3]
     * }
     * <p>
     * Note: using .all may start all actions in parallel (depending on how you started that list of ActionChains in the first place)
     * but there may be a limit of maximum parallel thread,
     * depending on how you instantiate the threadPolicy.
     *
     * @param objects if some objects in this list are created by ActionChain, this ActionChain will wait for them. Other objects will be
     *                directly put into the list returned result
     * @return the object you should return inside the .then()
     */
    public static Object all(List<Object> objects) {
        return new DotAll(objects);
    }
}