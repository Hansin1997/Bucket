package cn.dustlight.bucket.other;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Common Future implement the RunnableFuture
 * <p>
 * You can add listener,they will be call when the future was done.
 *
 * @param <T>
 */
public abstract class CommonFuture<T> implements RunnableFuture<T> {

    private List<CommonListener<T>> list;
    private T result;
    private Exception e;
    private boolean started;
    private boolean done;

    public CommonFuture() {
        list = new ArrayList<>();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return result;
    }

    public CommonFuture<T> addListener(CommonListener<T> listener) {
        if (isDone()) {
            listener.onDone(result, e);
        } else {
            synchronized (list) {
                list.add(listener);
            }
        }
        return this;
    }

    protected void done(T result, Exception e) {
        this.result = result;
        this.e = e;
        synchronized (list) {
            for (CommonListener<T> l : list) {
                l.onDone(this.result, this.e);
            }
            list.clear();
            done = true;
        }
    }

    protected void done(Exception e) {
        this.e = e;
        synchronized (list) {
            for (CommonListener<T> l : list) {
                l.onDone(this.result, this.e);
            }
            list.clear();
            done = true;
        }
    }

    protected void done(T result) {
        this.result = result;
        synchronized (list) {
            for (CommonListener<T> l : list) {
                l.onDone(this.result, this.e);
            }
            list.clear();
            done = true;
        }
    }

    public CommonFuture<T> start() {
        if (!started) {
            started = true;
            try {
                run();
            } catch (Exception e) {
                done(this.result, e);
            }
        }
        return this;
    }

    public static interface CommonListener<T> {
        void onDone(T result, Exception e);
    }
}
