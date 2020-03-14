/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * FutureTask 就统一了 Callable 和 Runnable
 *
 * FutureTask可以当做是线程运行的具体任务
 * 可取消的异步计算
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 *
 * FutureTask 可以用转化 Callable 和 Runnable
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     *
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     */

    /**
     * 任务的状态
     * The run state of this task, initially NEW.  The run state
     * transitions to a terminal state only in methods set,
     * setException, and cancel.
     * During completion, state may take on
     * transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a
     * cancel(true)). Transitions from these intermediate to final
     * states use cheaper ordered/lazy writes because values are unique
     * and cannot be further modified.
     *
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    /**
     * 线程任务创建
     */
    private static final int NEW          = 0;
    /**
     * 任务执行中
     */
    private static final int COMPLETING   = 1;
    /**
     * 任务执行结束
     */
    private static final int NORMAL       = 2;
    /**
     * 任务异常
     */
    private static final int EXCEPTIONAL  = 3;
    /**
     * 任务取消成功
     */
    private static final int CANCELLED    = 4;
    /**
     * 任务正在被打断中
     */
    private static final int INTERRUPTING = 5;
    /**
     * 任务被打断成功
     */
    private static final int INTERRUPTED  = 6;

    /** The underlying callable; nulled out after running 底层的调用；运行后为空 */
    // FutureTask 组合了 Callable
    private Callable<V> callable;

    /** The result to return or exception to throw from get() 要返回的结果或从get()抛出的异常 */
    // 异步线程返回的结果，读写锁保证了其线程安全
    private Object outcome; // non-volatile, protected by state reads/writes

    /** The thread running the callable; CASed during run() 运行可调用的线程；运行期间CASed */
    // 当前任务所运行的线程
    private volatile Thread runner;

    /** Treiber stack of waiting threads 等待线程的Treiber堆栈 */
    // 记录调用 get 方法时被等待的线程
    private volatile WaitNode waiters;

    /**
     * 任务执行完成，返回执行的结果
     * 其实就是返回outcome的值
     *
     * Returns result or throws exception for completed task.
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL)
            return (V)x;
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable)x);
    }

    /**
     * 使用 Callable 进行初始化
     *
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     * 翻译：创建一个FutureTask，它将在运行时执行给定的Callable。
     *
     * @param  callable the callable task callable任务
     * @throws NullPointerException if the callable is null 如果callable为空则抛出NullPointerException
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        // 任务状态初始化
        this.state = NEW;       // ensure visibility of callable 确保callable的可见性
    }

    /**
     * 使用 Runnable 初始化，并传入 result 作为返回结果
     * Runnable 是没有返回值的，所以 result 一般没有用，置为 null 就好了
     * @param runnable
     * @param result 返回结果
     */
    public FutureTask(Runnable runnable, V result) {
        // Executors.callable 方法把 runnable 适配成 RunnableAdapter，RunnableAdapter 实现了 callable，所以也就是把 runnable 直接适配成了 callable。
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable 确保callable的可见性
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

    /**
     * 取消任务，如果正在运行，尝试去打断
     * @param mayInterruptIfRunning true 表示 任务正在被打断中；false 表示 任务取消成功
     * @return
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 任务状态不是NEW 并且不能把 new 状态置为 取消，直接返回 false
        if (!(state == NEW &&
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        // 进行取消操作，打断可能会抛出异常，所以选择 try finally 的结构
        try {    // in case call to interrupt throws exception 在调用中断时抛出异常
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    // 状态设置成已打断
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            // 清理线程
            finishCompletion();
        }
        return true;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 如果任务刚创建或在执行中，等待执行成功
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        // 任务执行完成，返回执行的结果
        return report(s);
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        // 如果任务状态为执行中，并且等待一定的时间后，状态仍然为执行中，直接抛出超时异常
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            throw new TimeoutException();
        // 任务执行成功，返回执行的结果
        return report(s);
    }

    /**
     * 等待任务执行完成
     * @param timed 任务是否一直等待，false表示一直等待
     * @return 任务执行状态
     */
    private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        // 计算等待终止时间，如果一直等待的话，终止时间为 0
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        // 不排队
        boolean queued = false;
        // 无限循环
        for (;;) {
            // 如果线程已经被打断了，删除节点，抛异常
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }
            // 获取当前任务状态
            int s = state;
            // 当前任务已经执行完了，返回
            if (s > COMPLETING) {
                // 当前任务的线程置空
                if (q != null)
                    q.thread = null;
                return s;
            }
            // 如果正在执行，当前线程让出 cpu，重新竞争，防止 cpu 飙高
            else if (s == COMPLETING) // cannot time out yet 还不能超时
                Thread.yield();
            // 如果第一次运行，新建 waitNode，当前线程就是 waitNode 的属性
            else if (q == null)
                q = new WaitNode();
            // 默认第一次都会执行这里，执行成功之后，queued 就为 true，就不会再执行了
            // 把当前 waitNode 当做 waiters 链表的第一个
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                     q.next = waiters, q);
            // 如果设置了超时时间，并过了超时时间的话，从 waiters 链表中删除当前 wait
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                // 没有过超时时间，线程进入 TIMED_WAITING 状态
                LockSupport.parkNanos(this, nanos);
            }
            // 没有设置超时时间，进入 WAITING 状态
            else
                LockSupport.park(this);
        }
    }

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    protected void done() { }

    /**
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     *
     * @param v the value
     */
    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion();
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }

    /**
     * run 方法可以直接被调用，也可以由线程池进行调用
     * 如果需要开启子线程的话，只能走线程池，线程池会帮忙开启线程
     */
    public void run() {
        // 状态不是NEW || 当前任务已经有线程在执行了，就直接返回
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            // 如果 Callable 不为空，并且已经初始化完成
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    // 调用执行
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    // 给 outcome 赋值
                    setException(ex);
                }
                // 给 outcome 赋值
                if (ran)
                    set(result);
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // 翻译：运行程序必须是非空的，直到确定状态为止以防止对run()的并发调用。
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // 翻译：为防止泄露的中断，在运行程序为空之后必须重新读取状态
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    // 简单的链表 记录在对账上等待的线程
    static final class WaitNode {
        // 当前等待的线程
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }

    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
