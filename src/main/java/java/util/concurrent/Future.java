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
 * 代表着异步的计算，提供了计算是否完成的 check，等待完成，取回等多种方法
 * A {@code Future} represents the result of an asynchronous
 * computation.  Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation.
 * 翻译：Future表示一个异步计算的结果。它的方法可用于检查计算是否完成，等待计算的完成，并检索计算结果。
 *
 * 得到结果可以使用 get，会一直阻塞到结果计算完成。
 * The result can only be retrieved using method
 * {@code get} when the computation has completed, blocking if
 * necessary until it is ready.
 * 翻译：当计算完成时，才可以使用get方法检索结果，如果有必要，将进行阻塞，直到计算完成为止。
 *
 * 取消可以使用 cancel，一旦计算完成，就无法被取消了
 * Cancellation is performed by the
 * {@code cancel} method.  Additional methods are provided to
 * determine if the task completed normally or was cancelled.
 * 翻译：取消由cancel方法执行。还提供了其他方法来确定任务是正常完成还是被取消。
 *
 * Once a computation has completed, the computation cannot be cancelled.
 * 翻译：一旦计算完成，计算就不能被取消。
 *
 * If you would like to use a {@code Future} for the sake
 * of cancellability but not provide a usable result, you can
 * declare types of the form {@code Future<?>} and
 * return {@code null} as a result of the underlying task.
 * 翻译：如果你为了可取消性而使用Future，它除了提供一个可用的结果之外，也可以声明Future的类型为Future<?>，它会返回null作为一个底层任务的结果
 *
 * <p>
 * <b>Sample Usage</b> (Note that the following classes are all
 * made-up.)
 * 翻译：示例用法(注意以下类都是虚构的)。
 * <pre> {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}</pre>
 *
 * The {@link FutureTask} class is an implementation of {@code Future} that
 * implements {@code Runnable}, and so may be executed by an {@code Executor}.
 * For example, the above construction with {@code submit} could be replaced by:
 * 翻译：FutureTask类是一个Future的实现，也实现了Runnable，因此可以通过Executor来执行。例如，上述submit结构可以替换为：
 *  <pre> {@code
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}</pre>
 *
 * <p>Memory consistency effects: Actions taken by the asynchronous computation
 * <a href="package-summary.html#MemoryVisibility"> <i>happen-before</i></a>
 * actions following the corresponding {@code Future.get()} in another thread.
 * 翻译：内存一致性效应：在另一个线程中，异步计算的操作发生在对应的Future.get()操作之前。
 *
 * @see FutureTask
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this Future's {@code get} method 通过这个Future的get方法返回的结果类型
 */
public interface Future<V> {

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason.
     * If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.
     * If the task has already started,
     * (中断并不会立马成功，列举了成功和失败的场景)
     * then the {@code mayInterruptIfRunning} parameter determines(决心)
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     * 翻译：试图取消此任务的执行。如果任务已经完成，已经取消，或者由于其他原因无法取消，则此尝试将失败。如果调用成功，并且调用cancel方法时任务还没有启动，则此任务不应该运行。如果任务已经开始，mayInterruptIfRunning参数用来确定执行此任务的线程是否应该在试图停止该任务时被中断。
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     * 翻译：此方法返回后，对isDone方法的后续调用将始终返回true。如果此方法返回true，那么对iscancel的后续调用将始终返回true。
     * 如果方法已经返回了，isDone和isCancelled都会返回true
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete 如果执行该任务的线程应该被中断则参数为true；否则，正在进行的任务将被允许完成
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise 如果任务不能被取消，典型的场景比如说任务已经正常完成了，会返回false；反之返回true
     */
    // 如果任务已经成功了，或已经取消了，是无法再取消的，会直接返回取消成功(true)
    // 如果任务还没有开始进行时，发起取消，是可以取消成功的。
    // 如果取消时，任务已经在运行了，mayInterruptIfRunning 为 true 的话，就可以打断运行中的线程
    // mayInterruptIfRunning 为 false，表示不能打断直接返回
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     * 翻译：如果任务被取消之前已经正常完成了会返回true。
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    // 返回线程是否已经被取消了，true 表示已经被取消了
    // 线程如果已经运行结束了，isCancelled 和 isDone 返回的都是 true
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     * 翻译：如果这个任务完成了会返回true。
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     * 完成可能是由于正常的终止，一个异常，或者取消 -- 在所有这些情况下，此方法将返回true。
     *
     * @return {@code true} if this task completed
     */
    // 线程是否已经运行结束了
    boolean isDone();

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves(取回) its result.
     * 翻译：如果需要，会等待计算的完成，然后取回计算结果
     *
     * @return the computed result 计算结果
     * @throws CancellationException if the computation was cancelled 如果计算被取消则抛出CancellationException
     * @throws ExecutionException if the computation threw an
     * exception 如果计算出异常则抛出ExecutionException
     * @throws InterruptedException if the current thread was interrupted
     * while waiting 如果当前线程在等待时被中断则抛出InterruptedException
     */
    // 等待结果返回
    // 如果任务呗取消了，抛 CancellationException 异常
    // 如果等待过程中被打断了，抛 InterruptedException 异常
    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     * 翻译：如果需要，将最多等待给定的时间来完成计算，如果可用，然后取回结果。
     *
     * @param timeout the maximum time to wait 等待的最大时间
     * @param unit the time unit of the timeout argument timeout参数的时间单位
     * @return the computed result 计算结果
     * @throws CancellationException if the computation was cancelled 如果计算被取消则抛出CancellationException
     * @throws ExecutionException if the computation threw an
     * exception 如果计算出异常则抛出ExecutionException
     * @throws InterruptedException if the current thread was interrupted
     * while waiting 如果当前线程在等待时被中断则抛出InterruptedException
     * @throws TimeoutException if the wait timed out 如果等待超时则抛出TimeoutException
     */
    // 等待，但是带有超时时间的，如果超时时间外仍然没有响应，抛 TimeoutException 异常
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
