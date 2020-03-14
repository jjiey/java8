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
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * ExecutorService 使用线程池中的线程执行提交的任务，线程池我们可以使用 Executors 进行配置
 * An {@link ExecutorService} that executes each submitted task using
 * one of possibly several pooled threads, normally configured
 * using {@link Executors} factory methods.
 * 翻译：一个ExecutorService使用线程池中几个可用的线程之一来执行每个提交的任务，通常使用Executors的工厂方法来配置。
 *
 * 线程池解决两个问题：1：通过减少任务间的调度开销(线程池中的线程被重复使用)，来提高大量任务时的执行性能；
 * 2：提供了一种方式来管理线程和消费，维护基本数据统计，比如已完成的任务数
 * <p>Thread pools address two different problems: they usually
 * provide improved performance(性能) when executing large numbers of
 * asynchronous(异步) tasks, due to(由于) reduced(减少) per-task invocation overhead(每个任务的调度开销),
 * and they provide a means of(一种方式) bounding and managing the resources,(对线程边界和管理)
 * including threads, consumed when executing a collection of tasks.
 * Each {@code ThreadPoolExecutor} also maintains some basic
 * statistics, such as the number of completed tasks.（基本的统计）
 * 翻译：线程池设法解决两个不同的问题：由于减少了每个任务调用的开销，它们通常在执行大量异步任务时提供了更好的性能，并且它们提供了一种绑定和管理资源(包括执行一批任务时消耗的线程)的方法。每个ThreadPoolExecutor还维护一些基本的统计信息，比如完成任务的数量。
 *
 *
 * Executors 为常用的场景设定了可直接拿到线程池的方法，比如 Executors#newCachedThreadPool 无界的线程池，并且可以自动回收；
 * Executors#newFixedThreadPool 固定大小线程池；Executors#newSingleThreadExecutor 单个线程线程池。
 * <p>To be useful across a wide range of contexts, this class
 * provides many adjustable parameters and extensibility
 * hooks. However, programmers are urged to use the more convenient
 * {@link Executors} factory methods {@link
 * Executors#newCachedThreadPool} (unbounded thread pool, with
 * automatic thread reclamation), {@link Executors#newFixedThreadPool}
 * (fixed size thread pool) and {@link
 * Executors#newSingleThreadExecutor} (single background thread), that
 * that preconfigure settings for the most common usage scenarios.
 * Otherwise, use the following guide when manually
 * configuring and tuning this class:
 * 翻译：要在广泛的上下文中有用，这个类有许多可调参数和可扩展性钩子。然而,程序员被敦促使用更方便的Executors类的工厂方法Executors#newCachedThreadPool（无界线程池，自动回收线程），Executors#newFixedThreadPool（固定大小的线程池），Executors#newSingleThreadExecutor（单个后台线程），为最常见的使用场景预配置设置。否则，在手动配置和调优这个类时，请使用以下guide：
 *
 * 为了在各种上下文中使用线程池，线程池提供可供扩展的参数设置：
 *
 *
 * 1：coreSize：当新任务提交时，发现运行的线程数小于 coreSize，一个新的线程将被创建，即使这时候其他工作线程是空闲的，可以通过 getCorePoolSize 方法获得 coreSize；
 * 2：maxSize:当心任务提交时，coreSize < 运行线程数 < maxSize，但队列没有满时，任务提交到队列中，如果队列满了，在 maxSize 允许的范围内新建线程。
 *
 *
 * <dl>
 *
 * <dt>Core and maximum pool sizes</dt>
 * 翻译：核心和最大线程池大小
 *
 * <dd>A {@code ThreadPoolExecutor} will automatically adjust the
 * pool size (see {@link #getPoolSize})
 * according to the bounds set by
 * corePoolSize (see {@link #getCorePoolSize}) and
 * maximumPoolSize (see {@link #getMaximumPoolSize}).
 * 翻译：一个ThreadPoolExecutor将根据corePoolSize（getCorePoolSize方法）和maximumPoolSize（getMaximumPoolSize方法）设置的范围大小自动调整线程池的大小（getPoolSize方法）
 *
 * When a new task is submitted in method {@link #execute(Runnable)},
 * and fewer than corePoolSize threads are running, a new thread is
 * created to handle the request,even if other worker threads are idle.
 * 翻译：当一个新线程通过execute(Runnable)方法被提交时，正在运行的threads少于corePoolSize的值，即使其它线程是空闲的，也会创建一个新线程来处理请求。
 *
 * If there are more than corePoolSize but less than
 * maximumPoolSize threads running, a new thread will be created only
 * if the queue is full.
 * By setting corePoolSize and maximumPoolSize
 * the same, you create a fixed-size thread pool.
 * By setting
 * maximumPoolSize to an essentially unbounded value such as {@code
 * Integer.MAX_VALUE}, you allow the pool to accommodate an arbitrary
 * number of concurrent tasks.
 * 翻译：如果正在运行的threads多于corePoolSize的值但少于maximumPoolSize的值，只有当队列是满的时候，将会创建一个新线程。通过将corePoolSize和maximumPoolSize的值设置相同，你将创建一个固定大小的线程池。通过将maximumPoolSize设置为一个实质上无界的值比如Integer.MAX_VALUE，你将允许线程池容纳一个任意数量的并发任务。
 *
 * 一般来说，coreSize 和 maxSize 在线程池初始化时就已经设定了，但我们也可以通过 setCorePoolSize、setMaximumPoolSize 方法动态的修改这两个值。
 *
 * Most typically, core and maximum pool
 * sizes are set only upon construction, but they may also be changed
 * dynamically using {@link #setCorePoolSize} and {@link
 * #setMaximumPoolSize}. </dd>
 * 翻译：最典型的情况下，corePoolSize和maximumPoolSize的大小仅在构建时设置，但是也可以用setCorePoolSize方法和setMaximumPoolSize方法动态的改变它们。
 *
 * <dt>On-demand construction</dt>
 * 翻译：按需构建
 *
 * 默认的，core threads 需要是任务提交后才创建的，但我们可以分别使用 prestartCoreThread、prestartAllCoreThreads 两个方法来提前创建一个、所有的 core threads
 * <dd>By default, even core threads are initially created and
 * started only when new tasks arrive, but this can be overridden
 * dynamically using method {@link #prestartCoreThread} or {@link
 * #prestartAllCoreThreads}.  You probably want to prestart threads if
 * you construct the pool with a non-empty queue. </dd>
 * 翻译：默认的，即使核心线程最初也只是在新任务到达时才创建和启动，但是可以使用prestartCoreThread方法或prestartAllCoreThreads方法动态地覆盖它。如果使用非空队列构建线程池，则可能需要预启动线程。
 *
 * <dt>Creating new threads</dt>
 * 翻译：创建新线程
 *
 * 新的线程被 ThreadFactory 创建,优先级会被限制成 NORM_PRIORITY，默认会被设置成非守护线程，这个和新建线程的继承是不同的
 * <dd>New threads are created using a {@link ThreadFactory}.  If not
 * otherwise specified, a {@link Executors#defaultThreadFactory} is
 * used, that creates threads to all be in the same {@link
 * ThreadGroup} and with the same {@code NORM_PRIORITY} priority and
 * non-daemon status. By supplying a different ThreadFactory, you can
 * alter the thread's name, thread group, priority, daemon status,
 * etc. If a {@code ThreadFactory} fails to create a thread when asked
 * by returning null from {@code newThread}, the executor will
 * continue, but might not be able to execute any tasks. Threads
 * should possess the "modifyThread" {@code RuntimePermission}. If
 * worker threads or other threads using the pool do not possess this
 * permission, service may be degraded(分解): configuration changes may not
 * take effect in a timely manner, and a shutdown pool may remain in a
 * state in which termination is possible but not completed.</dd>
 * 翻译：新的线程被用ThreadFactory创建，如果没有特别说明，则默认使用的ThreadFactory为Executors#defaultThreadFactory，它创建的线程都在相同的ThreadGroup中，并且具有相同的NORM_PRIORITY优先级，都是非守护状态。通过提供不同的ThreadFactory，你可以更改线程的名称、线程组、优先级、守护进程状态等。如果ThreadFactory通过从newThread返回null创建线程失败，执行程序将继续执行，但可能无法执行任何任务。线程应该拥有“modifyThread”许可（RuntimePermission）。如果使用的线程池中的工作线程或其他线程没有拥有这个许可，服务可能被降级：配置更改可能不会及时生效，关闭池可能仍然处于终止但尚未完成的状态。
 *
 * <dt>Keep-alive times</dt>
 * 翻译：存活时间
 *
 * 1：如果当前线程池中有超过 coreSize 的线程；2：并且线程空闲的时间超过 keepAliveTime，当前线程就会被回收。
 * <dd>If the pool currently has more than corePoolSize threads,
 * excess threads will be terminated if they have been idle for more
 * than the keepAliveTime (see {@link #getKeepAliveTime(TimeUnit)}).
 * 翻译：如果当前线程池有超过corePoolSize规定的线程，那么如果闲置时间超过keepAliveTime，多余的线程将被终止。
 *
 * 避免了线程没有被使用时的浪费，如果过会儿请求变活跃，可以再创建新线程
 * 通过 setKeepAliveTime 也是可以动态的设置
 * 如果设置 allowCoreThreadTimeOut 为 ture 的话，core thread 空闲时间超过 keepAliveTime 的话，也会被回收
 * This provides a means of reducing resource consumption when the
 * pool is not being actively used. If the pool becomes more active
 * later, new threads will be constructed. This parameter can also be
 * changed dynamically using method {@link #setKeepAliveTime(long,
 * TimeUnit)}.  Using a value of {@code Long.MAX_VALUE} {@link
 * TimeUnit#NANOSECONDS} effectively disables idle threads from ever
 * terminating prior to shut down. By default, the keep-alive policy
 * applies only when there are more than corePoolSize threads. But
 * method {@link #allowCoreThreadTimeOut(boolean)} can be used to apply this time-out policy to core threads as well, so long as the
  keepAliveTime value is non-zero. </dd>
 * 翻译：这提供了一种当线程池没有被活跃的使用时减少资源消耗的方法。如果线程池之后变得更活跃，新线程将被构造。这个参数也能被用setKeepAliveTime(long, TimeUnit)方法动态的改变。使用Long.MAX_VALUE值单位TimeUnit#NANOSECONDS有效禁止闲置线程在关闭之前终止。默认情况下，keep-alive策略仅适用于超过corePoolSize线程的情况。但allowCoreThreadTimeOut(boolean)方法也能将这个超时策略应用到核心线程，只要keepAliveTime的值不为0。
 *
 * <dt>Queuing</dt>
 * 翻译：排队论
 *
 * <dd>Any {@link BlockingQueue} may be used to transfer and hold
 * submitted tasks.  The use of this queue interacts with pool sizing:
 * 翻译：任何BlockingQueue都可以传输和阻塞提交的任务。此队列的使用与线程池大小的交互:
 *
 * <ul>
 *
 * <li> If fewer than corePoolSize threads are running, the Executor
 * always prefers adding a new thread
 * rather than queuing.</li>
 * 翻译：如果运行的线程小于corePoolSize，Executor总是宁愿添加一个新线程而不是排队。
 *
 * <li> If corePoolSize or more threads are running, the Executor
 * always prefers queuing a request rather than adding a new
 * thread.</li>
 * 翻译：如果正在运行corePoolSize或更多的线程，Executor总是宁愿对请求进行排队，而不是添加一个新线程。
 *
 * <li> If a request cannot be queued, a new thread is created unless
 * this would exceed maximumPoolSize, in which case, the task will be
 * rejected.</li>
 * 翻译：如果一个请求不能排队（可能是队列满了），那么将创建一个新线程，除非这个线程导致超过了maximumPoolSize，在这种情况下，任务将被拒绝。
 *
 * </ul>
 *
 * There are three general strategies for queuing:
 * 翻译：排队有三种基本策略：
 * <ol>
 *
 * 队列有三种情况：
 * 1：SynchronousQueue，为了避免任务被拒绝，要求线程池的 maxSize 无界，缺点是当任务提交的速度超过消费的速度时，可能出现无限制的线程增长。
 * 2：LinkedBlockingQueue，无界队列,未消费的任务可以在队列中等待；
 * 3：ArrayBlockingQueue，有界队列，可以防止资源被耗尽。
 *
 * 需要均衡线程池和队列之间的大小平衡
 *
 * <li> <em> Direct handoffs.</em> A good default choice for a work
 * queue is a {@link SynchronousQueue} that hands off tasks to threads
 * without otherwise holding them. Here, an attempt to queue a task
 * will fail if no threads are immediately available to run it, so a
 * new thread will be constructed. This policy avoids lockups when
 * handling sets of requests that might have internal dependencies.
 * Direct handoffs generally require unbounded maximumPoolSizes to
 * avoid rejection of new submitted tasks. This in turn admits the
 * possibility of unbounded thread growth when commands continue to
 * arrive on average faster than they can be processed.  </li>
 * 翻译：直接传递。工作队列的一个不错的默认选择是SynchronousQueue，它将任务交给线程，而不需要占用线程。在这里，如果没有立即可用的线程来运行任务，则对任务进行排队的尝试将失败，因此将构造一个新线程。此策略当处理可能具有内部依赖的一些请求时避免锁定。直接传递通常需要无界大小的线程池来避免拒绝新提交的任务。反过来，当命令到达的平均速度比它们被处理的速度还要快时，有可能出现无限的线程增长。
 *
 * <li><em> Unbounded queues.</em> Using an unbounded queue (for
 * example a {@link LinkedBlockingQueue} without a predefined
 * capacity) will cause new tasks to wait in the queue when all
 * corePoolSize threads are busy. Thus, no more than corePoolSize
 * threads will ever be created. (And the value of the maximumPoolSize
 * therefore doesn't have any effect.)  This may be appropriate when
 * each task is completely independent of others, so tasks cannot
 * affect each others execution; for example, in a web page server.
 * While this style of queuing can be useful in smoothing out
 * transient bursts of requests, it admits the possibility of
 * unbounded work queue growth when commands continue to arrive on
 * average faster than they can be processed.  </li>
 * 翻译：无界队列。当所有corePoolSize线程都繁忙时，用一个无界队列（例如一个没有预先定义容量的LinkedBlockingQueue）将造成新任务在队列里等待。因此，创建的线程不会超过corePoolSize。（maximumPoolSize的值因此没有任何用。）当每个任务完全独立于其他任务时这可能是合适的，因此任务不会影响其他任务的执行；例如，在一个web页面服务中。虽然这种类型的队列在平滑短暂的请求突发方面很有用，但它也承认，当命令平均到达速度超过处理速度时，可能会出现无限的工作队列增长。
 *
 * <li><em>Bounded queues.</em> A bounded queue (for example, an
 * {@link ArrayBlockingQueue}) helps prevent resource exhaustion when
 * used with finite maximumPoolSizes, but can be more difficult to
 * tune and control.  Queue sizes and maximum pool sizes may be traded
 * off for each other: Using large queues and small pools minimizes
 * CPU usage, OS resources, and context-switching overhead, but can
 * lead to artificially low throughput.  If tasks frequently block (for
 * example if they are I/O bound), a system may be able to schedule
 * time for more threads than you otherwise allow. Use of small queues
 * generally requires larger pool sizes, which keeps CPUs busier but
 * may encounter unacceptable scheduling overhead, which also
 * decreases throughput.  </li>
 * 翻译：有界队列。当使用有限的最大线程池时，一个有界队列（例如一个ArrayBlockingQueue）有助于防止资源耗尽，但是调优和控制可能更困难。队列大小和最大线程池大小可能被相互交换：使用大队列和小线程池可以最小化CPU使用，操作系统资源，和上下文切换的开销，但是会导致人为的低吞吐量。如果任务经常阻塞(例如它们受到I/O的限制)，系统可能为比你允许的更多的线程安排时间。使用小队列通常需要更大的线程池，这会使cpu更忙但可能会遭遇无法接受的调度开销，这也会降低吞吐量。
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>Rejected tasks</dt>
 * 翻译：拒绝任务
 *
 * 在 Executor 已经关闭或对最大线程和最大队列都使用饱和时，可以使用 RejectedExecutionHandler 类进行异常捕捉，有如下四种处理策略：
 * ThreadPoolExecutor.AbortPolicy、ThreadPoolExecutor.DiscardPolicy、ThreadPoolExecutor.CallerRunsPolicy、ThreadPoolExecutor.DiscardOldestPolicy
 *
 * <dd>New tasks submitted in method {@link #execute(Runnable)} will be
 * <em>rejected</em> when the Executor has been shut down, and also when
 * the Executor uses finite bounds for both maximum threads and work queue
 * capacity, and is saturated(饱和).  In either case(不管发生什么), the {@code execute} method
 * invokes the {@link
 * RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)}
 * method of its {@link RejectedExecutionHandler}.  Four predefined handler
 * policies are provided:
 * 翻译：当Executor已经被关闭，也当Executor的最大线程数和工作队列容量都使用有限的界限并且是饱和的时，用execute(Runnable)方法提交新任务将被拒绝。无论在哪种情况下，execute方法都将调用其RejectedExecutionHandler的RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)方法。提供四个预定义的处理策略:
 * <ol>
 *
 * <li> In the default {@link ThreadPoolExecutor.AbortPolicy}, the
 * handler throws a runtime {@link RejectedExecutionException} upon
 * rejection. </li>
 * 翻译：在默认的ThreadPoolExecutor.AbortPolicy下，处理程序在拒绝后会抛出一个运行时RejectedExecutionException。
 *
 * <li> In {@link ThreadPoolExecutor.CallerRunsPolicy}, the thread
 * that invokes itself runs the task. This provides a
 * simple feedback control mechanism that will slow down the rate that
 * new tasks are submitted. </li>
 * 翻译：在ThreadPoolExecutor.CallerRunsPolicy下，调用自身的线程运行任务。这提供了一个简单的反馈控制机制，这将降低新任务被提交的速率。
 *
 * <li> In {@link ThreadPoolExecutor.DiscardPolicy}, a task that
 * cannot be executed is simply dropped.  </li>
 * 翻译：在ThreadPoolExecutor.DiscardPolicy下，一个无法执行的任务将被丢弃。
 *
 * <li>In {@link ThreadPoolExecutor.DiscardOldestPolicy}, if the
 * executor is not shut down, the task at the head of the work queue
 * is dropped, and then execution is retried (which can fail again,
 * causing this to be repeated.) </li>
 * 翻译：在ThreadPoolExecutor.DiscardOldestPolicy下，如果执行程序没有关闭，工作队列头部的任务将被丢弃，然后执行被重试（可能再次失败，导致重复执行）。
 *
 * </ol>
 *
 * It is possible to define and use other kinds of {@link
 * RejectedExecutionHandler} classes. Doing so requires some care
 * especially when policies are designed to work only under particular
 * capacity or queuing policies. </dd>
 * 翻译：可以定义和使用其他类型的RejectedExecutionHandler类。当策略设计为仅在特定容量或队列策略下工作时，这样做需要特别谨慎。
 *
 * <dt>Hook methods</dt>
 * 翻译：钩子函数
 *
 * 1：提供在每个任务执行之前 beforeExecute 和执行之后 afterExecute 的钩子方法，主要用于操作执行环境，比如初始化 ThreadLocals、收集统计数据、添加日志条目等；
 * 2: 如果在执行器执行完成之后想干一些事情，可以实现 terminated 方法。
 * <dd>This class provides {@code protected} overridable
 * {@link #beforeExecute(Thread, Runnable)} and
 * {@link #afterExecute(Runnable, Throwable)} methods that are called
 * before and after execution of each task.  These can be used to
 manipulate the execution environment; for example, reinitializing
 * ThreadLocals, gathering statistics, or adding log entries.
 * Additionally, method {@link #terminated} can be overridden to perform
 * any special processing that needs to be done once the Executor has
 * fully terminated.
 * 翻译：这个类提供了protected可覆盖的beforeExecute(Thread, Runnable)和afterExecute(Runnable, Throwable)方法，这两个方法在执行每个任务之前和之后被调用。这些可以用来操作执行环境；例如，重新初始化ThreadLocals，收集统计信息，或添加日志条目。此外，可以重写terminated方法来执行一旦Executor已经完全终止后任何需要的特殊处理。
 *
 * <p>If hook or callback methods throw exceptions, internal worker
  threads may in turn fail and abruptly terminate.</dd>
 如果钩子方法执行时发生异常，工作线程可能会失败并立即终止。
 * 翻译：如果钩子或回调方法抛出异常，内部工作线程可能会依次失败并突然终止。
 *
 * 队列的维护：提供了 getQueue() 方法方便我们进行监控和调试，严禁用于其他目的，remove 和 purge 两个方法可以对队列进行操作。
 * <dt>Queue maintenance</dt>
 * 翻译：队列的维护
 *
 * <dd>Method {@link #getQueue()} allows access to the work queue
 * for purposes of monitoring and debugging.  Use of this method for
 * any other purpose is strongly discouraged.  Two supplied methods,
 * {@link #remove(Runnable)} and {@link #purge} are available to
 * assist in storage reclamation when large numbers of queued tasks
 * become cancelled.</dd>
 * 翻译：getQueue()方法允许访问工作队列，用于监视和调试的目的。强烈反对将此方法用于任何其他目的。当大量排队的任务被取消时，提供了两个方法remove(Runnable)和purge可用来帮助回收储存。
 *
 * <dt>Finalization</dt>
 * 翻译：终止
 *
 * 线程池将自动终止，并且线程池中的线程也会自动关闭，为了确保关闭，可以执行 shutdown 方法
 * <dd>A pool that is no longer referenced in a program AND has no remaining threads will be  shutdown automatically.
 * If you would like to ensure that unreferenced pools are reclaimed even if users forget to call {@link #shutdown},
 * then you must arrange
 * that unused threads eventually die, by setting appropriate
 * keep-alive times, using a lower bound of zero core threads and/or
 * setting {@link #allowCoreThreadTimeOut(boolean)}.  </dd>
 * 翻译：程序中不再引用并且没有剩余线程的线程池将被自动关闭。如果你希望确保即使用户忘记调用shutdown方法未引用的线程池也能被回收，那么你必须通过设置适当的keep-alive时间来安排未使用的线程最终死亡，用0个核心线程这个下限and/or设置allowCoreThreadTimeOut(boolean)。
 *
 * </dl>
 *
 * <p><b>Extension example</b>. Most extensions of this class
 * override one or more of the protected hook methods. For example,
 * here is a subclass that adds a simple pause/resume feature:
 * 翻译：扩展示例。该类的大多数扩展都会重写一个或多个protected的钩子函数。例如，这里有一个子类，它添加了一个简单的pause/resume特性:
 *
 *  <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ThreadPoolExecutor extends AbstractExecutorService {

    /**
     * ctl 线程池状态控制字段，由两部分组成：
     * 1:workerCount wc 低29位用来记录线程池工作线程数，我们限制workerCount最大值为 (2^29)-1，大概 5 亿个线程
     * 2:runState rs 高3位用来表示线程池状态，提供了生命周期的控制，源码中有很多关于状态的校验，状态枚举如下面的5种（具体看源码）：
     * 默认是RUNNING状态，线程个数为0
     * RUNNING（-536870912），SHUTDOWN（0），STOP（536870912），TIDYING（1073741824），TERMINATED（1610612736）
     *
     * The main pool control state, ctl, is an atomic integer packing two conceptual fields
     * workerCount, indicating the effective number of threads
     * runState, indicating whether running, shutting down etc
     * 翻译：主线程池控制状态tl是一个原子整数，它包含两个概念字段：workerCount，表示线程的有效运行状态数，表示是否运行、是否关闭等。
     *
     * 为了把两个概念压缩成 int，我们限制 workerCount 为 (2^29)-1
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in the future,
     * the variable can be changed to be an AtomicLong,and the shift/mask constants below adjusted.
     * But until the need arises, this code is a bit faster and simpler using an int.
     * 翻译：为了将它们打包成一个int型，我们将workerCount限制为(2^29)-1(大约5亿)个线程，而不是(2^31)-1(20亿)个线程。如果将来出现这个问题，可以将变量更改为一个AtomicLong，并调整下面的shift/mask常量。但是在需要之前，使用int可以使这段代码更快、更简单。
     *
     * The workerCount is the number of workers that have been permitted to start and not permitted to stop.
     * The value may be transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when asked,
     * and when exiting threads are still performing bookkeeping before terminating.
     * The user-visible pool size is reported as the current size of the workers set.
     * 翻译：workerCount是允许开始和不允许停止的工作线程数量。这个值可能与实际的活动线程数有短暂的不同，例如当一个ThreadFactory在被请求时创建线程失败，或者当退出的线程在终止之前仍在执行记账时。用户可见的线程池大小是当前工作线程集合的大小。
     *
     * The runState provides the main lifecycle control, taking on values:
     * 翻译：runState提供了主要的生命周期控制，具有以下值：
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING will run the terminated() hook method
     *   TERMINATED: terminated() has completed
     *
     * The numerical order among these values matters,
     * to allow ordered comparisons.
     * The runState monotonically increases over time,
     * but need not hit each state. The transitions are:
     *
     * runState 之间的转变过程：
     * RUNNING -> SHUTDOWN：调用 shutdown()，finalize()
     * (RUNNING or SHUTDOWN) -> STOP：调用shutdownNow()
     * SHUTDOWN -> TIDYING -> workerCount ==0
     * STOP -> TIDYING -> workerCount ==0
     * TIDYING -> TERMINATED -> terminated() 执行完成之后
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown(), perhaps implicitly in finalize()
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     * STOP -> TIDYING
     *    When pool is empty
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     * 翻译：当状态是TERMINATED时，在awaitterminate()方法中等待的线程将返回。
     *
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    /**
     * 29，作为下面计算的指数
     */
    private static final int COUNT_BITS = Integer.SIZE - 3;
    /**
     * workerCount 最大值：(2^29)-1 = 536870911
     */
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // Packing and unpacking ctl 包装和拆包ctl
    /**
     * 计算线程状态和线程个数的新值
     */
    private static int ctlOf(int rs, int wc) { return rs | wc; }
    /**
     * 获取线程池工作中的线程数
     */
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    /**
     * 获取线程池当前运行状态
     */
    private static int runStateOf(int c)     { return c & ~CAPACITY; }

    // runState is stored in the high-order bits runState以高阶位存储 be stored in（被存储在）
    /**
     * RUNNING：接受新任务并处理阻塞队列里的任务
     * -536870912
     */
    private static final int RUNNING    = -1 << COUNT_BITS;
    /**
     * SHUTDOWN：拒绝新任务，但仍处理已经在阻塞队列里的任务
     * 0
     */
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    /**
     * STOP：拒绝新任务，并抛弃阻塞队列里的任务，同时会中断正在处理的任务
     * 536870912
     */
    private static final int STOP       =  1 << COUNT_BITS;
    /**
     * TIDYING：所有任务都执行完（包含阻塞队列里的任务）后，当前线程池workerCount = 0，将要调用 terminated() 方法（terminated()方法是个扩展方法）
     * 1073741824
     */
    private static final int TIDYING    =  2 << COUNT_BITS;
    /**
     * TERMINATED：终止状态。terminated() 调用完成后的状态（terminated()方法是个扩展方法）
     * 1610612736
     */
    private static final int TERMINATED =  3 << COUNT_BITS;

    /**
     * 已完成任务的计数
     */
    volatile long completedTasks;
    /**
     * 线程池最大容量
     */
    private int largestPoolSize;
    /**
     * 已经完成的任务数
     */
    private long completedTaskCount;
    /**
     * 用户可控制的参数都是 volatile 修饰的
     * 可以使用 threadFactory 创建 thread
     * 创建失败一般不抛出异常，只有在 OutOfMemoryError 时候才会
     */
    private volatile ThreadFactory threadFactory;
    /**
     * 饱和或者运行中拒绝任务的 handler 处理类
     */
    private volatile RejectedExecutionHandler handler;
    /**
     * 线程存活时间设置
     */
    private volatile long keepAliveTime;
    /**
     * 设置 true 的话，核心线程空闲 keepAliveTime 时间后，也会被回收
     */
    private volatile boolean allowCoreThreadTimeOut;
    /**
     * coreSize
     */
    private volatile int corePoolSize;
    /**
     * maxSize 最大限制 CAPACITY = (2^29)-1 = 536870911
     */
    private volatile int maximumPoolSize;
    /**
     * 默认的拒绝策略
     */
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();
    /**
     * 队列会 hold 住任务，并且利用队列阻塞的特性，来保持线程的存活周期
     */
    private final BlockingQueue<Runnable> workQueue;
    /**
     * 大多数情况下是控制对 workers 的访问权限
     */
    private final ReentrantLock mainLock = new ReentrantLock();
    private final Condition termination = mainLock.newCondition();
    /**
     * 包含线程池中所有的工作线程
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * 维护着运行中的任务的线程锁和可中断状态
     * 线程池中任务执行的最小单元
     * Worker 继承 AQS，具有锁功能（不可重入独占锁）。在执行任务的时候，会锁住自己，任务执行完成之后，会释放自己
     * Worker 实现 Runnable，本身是一个可执行的任务
     */
    private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable
    {
        /**
         * 具体执行任务的线程
         */
        final Thread thread;
        /**
         * 该工作线程执行的第一个任务（该工作线程需要执行的任务）
         */
        Runnable firstTask;

        // 非常巧妙的设计，Worker本身是个 Runnable，把自己作为任务传递给 thread
        // 内部有个属性又设置了 Runnable
        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker 在调用runWorker()方法前禁止中断（因为当其它线程调用了线程池的shutdownNow()方法时，如果Worker状态 >= 0则会中断该线程。在runWorker()方法里会执行w.unlock()方法来把state设置成0，此时就允许中断该Worker线程）
            this.firstTask = firstTask;
            // 把 Worker 自己作为 thread 运行的任务。执行run方法时，其实执行的是firstTask的run方法
            this.thread = getThreadFactory().newThread(this);
        }

        /**
         * Worker 本身是 Runnable，run 方法是 Worker 执行的入口， runWorker 是外部 ThreadPoolExecutor 的方法。执行该方法时，其实执行的是firstTask的run方法
         */
        public void run() {
            runWorker(this);
        }

        private static final long serialVersionUID = 6138294804551838833L;

        // Lock methods
        // 0 代表没有锁住，1 代表锁住
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        // 尝试加锁，CAS 赋值为 1，表示锁住
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 尝试释放锁，释放锁没有 CAS 校验，可以任意的释放锁
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }







    /**
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     *
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     */
    private static final  RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");

    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     * 翻译：尝试去CAS减少ctl的workerCount字段。
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * do...while确保一定能成功 - 1
     *
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     * 翻译：减少ctl的workerCount字段。这仅在一个线程突然终止时被调用(请参阅processWorkerExit)。其它的减少在getTask中执行。
     */
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }


    /*
     * Methods for setting control state
     */

    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *        (but not TIDYING or TERMINATED -- use tryTerminate for that)
     */
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            // 如果当前线程池状态 >= SHUTDOWN直接返回，否则设置为SHUTDOWN状态
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 尝试设置线程池状态为TERMINATED
     *
     * Transitions to TERMINATED state if either (SHUTDOWN and pool and queue empty) or (STOP and pool empty).
     * If otherwise eligible to terminate but workerCount is nonzero,
     * interrupts an idle worker to ensure that shutdown signals propagate.
     * This method must be called following any action that might make termination possible
     * reducing worker count or removing tasks from the queue during shutdown.
     * The method is non-private to allow access from ScheduledThreadPoolExecutor.
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // 设置当前线程池状态为TIDYING
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        // 扩展接口
                        terminated();
                    } finally {
                        // 设置当前线程池状态为TERMINATED
                        ctl.set(ctlOf(TERMINATED, 0));
                        // 激活因调用条件变量termination的await系列方法而被阻塞的所有线程
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     */

    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     */
    private void checkShutdownAccess() {
        // 是否设置了安全管理器
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            // 当前调用shutdown方法的线程是否有关闭线程池的权限
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    // 调用线程是否有中断工作线程的权限。如果没有权限则抛出异常
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 设置所有空闲线程的中断标志（或者理解为 回收空余线程）
     *
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 循环回收，onlyOne=false，说明要回收很多个
            for (Worker w : workers) {
                Thread t = w.thread;
                // 如果工作线程没有被中断，并且没有正在运行（worker可以获得锁），则当前线程可以设置中断标志
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        // 建议线程中断
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     */
    void onShutdown() {
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /*
     * Methods for creating, running and cleaning up after workers
     */

    /**
     * 结合线程池的情况看是否可以添加新的 worker
     * break retry：跳到retry处，且不再进入循环
     * continue retry：跳到retry处，且再次进入循环
     * 主要分为两部分代码，见注释
     *
     * @param firstTask 不为空可以直接执行，为空执行不了，因为在Thread.run()方法有判断，Runnable为空不执行
     * @param core true 表示线程最大新增个数是 coreSize，false 表示最大新增个数是 maxSize
     * @return 返回 true 代表成功，false 失败
     *
     * 三种情况会返回false：如果线程池状态异常 或 工作线程数超过限制 或 任务启动失败即w添加到workers里失败
     * 主要步骤：
     * 1.通过CAS操作增加工作线程数。如果线程池状态异常 或 工作线程数超过限制（和参数core有关系，见参数注释） 则返回false
     * 2.1.加独占锁，添加w到workers里
     * 2.2.添加w到workers里成功，启动w。只有启动成功才返回true，如果启动失败即w添加到workers里失败 返回false
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        /* 第一部分：这个双重for循环的目的是通过CAS操作增加工作线程数 */
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary. 仅在必要时检查队列是否为空。
            // if条件等价于：rs >= SHUTDOWN && (rs != SHUTDOWN || firstTask != null || workQueue.isEmpty())。也就是说，下面三种情况下会返回false：
            // 1.当前线程池状态为 STOP，TIDYING，TERMINATED 三个状态任意一个
            // 2.当前线程池状态为 SHUTDOWN 并且已经有了第一个任务（因为 SHUTDOWN 状态拒绝新任务）
            // 3.当前线程池状态为 SHUTDOWN 并且任务队列为空（因为 SHUTDOWN 状态拒绝新任务，但仍处理已经在阻塞队列里的任务，而 firstTask 不在阻塞队列里）
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;

            // 循环CAS增加线程个数
            for (;;) {
                // 获取工作线程数
                int wc = workerCountOf(c);
                // 判断线程个数是否超过限制，超过限制返回false
                // 代码思路：工作中的线程数 >= 最大容量 || 工作中的线程数 >= coreSize or maxSize
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                // CAS增加线程个数，同时只有一个线程能成功
                if (compareAndIncrementWorkerCount(c))
                    // 成功后，跳转到retry位置，不再进入外层 for 循环，即退出双层 for 循环
                    break retry;
                // CAS失败了，再看线程池状态是否发生变化了，如果变化了则再次跳到外层 for 循环重新尝试获取线程池状态，否则在内层循环继续重新进行CAS尝试
                c = ctl.get();  // Re-read ctl
                // 如果线程池状态被更改
                if (runStateOf(c) != rs)
                    // 跳转到retry位置，再次进入外层 for 循环
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop 否则由于workerCount改变导致CAS失败，重试内层循环
            }
        }

        // 到这里说明上面代码CAS成功了
        /* 第二部分：主要是把任务并发安全的添加到workers里面，并且启动任务执行 */
        // w是否启动成功，也是方法最后返回的值
        boolean workerStarted = false;
        // 添加w到workers里是否成功
        boolean workerAdded = false;
        Worker w = null;
        try {
            // 巧妙的设计，Worker 本身是个 Runnable
            // 在初始化的过程中，会把 worker 丢给 thread 去初始化
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                /* 第二部分第一小部分：加独占锁，添加w到workers里 */
                // 加独占锁，为了实现workers的同步，因为可能多个线程调用了线程池的execute方法
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    // 重新检查线程池状态，为了避免在获取锁前其它线程调用了shutdown()方法关闭了线程池。如果线程池已经被关闭，则释放锁，新增线程失败
                    int rs = runStateOf(ctl.get());

                    // 线程池是RUNNING状态 || 线程池是SHUTDOWN状态并且第一个任务为空
                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        // 添加工作线程任务
                        workers.add(w);
                        int s = workers.size();
                        // 更新线程池最大容量，largestPoolSize只在这里赋值
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                /* 第二部分第二小部分：添加w到workers里成功，启动w */
                // 如果工作线程添加成功，则启动任务
                if (workerAdded) {
                    // 启动线程，实际上是去执行 Worker.run 方法
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     *   worker was holding up termination
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 执行善后清理工作
     *
     * Performs cleanup and bookkeeping for a dying worker.
     * Called only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account for exit.
     * This method removes thread from worker set,
     and possibly terminates the pool or replaces the worker
     if either it exited due to user task exception or if fewer than
      corePoolSize workers are running or queue is non-empty but there are no workers.
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 添加任务时workerCount已经+1了，completedAbruptly=true表示消费的时候出异常了
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 统计整个线程池完成的任务个数
            completedTaskCount += w.completedTasks;
            // 从工作集里删除当前Worker
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        // 尝试设置线程池状态为TERMINATED
        // 如果当前状态是SHUTDOWN并且工作队列为空 或 当前状态是STOP，当前线程池里没有活动线程
        tryTerminate();

        // 如果当前线程个数 < coreSize，则增加一个线程
        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            addWorker(null, false);
        }
    }

    /**
     * 从阻塞队列中拿任务
     *
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out? 上次poll()超时了吗?

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // 如果是以下两种情况，就直接返回：
            // 1.线程池状态为STOP，TIDYING，TERMINATED
            // 2.线程池状态为SHUTDOWN，并且阻塞队列为空（从阻塞队列中拿不到任务）
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                // CAS让WorkerCount - 1
                decrementWorkerCount();
                // return 之后，该线程就执行结束了，JVM 会自动回收该线程
                return null;
            }

            int wc = workerCountOf(c);

            // Are workers subject to culling? workers会被淘汰吗?
            // 如果核心线程也允许被超时回收 || 运行的线程数大于 coreSize（这种情况本来就需要被超时回收）
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            // 以 LinkedBlockingQueue 队列为例
            // timedOut 为 true 说明：poll() 方法执行返回的是 null，即说明在等待 keepAliveTime 时间后，队列中仍然没有数据，即说明此线程已经空闲了 keepAliveTime 了
            // (线程池线程数 > maxSize || 该线程允许被超时回收且已经超时) && (线程池线程数 > 1 || 阻塞队列为空)
            // if条件等价于：(wc > maximumPoolSize && wc > 1) || (wc > maximumPoolSize && workQueue.isEmpty()) || (1 < wc <= maximumPoolSize && (timed && timedOut)) || (wc <= maximumPoolSize && (timed && timedOut) && wc <= 1 && workQueue.isEmpty())
            if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
                // 满足if条件后，尝试CAS使线程池数量减少 1
                if (compareAndDecrementWorkerCount(c))
                    // return 之后，该线程就执行结束了，JVM 会自动回收该线程
                    return null;
                continue;
            }

            try {
                // 从队列中拿 worker
                // poll()过超时时间会返回null，而这个超时时间正是 keepAliveTime；take()会一直阻塞
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
                if (r != null)
                    return r;
                // 到这里，说明经过 keepAliveTime 时间也没有获取到任务，即 poll() 方法超时，也说明此时队列没有数据
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }


    /**
     * 不断运行的 worker，重复的从队列中获取任务并执行，除此之外，还执行：
     * Main worker run loop.  Repeatedly gets tasks from queue and executes them,
     * while coping with a number of issues:
     *
     * 1. We may start out with an initial task,
     * in which case we don't need to get the first one.
     * Otherwise, as long as pool is running, we get tasks from getTask.
     * If it returns null then the worker exits due to changed pool state or configuration parameters.
     * Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        // 帮助gc回收
        w.firstTask = null;
        w.unlock(); // allow interrupts 允许中断（此时将state设置为0）
        boolean completedAbruptly = true;
        try {
            // task 为空的情况：
            // 1.任务入队列了，极限情况下，发现没有运行的线程，允许的话于是会新增一个线程（execute方法中入队后进行二次检查会做这件事）
            // 2.线程执行完任务后，再次回到 while 循环，那么这时就会去阻塞队列里拿任务来运行。会重复的执行这个while循环，从阻塞队列里拿任务来运行
            // 如果 task 为空 || 使用 getTask() 从阻塞队列中拿数据，如果拿不到数据，会在这里阻塞住
            while (task != null || (task = getTask()) != null) {
                // 锁住worker。执行任务期间加锁，是为了避免在任务运行期间，其他线程调用了shutdown()方法后正在执行的任务被中断，因为shutdown只会中断当前被阻塞挂起的线程
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                // 线程池stop中，但是线程没有到达中断状态，帮助线程中断
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    // 执行before钩子函数
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        // 同步执行任务，其实执行的是FutureTask.run()，所以可以用FutureTask.get()获取执行结果 或 异常
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        // 执行after钩子函数，如果这里抛出异常，会覆盖catch的异常，所以这里的异常最好不要抛出来
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    // 任务执行完成，统计当前Worker完成了多少个任务
                    w.completedTasks++;
                    // 释放锁
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            // 执行善后清理工作
            processWorkerExit(w, completedAbruptly);
        }
    }


    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        // 入参校验，其中需要注意的是 keepAliveTime 不能为负数
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * Executes the given task sometime in the future.
     * The task may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution,
     * either because this executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute 要执行的任务，类型其实是FutureTask
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null 如果command为空则抛出NullPointerException
     *
     * 主要步骤：
     * 1.当前线程池中工作线程数 < corePoolSize（核心线程数），则创建新的核心线程运行，运行成功则返回，addWorker失败或者当前线程池中工作线程数 >= corePoolSize则进入2；
     * 2.如果当前线程池是 RUNNING 状态 && 阻塞队列不满，则添加任务到阻塞队列；添加成功后会进行二次检查，
     * 3.如果当前线程池不是RUNNING状态 || 队列满导致入队失败，则新增线程运行，运行成功则返回，addWorker失败执行拒绝策略
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps:
         * 翻译：分三步进行
         *
         * 1. If fewer than corePoolSize threads are running,
         * try to start a new thread with the given command as its first task.
         * The call to addWorker atomically checks runState and workerCount,
         * and so prevents false alarms that would add threads when it shouldn't, by returning false.
         * 翻译：如果正在运行的线程数小于 corePoolSize，新建线程
         *
         * 2. If a task can be successfully queued,
         * then we still need to double-check whether we should have added a thread
         * (because existing ones died since last checking) or
         *  that the pool shut down since entry into this method.
         * So we recheck state and if necessary roll back the enqueuing if stopped,
         * or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new thread.
         * If it fails, we know we are shut down or saturated and so reject the task.
         */
        int c = ctl.get();
        // 当前线程池中线程个数 < corePoolSize（核心线程数），则创建新的核心线程运行，运行成功返回，addWorker失败不抛异常
        if (workerCountOf(c) < corePoolSize) {
            // addWorker 第二个参数为 true 表示线程最大新增个数是 coreSize
            if (addWorker(command, true))
                return;
            // 如果创建新的线程并运行成功或线程池状态发生变化，ctl的值都会发生变化，所以再取一次最新值
            c = ctl.get();
        }
        // 到这里说明：工作的线程 >= corePoolSize（核心线程数），或者addWorker失败
        // 如果当前线程池是RUNNING状态 && 阻塞队列不满，则添加任务到阻塞队列
        if (isRunning(c) && workQueue.offer(command)) {
            /* 入队后进行二次检查，主要是为了防止线程池的状态发生变化 或者 工作线程数为 0 */
            int recheck = ctl.get();
            // 如果当前线程池状态不是RUNNING，则从队列中移除任务，移除后执行拒绝策略
            if (!isRunning(recheck) && remove(command))
                reject(command);
            // 如果（当前线程池状态是RUNNING || 当前线程池状态不是RUNNING但从队列中移除任务失败） && 当前线程池为空（工作线程数是 0），那么就添加一个线程运行
            // 这里是个极限情况，入队后，突然发现可用线程都被回收了
            else if (workerCountOf(recheck) == 0)
                // Runnable是空的，不会影响新增线程，但是线程在 start 的时候不会运行，因为在 Thread.run() 里面有判断
                // addWorker 第二个参数为 false 表示线程最大新增个数是 maxSize
                addWorker(null, false);
        }
        // 如果当前线程池不是RUNNING状态 || 队列满导致入队失败，则新增线程运行，如果addWorker失败说明有可能线程池中的线程个数 > maxSize，则执行拒绝策略
        // addWorker 第二个参数为 false 表示线程最大新增个数是 maxSize
        else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * 调用该方法后，线程池就不会再接受新任务了，但是工作队列里的任务还是要执行的。该方法会立刻返回，并不等待队列任务完成再返回
     *
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 权限检查
            checkShutdownAccess();
            // 设置当前线程池状态为SHUTDOWN，如果已经是SHUTDOWN则直接返回
            advanceRunState(SHUTDOWN);
            // 设置中断标志
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        // 尝试设置线程池状态为TERMINATED
        tryTerminate();
    }

    /**
     * 调用该方法后，线程池就不会再接受新任务了，并且会丢弃工作队列里的任务，正在执行的任务会被中断，该方法会立刻返回，并不等待激活的任务执行完成
     *
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @return 此时队列里被丢弃的任务列表
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 权限检查
            checkShutdownAccess();
            // 设置线程池状态为STOP
            advanceRunState(STOP);
            // 中断所有工作线程。包含空闲线程和正在执行任务的线程
            interruptWorkers();
            // 将队列任务移动到tasks中
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }

    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    /**
     * 当线程调用该方法后，当前线程会被阻塞，知道线程池状态变为TERMINATED或者等待时间超时才返回
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                // 判断当前线程池状态是否至少是TERMINATED，如果是，直接返回。否则说明当前线程池里面还有线程在执行
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                // nanos <= 0 说明不需要等待，那就直接返回
                if (nanos <= 0)
                    return false;
                // 等待nanos时间，期望在这段时间里线程池状态变为TERMINATED
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     */
    protected void finalize() {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * 如果新设置的值小于 coreSize，多余的线程在空闲时会被回收（不保证一定可以回收成功）
     * 如果大于 coseSize，会新创建线程
     *
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        // 活动的线程数大于新设置的核心线程数
        if (workerCountOf(ctl.get()) > corePoolSize)
            // 尝试将可以获得锁的 worker 中断，只会循环一次
            // 最后并不能保证活动的线程数一定小于核心线程数
            interruptIdleWorkers();
        // 设置的核心线程数大于原来的核心线程数
        else if (delta > 0) {
            // 并不清楚应该新增多少线程，取新增核心线程数和等待队列数据的最小值，够用就好
            int k = Math.min(delta, workQueue.size());
            // 新增线程直到k，如果期间等待队列空了也不会再新增
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);
    }

    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * @return the number of threads started
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out,
     *         else {@code false}
     *
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *         and the current keep-alive time is not greater than zero
     *
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * 如果 maxSize 大于原来的值，直接设置。
     * 如果 maxSize 小于原来的值，尝试干掉一些 worker
     *
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     *
     * @param time the time to wait.  A time value of zero will cause
     *        excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *         if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* User-level queue utilities */

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /* Statistics */

    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * @return the number of tasks
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                     (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                      "Shutting down"));
        return super.toString() +
            "[" + rs +
            ", pool size = " + nworkers +
            ", active threads = " + nactive +
            ", queued tasks = " + workQueue.size() +
            ", completed tasks = " + ncompleted +
            "]";
    }

    /* Extension hooks */

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * @param t the thread that will run task {@code r}
     * @param r the task that will be executed
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     * execution completed normally
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     */
    protected void terminated() { }

    /* Predefined RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() { }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }

    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() { }

        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
