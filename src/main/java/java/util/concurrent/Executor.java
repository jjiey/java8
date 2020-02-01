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

/**
 * Executor产生的原因：callable和runnable都是定义任务的，Executor是用来执行任务的。
 *
 * 提供了一种方式，让任务提交和运行解耦开。
 *
 * An object that executes submitted {@link Runnable} tasks.
 * This interface provides a way of decoupling task submission from the mechanics of how each task will be run,
 * including details of thread use, scheduling, etc.  An {@code Executor} is normally used
 * instead of explicitly creating threads. For example, rather than
 * invoking {@code new Thread(new(RunnableTask())).start()} for each
 * of a set of tasks, you might use:
 * 翻译：一个用来执行已提交Runnable任务的对象。该接口提供了一种将任务提交与每个任务如何运行的机制解耦的方式，包括线程使用、调度等细节等。通常使用Executor来替代显式地创建线程。例如，执行一组任务中的每一个任务，你可以像下面这样使用（而不是使用new Thread(new(RunnableTask())).start()）：
 *
 * <pre>
 * Executor executor = <em>anExecutor</em>;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * ...
 * </pre>
 *
 * However, the {@code Executor} interface does not strictly
 * require that execution be asynchronous. In the simplest case, an
 * executor can run the submitted task immediately in the caller's
 * thread:
 * 翻译：然而，Executor接口并不严格要求执行是异步的。在最简单的情况下，一个执行者可以在调用者的线程中立即运行提交的任务：
 *
 *  <pre> {@code
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run();
 *   }
 * }}</pre>
 *
 * More typically, tasks are executed in some thread other
 * than the caller's thread.  The executor below spawns a new thread
 * for each task.
 * 翻译：更典型的情况是，任务在调用者的线程之外的某个线程中执行。下面的执行程序为每个任务生成一个新线程。
 *
 *  <pre> {@code
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start();
 *   }
 * }}</pre>
 *
 * Many {@code Executor} implementations impose some sort of
 * limitation on how and when tasks are scheduled.  The executor below
 * serializes the submission of tasks to a second executor,
 * illustrating a composite executor.
 * 翻译：许多Executor的实现对如何以及何时调度任务施加了某些限制。下面的执行程序序列化任务的提交到第二个执行程序，展示了一个复合的执行程序。
 *
 *  <pre> {@code
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
 *   final Executor executor;
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.offer(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active);
 *     }
 *   }
 * }}</pre>
 *
 * The {@code Executor} implementations provided in this package
 * implement {@link ExecutorService}, which is a more extensive
 * interface.  The {@link ThreadPoolExecutor} class provides an
 * extensible thread pool implementation. The {@link Executors} class
 * provides convenient factory methods for these Executors.
 * 翻译：这个package下提供的Executor的实现其实是实现了ExecutorService，这是一个更广泛的接口。ThreadPoolExecutor类提供了一个可扩展的线程池实现。Executors类为这些执行程序提供了方便的工厂方法。
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a {@code Runnable} object to an {@code Executor}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * its execution begins, perhaps in another thread.
 * 翻译：内存一致性：某个线程提交一个Runnable对象到一个Executor这个操作发生在Executor开始执行之前，也许是在另一个线程中。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Executor {

    /**
     * 其实不管AbstractExecutorService类的submit方法的任务入参是Runnable还是Callable，execute 方法最终执行的任务都是 FutureTask
     * FutureTask implements RunnableFuture extends Runnable, Future
     *
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     * 翻译：在将来的某个时候执行给定的命令。该命令可以在一个新线程、一个线程池或调用线程中执行，由Executor的实现来处理。
     *
     * @param command the runnable task 可运行的任务
     * @throws RejectedExecutionException if this task cannot be
     * accepted for execution 如果无法接受此任务执行则抛出RejectedExecutionException
     * @throws NullPointerException if command is null 如果命令是空的则抛出NullPointerException
     */
    void execute(Runnable command);
}
