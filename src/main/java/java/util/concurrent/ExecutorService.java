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
import java.util.List;
import java.util.Collection;

/**
 * An {@link Executor} that provides methods to manage termination and
 * methods that can produce a {@link Future} for tracking progress of
 * one or more asynchronous tasks.
 * 翻译：一个提供管理终止的方法的Executor，以及可以生成一个用于跟踪一个或多个异步任务的进度的Future的方法。
 *
 *
 * <p>An {@code ExecutorService} can be shut down, which will cause
 * it to reject new tasks.  Two different methods are provided for
 * shutting down an {@code ExecutorService}. The {@link #shutdown}
 * method will allow previously submitted tasks to execute before
 * terminating, while the {@link #shutdownNow} method prevents waiting
 * tasks from starting and attempts to stop currently executing tasks.
 * Upon termination, an executor has no tasks actively executing, no
 * tasks awaiting execution, and no new tasks can be submitted.  An
 * unused {@code ExecutorService} should be shut down to allow
 * reclamation of its resources.
 * 翻译：ExecutorService可以被关闭，这将导致它拒绝新任务。提供了两种不同的方法来关闭一个ExecutorService。shutdown方法将允许在终止之前执行完之前提交的任务，而shutdownNow方法防止等待中的任务启动并会尝试停止当前正在执行的任务。终止时，执行程序没有正在执行的任务，没有等待执行的任务，也不能提交新的任务。应该关闭没有使用的ExecutorService以允许回收其资源。
 *
 * <p>Method {@code submit} extends base method {@link
 * Executor#execute(Runnable)} by creating and returning a {@link Future}
 * that can be used to cancel execution and/or wait for completion.
 * Methods {@code invokeAny} and {@code invokeAll} perform the most
 * commonly useful forms of bulk execution, executing a collection of
 * tasks and then waiting for at least one, or all, to
 * complete. (Class {@link ExecutorCompletionService} can be used to
 * write customized variants of these methods.)
 * 翻译：submit方法通过创建和返回一个能被用来取消执行 和/或 等待完成的Future来扩展了基础方法Executor的execute(Runnable)方法。invokeAny和invokeAll方法执行最常用的批量执行的形式，执行一组任务，然后等待至少一个或全部任务完成。ExecutorCompletionService类可以被用来编写这些方法的定制变体。
 *
 * <p>The {@link Executors} class provides factory methods for the
 * executor services provided in this package.
 * 翻译：Executors类为这个package下提供的executor服务提供工厂方法
 *
 * <h3>Usage Examples</h3>
 * 翻译：用例
 *
 * Here is a sketch of a network service in which threads in a thread
 * pool service incoming requests. It uses the preconfigured {@link
 * Executors#newFixedThreadPool} factory method:
 * 翻译：这是网络服务的示意图，其中线程池服务中的线程传入的请求。它使用预先配置的Executors#newFixedThreadPool工厂方法:
 *
 *  <pre> {@code
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }}</pre>
 *
 * The following method shuts down an {@code ExecutorService} in two phases,
 * first by calling {@code shutdown} to reject incoming tasks, and then
 * calling {@code shutdownNow}, if necessary, to cancel any lingering tasks:
 * 翻译：下面的方法分两个阶段关闭一个ExecutorService，首先通过调用shutdown方法来拒绝传入新的任务，然后调用shutdownNow方法（如果需要的话）取消任何延迟的任务：
 *
 *  <pre> {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted 禁止提交新任务
 *   try {
 *     // Wait a while for existing tasks to terminate 等待现有存在的任务终止
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks 取消当前正在执行的任务
 *       // Wait a while for tasks to respond to being cancelled 等待任务响应被取消
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted (重新)如果当前线程中断也取消当前正在执行的任务
 *     pool.shutdownNow();
 *     // Preserve interrupt status 保存中断状态
 *     Thread.currentThread().interrupt();
 *   }
 * }}</pre>
 *
 * <p>Memory consistency effects: Actions in a thread prior to the
 * submission of a {@code Runnable} or {@code Callable} task to an
 * {@code ExecutorService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * any actions taken by that task, which in turn <i>happen-before</i> the
 * result is retrieved via {@code Future.get()}.
 * 翻译：内存一致性：某个线程提交一个Runnable或Callable任务到一个ExecutorService这个操作发生在该任务所采取的任何操作之前，反过来又发生在结果通过Future.get()被检索之前。
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * 翻译：开始一个有序的关闭，在此过程中只执行之前提交的任务，但不接受任何新任务。如果已经关闭了，调用将没有其他额外的效果。
     *
     * <p>This method does not wait for previously submitted tasks to complete execution.
     * Use {@link #awaitTermination awaitTermination}
     * to do that.
     * 翻译：此方法不等待之前提交的任务执行完成。使用awaitTermination方法去等待
     *
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     *         如果存在一个安全管理器，并且关闭这个ExecutorService可能会操作调用者不被允许修改的线程，因为它不包含RuntimePermission或安全管理器的checkAccess方法拒绝访问。则抛出SecurityException
     */
    // 关闭，不会接受新的任务，也不会等待未完成的任务
    // 如果需要等待未完成的任务，可以使用 awaitTermination 方法
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks,
     * halts the processing of waiting tasks, and returns a list of the tasks that were awaiting execution.
     * 翻译：尝试停止所有正在执行的任务，停止处理等待的任务，并返回一个等待执行的任务列表。
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     * 翻译：此方法不等待正在执行的任务终止。使用awaitTermination方法去等待
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     * 翻译：除了尽最大努力尝试去停止处理正在执行的任务外，没有任何保证。例如，经典的实现将通过Thread#interrupt取消，所以任何响应中断失败的任务可能永远都不会终止。
     *
     * @return list of tasks that never commenced execution 返回从未开始执行的任务的列表
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     *         如果存在一个安全管理器，并且关闭这个ExecutorService可能会操作调用者不被允许修改的线程，因为它不包含RuntimePermission或安全管理器的checkAccess方法拒绝访问。则抛出SecurityException
     */
    // 试图关闭所有正在执行的任务，返回等待执行的任务列表
    List<Runnable> shutdownNow();

    /**
     * Returns {@code true} if this executor has been shut down.
     * 翻译：如果这个执行程序已经被关闭了则返回true
     *
     * @return {@code true} if this executor has been shut down 如果这个执行程序已经被关闭了则返回true
     */
    // executor 是否已经关闭了，返回值 true 表示已关闭
    boolean isShutdown();

    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     * 翻译：如果所有任务都在关闭后完成，则返回true。注意，除非首先调用shutdown或shutdownNow其中之一，否则isTerminated方法永远都不会返回true
     *
     * @return {@code true} if all tasks have completed following shut down 如果所有任务都在关闭后完成，则返回true
     */
    // 所有的任务是否都已经终止，是的话，返回 true
    boolean isTerminated();

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever happens first.
     * 翻译：阻塞直到所有任务执行完毕。当任意以下情况出现则停止阻塞（以先发生为准）：收到关闭请求、超时、线程中断。
     *
     * @param timeout the maximum time to wait 等待的最长时间
     * @param unit the time unit of the timeout argument timeout参数的时间单位
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination 如果执行程序终止返回true，如果超时在终止之前结束返回false
     * @throws InterruptedException if interrupted while waiting 如果在等待时中断则抛出InterruptedException
     */
    // 在超时时间内阻塞，等待所有的任务执行完成
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing(代表) the pending results of the task(等待结果的任务). The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     * 翻译：提交一个有返回值的任务去执行，并返回一个Future来代表等待任务的结果。Future的get方法将在成功完成任务后返回任务的结果
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     * 翻译：如果你想立即阻塞等待一个任务，可以使用表单的结构：result = exec.submit(aCallable).get();
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     * 翻译：注意：Executors类包含一系列方法，可以转换其他一些类似关闭的公共对象，例如，PrivilegedAction to Callable表单以便它们能被提交。
     *
     * @param task the task to submit 提交的Callable任务
     * @param <T> the type of the task's result 任务结果的类型
     * @return a Future representing pending completion of the task 返回一个Future来代表等待任务的结果
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution 如果无法调度任务执行则抛出RejectedExecutionException
     * @throws NullPointerException if the task is null 如果任务为空则抛出NullPointerException
     */
    // 提交有返回值的任务，使用 get 方法可以阻塞等待任务的结果返回
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a Runnable task for execution and returns a Future representing that task.
     * The Future's {@code get} method will return the given result upon successful completion.
     * 翻译：提交一个Runnable任务去执行，并返回一个Future代表那个任务。Future的get方法将在成功完成时返回给定的结果
     *
     * @param task the task to submit 提交的Runnable任务
     * @param result the result to return 返回的结果
     * @param <T> the type of the result 结果的类型
     * @return a Future representing pending completion of the task 返回一个Future来代表等待任务的结果
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution 如果无法调度任务执行则抛出RejectedExecutionException
     * @throws NullPointerException if the task is null 如果任务为空则抛出NullPointerException
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     * 翻译：提交一个Runnable任务去执行，并返回一个Future代表那个任务。Future的get方法将在成功完成时返回null
     *
     * @param task the task to submit 提交的Runnable任务
     * @return a Future representing pending completion of the task 返回一个Future来代表等待任务的结果
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution 如果无法调度任务执行则抛出RejectedExecutionException
     * @throws NullPointerException if the task is null 如果任务为空则抛出NullPointerException
     */
    // 提交没有返回值的任务，如果使用 get 方法的话，任务执行完之后得到的是 null 值
    Future<?> submit(Runnable task);

    /**
     * Executes the given tasks, returning a list of Futures holding their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.(每个Future的状态是isDone的时候才返回)
     * 执行给定的任务，返回一个Futures列表，包含所有任务完成时的状态和结果。返回的列表中每个Future的状态都是isDone
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given collection is modified while this operation is in progress.
     * 注意，已完成的任务可以是正常终止的，也可以是抛出异常终止的。
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *         scheduled for execution
     */
    // 给定任务集合，返回已经执行完成的 Future 集合，每个返回的 Future 都是 isDone = true 的状态
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     * @throws InterruptedException if interrupted while waiting, in
     *         which case unfinished tasks are cancelled
     * @throws NullPointerException if tasks, any of its elements, or
     *         unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *         for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Executes the given tasks, returning the result of one that has completed successfully (i.e., without throwing an exception),
     * if any do. Upon normal or exceptional return,tasks that have not completed are cancelled.
     * (有一个成功返回了就返回，抛异常算取消)
     * The results of this method are undefined if the given collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks or any element task
     *         subject to execution is {@code null}
     * @throws IllegalArgumentException if tasks is empty
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    // 给定任务中有一个执行成功就返回，如果抛异常，其余未完成的任务将被取消
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     * @return the result returned by one of the tasks
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element
     *         task subject to execution is {@code null}
     * @throws TimeoutException if the given timeout elapses before
     *         any task successfully completes
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
