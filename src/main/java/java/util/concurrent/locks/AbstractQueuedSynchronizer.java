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

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

/**
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 * 提供了一种框架，用于实现依赖于先进先出(FIFO)等待队列的阻塞锁和相关同步器(信号量、事件等)。对于大多数依赖单个原子值来表示状态的同步器来说，这个类被设计为一个有用的基础类。子类必须定义受保护的方法来更改此状态，并定义该状态在被获取或释放的对象中的意义。考虑到这些，这个类中的其他方法执行所有排队和阻塞机制。子类可以维护其他状态字段，但是只能操作使用 getState、 setState 和 compareAndSetState 方法对原子地更新 int 值进行同步跟踪。
 *
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 * 子类应该被定义为非公开的内部帮助类用于实现，用于实现它们的封闭类的同步属性。AbstractQueuedSynchronizer 类不实现任何同步接口。相反，它定义了像 acquireInterruptibly 这样的方法，具体的锁和相关的同步器可以适当地调用这些方法来实现它们的公共方法。
 *
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 * 该类支持默认的独占模式和共享模式之一或两者。当以独占模式获取时，其他线程的尝试获取将无法成功。共享模式下多个线程获取可能(但不需要)成功。该类并不“理解”这些区别，除了在机械的意义上，（这些区别是）即当一个共享模式获取成功时，下一个等待线程(如果存在的话)也必须确定它是否也可以获取。不同模式等待的线程共享同一个 FIFO 队列。通常，子类实现只支持其中一种模式，但是这两种模式都可以发挥作用，例如 ReadWriteLock 类。只支持排他模式或只支持共享模式的子类不需要定义支持未使用模式的方法。
 *
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 * 该类定义了一个嵌套的 ConditionObject 类，它可以被支持独占模式的子类用做 Condition 的实现，这种情况下，isHeldExclusively 方法会报告关于当前线程的同步是否被独占（with respect to 关于；至于），使用当前 getState 方法的返回值调用的 release 方法会完全释放该对象（release(int)方法），acquire 方法给定此保存的状态值，最终将该对象恢复到之前获得时的状态。AbstractQueuedSynchronizer 类没有方法能创建这样一个 condition，所以如果不能满足此约束，就不要使用它。ConditionObject 的行为当然也取决于其同步器实现的语义。
 *
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 * 该类为内部队列提供检查、检测和监视的方法，以及用于条件对象的类似方法。这些可以根据需要来用 AbstractQueuedSynchronizer 导出到类中以获得同步机制。
 *
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 * 该类的序列化只存储底层原子整数维护状态，因此反序列化的对象具有空线程队列。需要序列化的典型子类将定义一个 readObject 方法，该方法在反序列化时将序列化恢复到已知的初始状态。
 *
 * <h3>Usage</h3>
 * 用法
 *
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 * 要使用这个类作为同步器的基础，可以通过使用 getState、setState 和/或 compareAndSetState 方法检查和/或修改同步状态来重新定义以下方法(如适用)：
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 * 这些方法（上面那 5 个）默认情况下都会抛出 UnsupportedOperationException。这些方法的实现必须是内部线程安全的，并且通常应该简短而不阻塞。定义这些方法是使用该类的唯一受支持的方法。所有其他方法都被声明为 final，因为它们不能独立变化。
 *
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 * 你可能还会发现从 AbstractOwnableSynchronizer 继承的方法对于跟踪拥有独占同步器的线程很有用。鼓励您使用它们——这使监控和诊断工具能够帮助用户确定哪些线程持有锁。
 *
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 * 即使这个类基于内部 FIFO 队列，它也不会自动执行 FIFO 获取策略。独占同步的核心形式是:
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        如果尚未排队，使线程入队
 *        <em>possibly block current thread</em>;
 *        可能阻塞当前线程
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 *        取消阻塞第一个排队的线程
 * </pre>
 *
 * (Shared mode is similar but may involve cascading signals.)
 * 共享模式相似，但可能涉及级联信号。
 *
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 * (乱入，获取顺序问题)因为检查能否获取是在入队之前调用的，所以一个新的获取线程可能会比其他被阻塞和排队的线程提前到达（barge：乱闯）。但是，如果需要，您可以定义 tryAcquire 和/或 tryAcquireShared 方法来通过内部调用一个或多个检查方法来禁用这种乱入，从而提供一个公平的 FIFO 获取顺序。特别是，如果 hasQueuedPredecessors (一个专门设计供公平同步器使用的方法)方法返回 true，那么大多数公平同步器可以定义 tryAcquire 方法来返回 false。还有可能出现其他变化。
 *
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 * 默认的乱入 (也称为贪婪（贪心）、放弃（拒绝）和 convoy-avoidance （避免护航？）)策略的吞吐量和可伸缩性通常最高。虽然不能保证这是公平的或无饥饿性的，但是允许较早排队的线程在较晚排队的线程之前重新竞争，并且每次重新竞争都有一个公平的机会可以成功地对抗（预防，抵御）进入的线程。另外，虽然 acquire 方法不像通常意义上的“自旋”，但是它们可能在阻塞之前执行多次 tryAcquire 方法的调用，并穿插其他计算。当独占同步只是短暂地进行时，这就提供了自旋的大部分好处，而当它不是时，却没有承担大部分责任（负债；债务？）。如果需要的话，您可以通过调用 acquire 方法之前使用“快速路径”检查来增加这一点，可能预先检查 hasContended 和/或 hasQueuedThreads 方法，仅在如果同步器很可能不竞争时才这样做。
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 * 该类为同步提供了一个有效的、可扩展的基础，部分是通过将其使用范围专门化（专门用于）到可以依赖于 int 状态、获取和释放参数以及内部 FIFO 等待队列的同步器。当这还不够时，您可以使用 java.util.concurrent.atomic 类，您自定义的 java.util.Queue 类和 LockSupport 类阻塞支持从较低的级别构建同步器。
 *
 * <h3>Usage Examples</h3>
 * 用法示例
 *
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 * 这是一个不可重入互斥锁类，它使用值 0 表示 unlocked 状态，使用值 1 表示 locked 状态。虽然不可重入锁并不严格要求记录当前所有者线程，但这个类无论如何都会这样做，以便更容易监视使用情况。它还支持条件并公开（暴露）一种检测方法：
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   // 我们的内部帮助类
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     // 报告是否处于锁定状态
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     // 如果 state 为零，则获取锁
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused 否则未使用
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     // 通过将 state 设置为零来释放锁
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused 否则未使用
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     // 提供一个 Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     // 正确反序列化
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state 重置为 unlocked 状态
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   // 同步对象完成了所有的困难工作。我们只是期待它。
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 * 这里有一个类似于 java.util.concurrent.CountDownLatch 的门闩类，除了（只不过）它只需要一个信号 signal 方法来触发。因为门闩类是非排他的，所以它使用 shared 获取和释放方法。
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
// park 挂起   unpark 唤醒
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     * 创建一个新的 AbstractQueuedSynchronizer 实例，其初始同步状态为 0。
     */
    protected AbstractQueuedSynchronizer() { }

    // 同步队列：获取不到锁时，让线程等待的等待队列
    // 同步队列节点定义，线程进入队列时，会被 Node 所包装
    // 同步队列本身就是一个队列，想想队列章节很多队列的实现
    // 入队时会加上 CLH lock(自旋实现)，被加到队尾
    // 出队时，取队头
    /**
     * Wait queue node class.
     * 等待队列节点类。
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     * 等待队列是“CLH”(Craig, Landin 和 Hagersten)锁队列的变体。CLH锁通常用于自旋锁。相反，我们使用它们来阻塞同步器，但使用相同的基本策略，即在线程节点的前驱节点中保存关于线程的一些控制信息。每个节点的“状态”字段跟踪一个线程是否应该阻塞。当节点的前驱节点释放时，它会被发信号（来告知）。队列中的每个节点均充当一个特定通知样式的监视器，其中包含一个等待线程。状态字段并不控制线程是否被授予锁等等。如果线程节点是队列中的第一个，那么该线程可能会尝试获取锁。但第一个并不能保证成功；它只是被给予了竞争的权利。因此当前释放的竞争者线程可能需要重新等待。
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * 要进入 CLH 锁队列，您需要将其作为新的 tail 原子地拼接上。要出队，您只需设置头部字段。
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     * 插入到 CLH 队列只需要对“tail”执行一个单原子操作，因此从未排队到排队有一个简单的原子分界点。类似地，出队只涉及更新“head”。但是，节点需要更多的工作来确定谁是它们的后继节点，部分原因是为了处理由于超时和中断而可能导致的取消。
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     * “prev”连接(在原始的 CLH 锁中没使用)主要用于处理取消。如果一个节点被取消，它的后继节点(通常)会被重新连接到一个未取消的前驱节点。对于自旋锁的类似机制的解释，请参阅 Scott 和 Scherer 的论文，网址为 http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     * 我们还使用“next”连接来实现阻塞机制。我们还使用“next”连接来实现阻塞机制。每个节点的线程 id 保存在它自己的节点中，因此前驱节点通过遍历下一个连接来确定它是哪个线程，从而发信号去唤醒下一个节点。确定后继节点必须避免与新排队的节点竞争，以设置它们前驱节点的“next”字段。当一个节点的后继节点为空时，通过从原子更新的“tail”向后检查，必要时可以解决这个问题。(或者，换句话说，next-links 是一种优化，以便我们通常不需要向后扫描。)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     * Cancellation（取消；删除）在基本算法中引入了一些保守性。由于我们必须轮询其他节点的取消，因此我们可能没注意到一个已取消的节点在我们前面还是后面。解决这个问题的方法是在取消时始终唤醒后继节点，允许它们在一个新的前驱上稳定下来，除非我们能确定一个未取消的前驱将承担这一责任。
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     * CLH 队列需要一个虚拟头节点来开始。但我们不在建设中创造它们，因为如果没有竞争，这样就是在白费力气。相反，在第一次竞争时创建节点并设置头尾指针。
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     * 在 Conditions 中等待的线程使用相同的节点，但使用额外的连接。Conditions 只需要去连接简单(非并发)连接队列中的节点，因为它们只有在独占持有时才会被访问。在等待时，节点被插入到条件队列中。收到信号后，节点被传输到主队列。status 字段的一个特殊值被用来标记节点在哪个队列上。
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     * 感谢 Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer 和 Michael Scott，以及 JSR-166 专家组的成员们，对这个类的设计提出有帮助的想法、讨论和评论。
     */
    static final class Node {
        // node 是共享模式
        // 标记该线程是获取【共享】资源时被阻塞挂起后放入AQS队列的
        /**
         * Marker to indicate a node is waiting in shared mode
         * 表明节点正在共享模式下等待的标记
         */
        static final Node SHARED = new Node();
        // node 是排它模式
        // 标记该线程是获取【独占】资源时被阻塞挂起后放入AQS队列的
        /**
         * Marker to indicate a node is waiting in exclusive mode
         * 表明节点正在独占模式下等待的标记
         */
        static final Node EXCLUSIVE = null;

        // waitStatus 的状态有以下几种
        // 线程被取消
        /**
         * waitStatus value to indicate thread has cancelled
         * waitStatus 的值，表示线程已取消
         */
        static final int CANCELLED =  1;
        // 线程需要被唤醒。该状态的意义：同步队列中的节点在自旋获取锁的时候，如果前一个节点的状态是 SIGNAL，那么自己就可以阻塞休息了，否则自己会一直自旋尝试获得锁
        /**
         * waitStatus value to indicate successor's thread needs unparking
         * waitStatus 的值，表示后继的线程需要唤醒
         */
        static final int SIGNAL    = -1;
        // 表示当前 node（线程）正在条件队列里面等待，当有节点从同步队列转移到条件队列时，状态就会被赋值（更改）成 CONDITION
        /**
         * waitStatus value to indicate thread is waiting on condition
         * waitStatus 的值，表示线程正在条件队列里等待
         */
        static final int CONDITION = -2;
        // 释放共享资源时需要通知其他节点
        // 表示后续结点会传播唤醒的操作，共享模式下起作用
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         * waitStatus 的值，表示下一个 acquireShared 应该无条件传播
         */
        static final int PROPAGATE = -3;

        // 记录当前节点（线程）的等待状态，通过节点的状态来控制节点的行为
        // 普通同步节点，就是 0 ，条件节点是 CONDITION = -2
        /**
         * Status field, taking on only the values:
         * 状态字段，仅采用以下值：
         *   SIGNAL:     The successor of this node is (or will soon be)
         *               blocked (via park), so the current node must
         *               unpark its successor when it releases or
         *               cancels. To avoid races, acquire methods must
         *               first indicate they need a signal,
         *               then retry the atomic acquire, and then,
         *               on failure, block.
         *               这个节点的后继被(或即将被)阻塞(通过 park)，因此当前节点在释放或取消时必须唤醒它的后继。为了避免竞争，acquire 方法必须首先表明它们需要一个信号，然后重试原子获取，当失败时，阻塞。
         *   CANCELLED:  This node is cancelled due to timeout or interrupt.
         *               Nodes never leave this state. In particular,
         *               a thread with cancelled node never again blocks.
         *               该节点由于超时或中断被取消。节点不会离开这个状态。特别是，取消节点的线程不会再次阻塞。
         *   CONDITION:  This node is currently on a condition queue.
         *               It will not be used as a sync queue node
         *               until transferred, at which time the status
         *               will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the
         *               field, but simplifies mechanics.)
         *               此节点当前处于一个条件队列中。在转移之前，它不会被用作同步队列节点，此时状态将被设置为 0。(这里使用这个值与该字段的其他用途无关，但简化了机制。)
         *   PROPAGATE:  A releaseShared should be propagated to other
         *               nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation
         *               continues, even if other operations have
         *               since intervened.
         *               一次 releaseShared 应该被传播到其他节点。在 doReleaseShared 中设置这个(仅针对头节点)以确保传播继续，即使其他操作此后进行干预。
         *   0:          None of the above
         *               以上都不是
         *
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         * 数值按数字排列以简化使用。非负值意味着节点不需要信号。所以，大多数代码不需要检查特定的值，只需要检查符号。
         *
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         * 对于普通同步节点，该字段被初始化为0，对于条件节点，该字段被初始化为 CONDITION。使用 CAS 修改它(或者在可能的情况下，无条件的 volatile 写操作)。
         */
        volatile int waitStatus;

        // 当前节点的前驱节点
        // 节点被 acquire 成功后就会变成head
        // head 节点不能被 cancelled
        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         * 当前节点/线程依赖连接前驱节点来检查 waitStatus。入队时分配，只有在出队时才为空(为了 GC)。另外，在取消一个前驱节点时，我们在找到一个未取消的节点时短路，因为头节点从未被取消过，所以这个头节点始终存在: 只有在成功获取后，一个节点才会成为头节点。一个被取消的线程永远不会成功获取，并且线程只取消自己，而不取消任何其他节点。
         */
        volatile Node prev;

        // 当前节点的后继节点
        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         * 当前节点/线程在释放时唤醒连接的后继节点。入队时分配，在绕过取消的前驱节点时进行调整，在出队后无效(为了 GC)。直到连接之后，enq 操作才会分配一个前驱的下一个字段，因此看到一个空的下一个字段并不一定意味着节点在队列的末尾。但是，如果下一个字段出现是 null，我们可以从尾部扫描 prev 来再次检查。已取消节点的下一个字段被设置为指向节点自身，而不是 null，以使 isOnSyncQueue 更加容易。
         */
        volatile Node next;

        // 当前节点所代表的线程
        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         * 入队此节点的线程。在构造时初始化，使用后无效。
         */
        volatile Thread thread;

        // 在同步队列中，nextWaiter 并不表示其下一个节点元素，用 next 表示其下一个节点元素，nextWaiter 只是表示当前 Node 是排他模式还是共享模式
        // 在条件队列中，nextWaiter 就是表示其下一个节点元素
        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         * 连接到下一个等待条件的节点，或特殊值 SHARED。因为条件队列只有在独占模式持有时才会被访问，所以我们只需要一个简单的连接队列来在节点等待条件时持有它们。然后将它们转移到队列中重新获取。因为条件只能是排他的，所以我们保存一个字段通过使用特殊值来表示共享模式。
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         * 如果节点在共享模式下等待，返回 true。
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         * 返回前一个节点，如果前一个节点为 null 则抛出 NullPointerException。当前驱节点不为空时使用。可以省略 null 检查，但可以用来帮助 VM。
         *
         * @return the predecessor of this node 此节点的前驱节点
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker 用于建立初始 head 或 SHARED 标记
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter 被 addWaiter 方法调用（同步队列）
            // 如上所说，在同步队列中，nextWaiter 只是表示当前 Node 是排他模式还是共享模式
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition 被 Condition 调用（条件队列）
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    // 同步队列的头
    // 公平的锁先入先出
    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     * 等待队列的头，延迟初始化。除初始化外，只能通过 setHead 方法进行修改。
     * 注意：如果 head 存在，则保证其 waitStatus 肯定不为 CANCELLED。
     */
    private transient volatile Node head;

    // 等待队列的尾
    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     * 等待队列的尾，延迟初始化。只能通过 enq 方法进行修改以添加新的等待节点。
     */
    private transient volatile Node tail;

    // 同步器的状态，子类会根据当前状态字段进行判断是否可以获得锁
    // 比如 CAS 成功给 state 赋值 1 算得到锁，赋值失败为得不到锁，CAS 成功给 state 赋值 0 算释放锁，赋值失败为释放失败
    // 如果当前state是0，那么可以获得锁
    // 可重入锁，每次获得锁 +1，每次释放锁 -1
    // 最重要的属性，所有继承 AQS 的锁都是通过这个字段来判断能不能获得锁，能不能释放锁
    /**
     * The synchronization state.
     * 同步状态。
     */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * 返回同步状态的当前值。此操作具有 volatile 读的内存语义。
     * @return current state value 返回当前状态值
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * 设置同步状态的值。此操作具有 volatile 写的内存语义。
     * @param newState the new state value 返回新状态值
     */
    protected final void setState(int newState) {
        state = newState;
    }

    // CAS 设置同步器的状态。
    // 和其他 CAS 设置一样，这里并不是修改 state 的值，而是设置 stateOffset 的值
    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value 期望值
     * @param update the new value 新值
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     *         如果成功，则返回 true。返回 false 表明实际值不等于期望值。
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        // 请参阅下面的内部函数设置以支持此操作
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities
    // 排队工具

    // 自旋超时阀值，单位纳秒
    // 当设置等待时间时才会用到这个属性
    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     * 快速自旋的纳秒数，而不是用定时挂起。粗略估计足够在非常短的超时时间内提高响应能力。
     */
    static final long spinForTimeoutThreshold = 1000L;

    // 线程加入同步队列中方法，追加到队尾
    // 这里需要重点注意的是，返回值是添加 node 的前一个节点
    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     * 将节点插入队列，必要时进行初始化。参见上图。
     * @param node the node to insert 要插入的节点
     * @return node's predecessor 节点的前驱
     */
    private Node enq(final Node node) {
        for (;;) {
            // 得到队尾节点
            Node t = tail;
            // 如果队尾为空，说明当前同步队列都没有初始化，进行初始化
            if (t == null) { // Must initialize 必须初始化
                // 初始化同步队列时，会先设置一个哨兵节点new Node()，waitStatus == 0
                if (compareAndSetHead(new Node()))
                    // tail 和 head 都指向哨兵节点
                    tail = head;
                // 如果队尾不为空，将当前节点追加到队尾
            } else {
                node.prev = t;
                // node 追加到队尾
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    // 方法主要目的：node 追加到同步队列的队尾
    // 主要思路（双向链表）：
    // 1.新 node.prev = 队尾
    // 2.队尾.next = 新 node
    /**
     * creates and enqueues node for current thread and given mode.
     * 为当前线程和给定模式创建并入队节点。
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared Node.EXCLUSIVE 表示独占模式, Node.SHARED 表示共享模式
     * @return the new node 新节点
     */
    private Node addWaiter(Node mode) {
        // 初始化 Node
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // 尝试 enq 的快速路径；失败时备份到 full enq
        // 这里的逻辑和下面的 enq 方法一致，enq 的逻辑仅仅多了队尾是空，初始化的逻辑
        // 这个思路在java源码中很常见，先简单的尝试放一下，成功立马返回，如果不行，再while循环
        // 很多时候，这种算法可以帮忙解决大部分的问题，大部分的入队可能一次都能成功，无需自旋
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        // 自旋保证node加入到队尾
        enq(node);
        return node;
    }

    // node 排它模式下获得锁的节点
    // 排他模式下，获得锁的节点，一定会被设置成头节点
    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     * 设置节点为队列头，从而出队。仅通过 acquire 方法调用。为了 GC 并抑制不必要的信号和遍历，还可以清空未使用的字段。
     *
     * @param node the node 节点
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    // 当线程释放锁成功后或者共享模式获取锁后，从 node 开始唤醒同步队列中的节点
    // 通过唤醒机制，保证线程不会一直在同步队列中阻塞等待
    // @param node 当前释放锁的节点，也是同步队列的头节点
    /**
     * Wakes up node's successor, if one exists.
     * 如果存在，唤醒节点的后继。
     *
     * @param node the node 节点
     */
    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         * 如果 status 为负数（即可能需要信号），尝试清除预期的信号。如果失败或等待线程状态被更改，则可以。
         */
        int ws = node.waitStatus;
        // 如果节点不是CANCELLED状态，把节点的状态置为初始化 0
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        // 取头节点的后继节点
        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         * 唤醒线程将保留在后继中，通常只是下一个节点。但是，如果已取消或明显为 null，请从尾部向后遍历以找到实际的未取消后继。
         */
        Node s = node.next;
        // 队列中没有其他等待的节点 || 头节点的后继节点s的状态是CANCELLED
        // 就从队尾开始，向前遍历，找到第一个 waitStatus 不是 CANCELLED 的
        if (s == null || s.waitStatus > 0) {
            s = null;
            // 这里使用尾迭代的真正原因：是为了照顾刚刚加入到队列中的节点。见addWaiter()和enq()方法，可以发现节点入队不是一个原子操作，具体原因见笔记
            // 使用尾迭代找到node后边第一个状态不是CANCELLED的节点，唤醒它之后，在acquireQueued方法里第一次自旋循环在shouldParkAfterFailedAcquire方法里会把这个（或者node节点的后继连续多个）状态是CANCELLED的节点给删掉，然后第二次自旋循环就能去获取锁了
            // 如果直接唤醒状态是CANCELLED节点会怎样？看源码里好像倒是也能唤醒，但是CANCELLED这个状态本身好像就没有意义了。而且这个方法能够保证唤醒的一定不是CANCELLED状态的节点
            // for循环结束条件：t != null && t != 头节点
            for (Node t = tail; t != null && t != node; t = t.prev)
                // 如果节点 t 的状态不是 CANCELLED
                if (t.waitStatus <= 0)
                    s = t;
        }
        // 如果(s != null && s.waitStatus <= 0)，唤醒s；否则唤醒以上代码找到的s
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    // 自旋释放所有后继共享节点
    // 该方法有两处调用：
    // 1.acquireShared() -> doAcquireShared() -> 获取到锁后setHeadAndPropagate()
    // 2.releaseShared()
    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     * 共享模式下的释放动作-信号后继并确保传播。（注意：对于独占模式，如果需要信号，释放仅相当于调用 head 的 unparkSuccessor。）
     */
    private void doReleaseShared() {
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         * 即使有其他正在进行中的获取/释放，也要确保释放传播。如果需要信号，将以尝试 head 的 unparkSuccessor 的常规方式进行。但是，如果不需要，则将状态设置为 PROPAGATE，以确保释放时继续传播。此外，在执行此操作时，我们必须循环以防一个新节点被添加。另外，与 unparkSuccessor 的其他用法不同，我们需要知道 CAS 重置状态是否失败，如果失败，则重新检查。
         */
        for (;;) {
            Node h = head;
            /*
            这个if条件表面上看，是判断还没有到队尾，即此时队列中至少有两个节点时成立
            其实细想，还隐含了一种成立的情况：假如当前队列只有两个节点，有可能是第二个节点刚刚加进来，也就是说acquire()或者acquireShared()时，addWaiter()里compareAndSetTail()成功了，但是shouldParkAfterFailedAcquire()里compareAndSetWaitStatus()还没有成功。这个情况其实很重要，是理解这个if里面的else if的关键
             */
            if (h != null && h != tail) {
                // 获取 head 节点状态
                int ws = h.waitStatus;
                // 如果 head 节点状态是 SIGNAL，说明后续节点都需要唤醒
                if (ws == Node.SIGNAL) {
                    // CAS 保证只有一个线程可以运行唤醒的操作
                    // 前边说了，线程可以安心阻塞等待的标准，就是前一个节点线程状态是 SIGNAL，所以这里把 SIGNAL 改成 0，只要不是 SIGNAL，那后边的线程就能被唤醒了
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases 循环以重新检查 cases
                    // 进行唤醒操作，唤醒之后，在 doAcquireShared() 里又会自旋去获取锁，如果获取到锁了，就会再来这里唤醒下一个节点
                    unparkSuccessor(h);
                }
                /*
                if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))这个判断条件一定要好好的说一说：
                前提：同步队列中的节点状态只会有：0，SIGNAL，CANCELLED，PROPAGATE；如果是CANCELLED和PROPAGATE，在这里什么操作都不会做；如果是0和SIGNAL在这里才有意义
                （1）先明确两个问题：
                节点状态什么时候是0？shouldParkAfterFailedAcquire() 最终会设置前一个节点状态是 SIGNAL，所以一个正常的同步队列里的结点状态应该是这样的：SIGNAL <=> SIGNAL <=> SIGNAL <=> 0，所以同步队列的最后一个节点状态是0
                compareAndSetWaitStatus(h, 0, Node.PROPAGATE)什么时候会失败？在执行这个操作的瞬间，ws此时已经不为0了，说明有新的节点入队了，ws的值被改为了 SIGNAL
                （2）这里的ws为0是指：当前队列的最后一个节点成为了头节点。
                这里会不会还有一种可能，就是多线程同时调用该方法，有一个线程在上面把head的状态改成0，另一个线程再次循环走到这里？有可能，但是没关系，因为如果发生这种情况，说明线程1执行成功compareAndSetWaitStatus(h, Node.SIGNAL, 0)后cpu时间片用完了，线程2执行到(ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))把节点状态改成了PROPAGATE。但是线程1肯定会继续执行唤醒操作，把它的后一个节点唤醒去获取锁。如果没获取到锁，会把它的前一个节点状态再设置成SIGNAL；如果获取到锁，会把自己设置为head，然后再进入该方法，但是此时自己作为head的状态是SIGNAL了
                总结：所以这个if的意思其实是：当前队列的最后一个节点成为了头节点，但是正准备把它的状态设置为PROPAGATE时，有新的节点入队了，ws的值被改为了 SIGNAL。这种情况下，就 continue，以便在下次循环中能够再将这个刚刚新入队但准备挂起的线程唤醒。因此这算是一个优化，可以加速唤醒后继节点的过程
                所以这个if其实是描述了一个极其严苛且短暂的状态：
                1.大前提是队列里至少有两个节点
                2.执行到这里时，说明当前队列的最后一个节点成为了头节点；而此时有新的节点入队了，新的节点需要执行shouldParkAfterFailedAcquire()把ws的值改成 SIGNAL，但是目前这个修改操作还没有来的及执行；这时满足了这个if的前半段条件
                3.接下来if的后半段条件!compareAndSetWaitStatus(h, 0, Node.PROPAGATE)要成立的话，说明ws的状态不是0了，说明之前在上一步那个没有来得及执行的shouldParkAfterFailedAcquire()执行完了，将ws的值修改为了SIGNAL
                由此可见，这个if的 && 连接了两个不一致的状态，分别对应了shouldParkAfterFailedAcquire()中的compareAndSetWaitStatus(pred, ws, Node.SIGNAL)执行成功前和执行成功后
                因为当前方法doReleaseShared()和shouldParkAfterFailedAcquire()是可以并发执行的，所以这个条件是有可能满足的，只是满足的条件非常严苛，可能只是一瞬间的事
                作者竟然连这种情况都进行了优化，加速唤醒后继节点的过程，这真的是666到不行

                PROPAGATE状态也能理解了吧，其实就是介于0和SIGNAL之间的一个过度状态
                 */
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS 在失败的 CAS 上循环
            }
            // 第一种情况：头节点没有发生变化，结束
            // 第二种情况：因为此方法可以被两处调用，见注释；而获得锁的地方那里 setHeadAndPropagate() 里，会改变头节点；加上共享锁的特性就是可以多个线程获得锁，也可以释放锁，这就导致头节点可能会发生变化
            // 即，只有在当前head没有变化时，才会退出，否则继续循环
            if (h == head)                   // loop if head changed 如果 head 变了则循环
                break;
        }
    }

    // 主要做两件事情
    // 1：把当前节点设置成头节点
    // 2：看看后继节点有无正在等待，并且也是共享模式的，有的话唤醒这些节点
    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     * 设置队列头，并检查后继者是否可能在共享模式下等待，如果是，则传播是否 propagate > 0 或 PROPAGATE 状态被设置
     *
     * @param node the node 节点
     * @param propagate the return value from a tryAcquireShared tryAcquireShared 方法的返回值
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below 记录老的头节点为了下面的检查
        // 获得锁后，把当前节点设置为同步队列的 head 节点，即当前节点成为了新的哨兵节点（为什么说它也是一个哨兵节点？因为setHead方法里只是把head的指针指向了当前节点，里面的属性都设置为null了）
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         * 如果以下情况，尝试发信号给下一个排队的节点：
         *   Propagation 由调用者指示，或者由上一个操作记录(作为 setHead 之前或之后的 h.waitStatus)
         *   (注意：这将使用 waitStatus 的符号检查，因为 PROPAGATE 状态可能会转换为 SIGNAL。)
         * 和
         *   下一个节点正在共享模式下等待，或者我们不知道，因为它显示为 null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         * 这两项检查中的保守性可能会导致不必要的唤醒，但仅当有多个竞争获取/释放时，因此无论现在还是不久，大多数都需要信号。
         */
        // propagate > 0 表示已经有节点获得共享锁了。如果是从doAcquireShared()走到这里，propagate本来就是 >= 0
        // 已经有节点获得共享锁 || 老的头节点为空 || 老的头节点状态不是CANCELLED || 新的头节点为空 || 新的头节点状态不是CANCELLED
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {
            // 取该节点的后继节点
            Node s = node.next;
            // s 节点不为空，且是共享模式
            if (s == null || s.isShared())
                // 继续唤醒头节点的后继节点
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire
    // 各种获取版本的工具

    /**
     * Cancels an ongoing attempt to acquire.
     * 取消一个正在进行的获取尝试。
     *
     * @param node the node 节点
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        // 如果 node 不存在，则忽略
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors 跳过已取消前驱
        // 从后往前遍历，拿到node的前一个状态不是CANCELLED的节点，赋值给pred
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        // predNext 是明显要取消拼接的节点。如果没有，CASes 以下情况将失败，在这种情况下，我们输掉了竞争 vs 另一个取消或发出信号，因此无需采取进一步措施。
        // 拿到pred原本的下一个节点，这个节点不一定是node自己，因为虽然node的prev指向了pred，但是pred的下一个不一指向node
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        // 可以在此处使用无条件写入代替 CAS。在这个原子步骤之后，其他节点可以跳过我们。以前，我们不受其他线程的干扰。
        // node的状态设置为CANCELLED
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        // 如果我们是 tail，就移除自己。
        // 如果node是tail，就设置pred为新的tail
        if (node == tail && compareAndSetTail(node, pred)) {
            // 设置pred的next指向predNext
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            // 如果后继需要信号，请尝试设置 pred 的下一个连接以便获得一个。否则唤醒它来传播
            int ws;
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL ||
                 (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC 帮助 GC
        }
    }

    // 当前线程可以安心阻塞等待的标准，就是前一个节点状态是 SIGNAL
    // 关键操作：
    // 1：确认前驱节点是否有效（状态不是CANCELLED），无效的话，一直往前找到有效的（状态不是CANCELLED）节点。
    // 2：把前置节点状态置为 SIGNAL。
    // 1、2 两步操作，有可能一次就成功，有可能需要外部循环多次才能成功（外面是个无限的 for 循环），但最后一定是可以成功的
    // 这里有一个点：该方法最终会设置前一个节点状态是 SIGNAL，所以一个正常的同步队列里的结点状态应该是这样的：SIGNAL <=> SIGNAL <=> SIGNAL <=> 0
    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     * 检查并更新获取失败的节点状态。如果线程应该阻塞，则返回true。这是所有获取循环中的主要信号控制。要求pred == node.prev。
     *
     * @param pred node's predecessor holding status 节点的前驱保持状态
     * @param node the node 节点
     * @return {@code true} if thread should block 如果线程应该阻塞，则返回 true
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        // 如果前一个节点状态是 SIGNAL，直接返回，不需要再自旋了
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             * 该节点已设置状态，要求释放以发出信号，所以它可以安全地挂起。
             */
            return true;
        // 如果前一个节点状态是 CANCELLED
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             * 前驱被取消。跳过前驱节点并指示重试。
             */
            // 循环往前找，直到找到一个状态不是 CANCELLED 的节点，然后和它建立双向链表关系。同时会删除状态为 CANCELLED 的节点
            // 这么做是为了保证当前 node 要挂在有效节点（不是 CANCELLED）后面。也说明 CANCELLED 状态的节点不能作为 node 的前置节点
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        // 否则直接把pred节点的状态置为 SIGNAL
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             * waitStatus 必须是 0 或 PROPAGATE。我们需要一个信号指示它，但是还不能park。调用者将需要重试以确保它在挂起之前不能获取。
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     * 方便的方法来中断当前线程。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     * 方便的方法来挂起，然后并检查是否被打断
     *
     * @return {@code true} if interrupted 如果被打断返回 true
     */
    private final boolean parkAndCheckInterrupt() {
        // 线程会阻塞在这里
        LockSupport.park(this);
        // 线程被唤醒后会返回 线程是否被打断
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     * 各种获取方式，在独占/共享和控制模式下有所不同。每个都基本相同，但令人讨厌的不同。由于异常机制（包括确保在 tryAcquire 方法抛出异常时我们取消）和其他控制的相互作用，因此可能只有少量分解，至少在不损耗性能的前提下。
     */

    // 主要做两件事情：
    // 1：通过不断的自旋尝试，使自己前一个节点的状态变成 signal（线程需要被唤醒），然后阻塞自己。
    // 2：如果前一个节点获得锁，并执行完成之后（即获得锁的线程执行完成之后），再释放锁时，会把阻塞的 node 唤醒,node 唤醒之后再次自旋（无限 for 循环）尝试获得锁
    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     * 以排他的不中断模式获取已在队列中的线程。用于条件等待方法以及获取。
     *
     * @param node the node 节点
     * @param arg the acquire argument 获取参数
     * @return {@code true} if interrupted while waiting 如果在等待时被打断，则返回 true
     */
    final boolean acquireQueued(final Node node, int arg) {
        // 是否获取锁失败
        boolean failed = true;
        try {
            // 是否打断了当前线程
            boolean interrupted = false;
            // 自旋
            for (;;) {
                // 取该节点的前驱节点
                final Node p = node.predecessor();
                /**
                 * 有两种情况会走到 p == head（该节点的前驱节点 == AQS队列头节点）：
                 * 1.第一次调用enq()初始化同步队列时，会给头节点设置一个哨兵节点，然后第一次执行当前方法时就会达成p == head的条件，也就是说其实自己就是当前同步队列中除了哨兵节点的第一个节点，于是再尝试获取一次锁（tryAcquire）；如果成功，就把自己设置成 head，把前一个节点移除；如果失败，就去阻塞当前线程
                 * 2.node 之前一直在阻塞沉睡，然后被唤醒，此时唤醒 node 的节点正是其前置节点，也能走到 if，具体见 release 方法
                 */
                if (p == head && tryAcquire(arg)) {
                    // 获得锁后，把当前节点设置为同步队列的 head 节点，即当前节点成为了新的哨兵节点（为什么说它也是一个哨兵节点？因为setHead方法里只是把head的指针指向了当前节点，里面的属性都设置为null了）
                    setHead(node);
                    // p被回收
                    p.next = null; // help GC 帮助 GC
                    failed = false;
                    // 退出自旋 for 循环，返回当前获取到锁的线程是否被打断
                    return interrupted;
                }

                // shouldParkAfterFailedAcquire()：保证node的pred节点状态不是 CANCELLED，并把node的pred节点状态置为SIGNAL。这么做是因为只有自己的pred节点状态是SIGNAL，自己才可以阻塞(park)了
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // parkAndCheckInterrupt()：阻塞当前线程（LockSupport.park）。如果线程醒来后，它仍然在这个自旋 for 循环里，如果没被打断，就能再次自旋去尝试获得锁
                        parkAndCheckInterrupt())
                    // 如果到了这里，说明被阻塞的线程醒来后发现自己被打断了，但是仍然会再去自旋获取锁，只有获取到锁了才能退出
                    interrupted = true;
            }
        } finally {
            // 如果获取锁失败，将node从队列中移除
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive interruptible mode.
     * 在排他可中断模式下获取。
     * @param arg the acquire argument 获取参数
     */
    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC 帮助 GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    //超时等待
    //如果设置的等待时间大于1000L纳秒，不会一直自旋，会阻塞住
    //一直自旋会特别耗cpu性能
    /**
     * Acquires in exclusive timed mode.
     * 在排他定时模式下获取。
     *
     * @param arg the acquire argument 获取参数
     * @param nanosTimeout max wait time 最大等待时间
     * @return {@code true} if acquired 如果获取到，则返回 true
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        //计算死亡截止时间
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC 帮助 GC
                    failed = false;
                    return true;
                }
                //超时时间有没有过
                nanosTimeout = deadline - System.nanoTime();
                //超时时间过了，直接返回
                if (nanosTimeout <= 0L)
                    return false;
                //如果超时时间大于默认的1000L纳秒，会阻塞住，不会再自旋了
                //自旋太耗性能了
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                //线程中断，会有异常
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // 除了注释部分，其余都和 acquireQueued 逻辑是一致的
    /**
     * Acquires in shared uninterruptible mode.
     * 在共享不中断模式下获取。
     * @param arg the acquire argument 获取参数
     */
    private void doAcquireShared(int arg) {
        // 把当前线程以【共享】模式追加到同步队列的队尾
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    // tryAcquireShared()返回值 > 0 说明获取锁成功
                    if (r >= 0) {
                        // 只有此处和排它锁 acquireQueued() 逻辑不同，排它锁使用的是 setHead() 方法，这里是 setHeadAndPropagate() 方法
                        // setHeadAndPropagate()：不仅把当前节点设置成 head，还会唤醒头节点的后继节点。然后后继节点会再自旋来获取共享锁
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC 帮助 GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * 在共享可中断模式下获取。
     * @param arg the acquire argument 获取参数
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC 帮助 GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    // 这里是和 doAcquireShared() 唯一不同的地方
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     * 在共享定时模式下获取。
     *
     * @param arg the acquire argument 获取参数
     * @param nanosTimeout max wait time 最大等待时间
     * @return {@code true} if acquired 如果获取到，则返回 true
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC 帮助 GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods
    // 主要输出方法

    // 排他模式下，根据状态来判断是否能够获得锁
    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     * 尝试以独占模式获取。此方法应查询对象的状态是否允许以独占模式获取它，如果允许则获取它。
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     * 该方法始终由执行获取的线程调用。如果此方法报告失败，则 acquire 方法可以将线程排队（如果尚未排队），直到被其他线程释放发出信号为止。这可以用来实现方法 Lock＃tryLock()。
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     * 默认实现抛出 UnsupportedOperationException。
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     *        获取参数。该值始终是 1 传递给 acquire 方法，或者是在输入条件等待时保存的值。否则该值将无法解释，并且可以代表您喜欢的任何内容。
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     *         如果成功，则返回true。成功后，便已获取此对象。
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果获取会使该同步器处于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，以使同步正常工作。
     * @throws UnsupportedOperationException if exclusive mode is not supported
     *         如果不支持独占模式，则抛出 UnsupportedOperationException
     */
    protected boolean tryAcquire(int arg) {
        // 直接抛出一个异常，表明需要子类去实现
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     * 尝试设置状态以反映（体现）排他模式下的释放。
     *
     * <p>This method is always invoked by the thread performing release.
     * 该方法始终由执行释放的线程调用
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     * 默认实现抛出 UnsupportedOperationException
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *        释放参数。该值始终是 1 传递给 release 方法，或者是在输入条件等待时的当前状态值。否则该值将无法解释，并且可以代表您喜欢的任何内容。
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     *         如果此对象现在处于完全释放状态，则任何等待线程都可以尝试去获取，则返回 true；否则返回 false。
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果释放会使该同步器处于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，以使同步正常工作。
     * @throws UnsupportedOperationException if exclusive mode is not supported
     *         如果不支持独占模式，则抛出 UnsupportedOperationException
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     * 尝试以共享模式获取。此方法应查询对象的状态是否允许以共享模式获取它，如果允许则获取它。
     *
     * 该方法始终由执行获取的线程调用。这可以用来实现方法 Lock＃tryLock()。
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     * 该方法始终由执行获取的线程调用。如果此方法报告失败，则 acquire 方法可以将线程排队（如果尚未排队），直到被其他线程释放发出信号为止。
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}.
     * 默认实现抛出 UnsupportedOperationException
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     *        获取参数。该值始终是 1 传递给 acquire 方法，或者是在输入条件等待时保存的值。否则该值将无法解释，并且可以代表您喜欢的任何内容。
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     *         失败返回一个负​​值；如果共享模式下获取成功，但没有后续共享模式下的获取可以成功，则返回零；如果共享模式下获取成功，并且后续共享模式下的获取也可能成功，则返回一个正值，在这种情况下，一个后续的等待线程必须检查可用性。（支持三个不同的返回值使该方法可以在仅仅有时进行获取的情况下使用。）成功后，就已经获取了此对象。
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果获取会使该同步器处于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，以使同步正常工作。
     * @throws UnsupportedOperationException if shared mode is not supported
     *         如果不支持共享模式，则抛出 UnsupportedOperationException
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     * 尝试设置状态以反映（体现）共享模式下的释放。
     *
     * <p>This method is always invoked by the thread performing release.
     * 该方法始终由执行释放的线程调用
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     * 默认实现抛出 UnsupportedOperationException
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *        释放参数。该值始终是 1 传递给 release 方法，或者是在输入条件等待时的当前状态值。否则该值将无法解释，并且可以代表您喜欢的任何内容。
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     *         如果此共享模式的释放可能允许等待获取（共享或独占）成功，则返回 true，否则返回 false。
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *         如果释放会使该同步器处于非法状态，则抛出 IllegalMonitorStateException。必须以一致的方式抛出此异常，以使同步正常工作。
     * @throws UnsupportedOperationException if shared mode is not supported
     *         如果不支持共享模式，则抛出 UnsupportedOperationException
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    // 返回表示 true 已经加锁
    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     * 如果同步是被当前（调用）线程独占的，则返回true。每次调用未等待的 ConditionObject 方法时，都会调用此方法。等待方法改为调用 release。
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     * 默认实现抛出 UnsupportedOperationException
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise如果同步是独占的
     *         如果同步是被独占的，则返回 true；否则返回 false
     * @throws UnsupportedOperationException if conditions are not supported
     *         如果不支持条件，则抛出 UnsupportedOperationException
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    // 排他（独占）模式下，尝试获得锁
    // 主要步骤是：
    // 1.尝试执行一次 tryAcquire() 获取锁，如果成功直接返回，失败走第 2 步；（tryAcquire() 交给子类去实现）
    // 2.线程尝试进入同步队列，首先调用 addWaiter 方法，把当前线程以【EXCLUSIVE 排他（独占）】模式追加到同步队列的队尾
    // 3.接着调用 acquireQueued 方法，两个作用，1：阻塞当前节点，2：节点被唤醒时，使其能够继续尝试获得锁（自旋）
    // 4.如果当前获取到锁的线程被打断了，说明当前线程被打断了，那么就打断当前线程
    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     * 独占模式下获取，忽略中断。通过至少调用一次 tryAcquire 方法并成功返回来实现。否则，线程将排队，并可能反复阻塞和解除阻塞，并调用 tryAcquire 方法直到成功。此方法可用于实现 Lock＃lock 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     *        获取参数。此值被传递到 tryAcquire 方法，但不被解释，可以代表您喜欢的任何内容。
     */
    public final void acquire(int arg) {
        // tryAcquire方法是需要实现类去实现的，实现思路一般都是 cas 给 stats 赋值来决定是否能获得锁
        if (!tryAcquire(arg) &&
            // addWaiter 入参 Node.EXCLUSIVE 代表是排他模式
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            // 只有 tryAcquire() 返回false && acquireQueued() 返回true，才能进入到这里；而 acquireQueued() 返回true说明当前获取到锁的线程被打断了，那就中断当前线程
            selfInterrupt();
    }

    //可以中断的获得锁
    //一旦当前线程第一次没有获得锁，自旋时也没有获得，并且进入同步队列阻塞后，一旦被唤醒，就会抛出异常
    //方法的使用场景不大，因为异常是在被唤醒之后才会抛出异常，如果想得不到锁就抛异常，不如使用带有
    //等待时间的锁
    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     * 独占模式下获取，如果中断则中止。通过首先检查中断状态，然后至少调用一次 tryAcquire 方法并成功返回来实现。否则，线程将排队，并可能反复阻塞和解除阻塞，调用 tryAcquire 方法直到成功或线程被中断。此方法可用于实现 Lock#lockInterruptibly 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     *        获取参数。此值被传递到 tryAcquire 方法，但不被解释，可以代表您喜欢的任何内容。
     * @throws InterruptedException if the current thread is interrupted
     *         如果当前线程被中断，则抛出 InterruptedException
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     * 尝试以独占模式获取，如果中断则中止，如果给定的超时时间过去，则失败。通过首先检查中断状态，然后至少调用一次 tryAcquire 方法并成功返回来实现。否则，线程将排队，并可能反复阻塞和解除阻塞，调用 tryAcquire 方法直到成功或线程被中断或超时时间过去。此方法可用于实现 Lock#tryLock(long, TimeUnit) 方法。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     *        获取参数。此值被传递到 tryAcquire 方法，但不被解释，可以代表您喜欢的任何内容。
     * @param nanosTimeout the maximum number of nanoseconds to wait
     *                     最大等待的纳秒数
     * @return {@code true} if acquired; {@code false} if timed out
     *         如果已获取，则返回 true；如果超时，则返回 false
     * @throws InterruptedException if the current thread is interrupted
     *         如果当前线程被中断，则抛出 InterruptedException
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }

    // unlock的基础方法
    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     * 独占模式下释放。如果 tryRelease 方法返回 true，则通过解除阻塞一个或多个线程来实现。此方法可用于实现 Lock#unlock 方法。
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     *        释放参数。此值被传递到 tryRelease 方法，但不被解释，可以代表您喜欢的任何内容。
     * @return the value returned from {@link #tryRelease}
     *         从 tryRelease 方法返回的值
     */
    public final boolean release(int arg) {
        // tryRelease 交给实现类去实现，一般就是用当前同步器状态减去 arg，如果返回 true 说明成功释放锁
        if (tryRelease(arg)) {
            Node h = head;
            // 头节点不为空，并且非初始化状态，就去释放。 todo 这里为什么头节点不一定是SIGNAL？
            if (h != null && h.waitStatus != 0)
                // 唤醒等待锁的节点
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * 共享模式下，尝试获得锁
     * tryAcquireShared 首先尝试获得锁，返回值小于 0 表示没有获得锁
     * 共享锁和排他锁最大的不同在于：对于同一个共享资源
     * 排他锁只能让一个线程获得，共享锁可以让多个线程获得
     * arg 可以被子类当做任意参数，比如当做可获得锁线程的最大个数
     */
    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     * 共享模式下获取，忽略中断。通过首先至少调用一次 tryAcquireShared 方法并成功返回来实现。否则，线程将排队，并可能反复阻塞和解除阻塞，并调用 tryAcquireShared 方法直到成功。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     *        获取参数。此值被传递到 tryAcquireShared 方法，但不被解释，可以代表您喜欢的任何内容。
     */
    public final void acquireShared(int arg) {
        // tryAcquireShared() 是需要实现类去实现的，tryAcquireShared()返回值 > 0 说明获取锁成功
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * 共享模式下获取，如果中断则中止。通过首先检查中断状态，然后至少调用一次 tryAcquireShared 方法并成功返回来实现。否则，线程将排队，并可能反复阻塞和解除阻塞，调用 tryAcquireShared 方法直到成功或线程被中断。
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     *            获取参数。此值被传递到 tryAcquireShared 方法，但不被解释，可以代表您喜欢的任何内容。
     * @throws InterruptedException if the current thread is interrupted
     *         如果当前线程被中断，则抛出 InterruptedException
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     * 尝试以共享模式获取，如果中断则中止，如果给定的超时时间过去，则失败。通过首先检查中断状态，然后至少调用一次 tryAcquireShared 方法并成功返回来实现。否则，线程将排队，并可能反复阻塞和解除阻塞，调用 tryAcquireShared 方法直到成功或线程被中断或超时时间过去。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     *        获取参数。此值被传递到 tryAcquireShared 方法，但不被解释，可以代表您喜欢的任何内容。
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     *         如果已获取，则返回 true；如果超时，则返回 false
     * @throws InterruptedException if the current thread is interrupted
     *         如果当前线程被中断，则抛出 InterruptedException
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    // 共享模式下，释放当前线程的共享锁
    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     * 以共享模式释放。如果 tryReleaseShared 方法返回true，则通过解除阻塞一个或多个线程来实现。
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     *            释放参数。此值被传递到 tryReleaseShared 方法，但不被解释，可以代表您喜欢的任何内容。
     * @return the value returned from {@link #tryReleaseShared}
     *         从 tryReleaseShared 方法返回的值
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            // 这个方法也是线程在获得共享锁时，唤醒后续节点时调用的方法（doAcquireShared-setHeadAndPropagate-doReleaseShared）
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods
    // 队列检查方法

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     * 查询是否有任何线程正在等待获取。请注意，由于中断和超时导致的取消可能随时发生，因此 true 返回值不能保证任何其他线程都可以获取。
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     * 在此实现中，此操作将以常数时间返回。
     *
     * @return {@code true} if there may be other threads waiting to acquire
     *         如果可能还有其他线程在等待获取，则返回 true
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     * 查询是否有任何线程曾竞争该同步器；也就是说，是否获取方法曾被阻塞过。
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     * 在此实现中，此操作将以常数时间返回。
     *
     * @return {@code true} if there has ever been contention
     *         如果曾有过竞争，则返回 true
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     * 返回队列中的第一个（等待时间最长）线程，如果当前没有线程在队列中，则返回 null。
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     * 在此实现中，此操作通常以常数时间返回，但是如果其他线程正在同时修改队列，则可能在竞争时进行迭代。
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     *         返回队列中的第一个（等待时间最长）线程，如果当前没有线程在队列中，则返回 null。
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        // 仅处理快速路径，否则 relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     * 当快速路径失败时调用的 getFirstQueuedThread 版本
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         * 第一个节点通常是 head.next。尝试获取其线程字段，确保读取一致：如果线程字段无效或 s.prev 不再为 head，则其他一些线程在我们的某些读取之间同时执行了 setHead 方法。在遍历之前我们尝试两次。
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         * Head 的下一个字段可能尚未设置，或者在 setHead 之后可能未设置。因此，我们必须检查一下 tail 是否实际上是第一个节点。如果不是，我们继续，安全地从 tail 返回到 head 遍历以找到第一个，确保终止。
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * Returns true if the given thread is currently queued.
     * 如果给定线程目前正在排队，则返回true。
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     * 此实现遍历队列以确定给定线程的存在。
     *
     * @param thread the thread 线程
     * @return {@code true} if the given thread is on the queue 如果给定线程在队列中，则返回 true
     * @throws NullPointerException if the thread is null 如果线程为 null，则抛出 NullPointerException
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     * 如果明显的第一个排队线程（如果存在）正在独占模式下等待，则返回 true。如果此方法返回 true，并且当前线程正尝试以共享模式进行获取（也就是说，此方法是从 tryAcquireShared 方法调用过来的），则可以确保当前线程不是第一个排队线程。仅在 ReentrantReadWriteLock 中用作一个启发式方法。
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        // 代码思路：队列里至少存在一个独占模式的节点，且节点线程不为空
        // 头节点不为空 && 头节点的下一个节点不为空 && 头节点的下一个节点是SHARED && 头节点的下一个节点的线程不为空
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    // 该方法是公平锁加锁时tryAcquire()方法里实现公平性的关键
    // 会判断当前线程是不是属于同步队列的头节点的下一个节点(头节点是释放锁的节点)，如果是(返回false)，符合先进先出的原则，可以获得锁；如果不是(返回true)，则继续等待
    /**
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     * 查询是否有任何线程在等待获取比当前线程更长的时间。
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     * 调用此方法等效于（但可能比以下方法更有效）：
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     * 注意，由于中断和超时导致的取消可能随时发生，因此 true 返回值不能保证某些其他线程将在当前线程之前获取。同样，由于队列为空，另一个线程有可能赢得竞争，此方法返回 false 后入队。
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     * 此方法设计为由一个公平同步器使用，以避免乱入。此类同步器的 tryAcquire 方法应返回 false，并且如果此方法返回 true，则其 tryAcquireShared 方法应返回一个负值（除非这是一个可重入获取）。例如，用于公平，可重入，独占模式同步器 tryAcquire 方法可能如下所示：
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     // 一个重入获取；增量保持 count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *     // 尝试正常获取
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     *         如果有一个排队的线程在当前线程之前，则返回 true，如果当前线程位于队列的开头或队列为空，则返回 false
     * @since 1.7
     */
    // has queued predecessors：是否有排队的前驱节点
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        // 此方法的正确性取决于 head 在 tail 之前初始化，并且 head.next 是准确的（如果当前线程是队列中的第一个）。
        Node t = tail; // Read fields in reverse initialization order 以相反的初始化顺序读取字段
        Node h = head;
        Node s;
        // h == t 说明当前队列为空，直接返回 false
        // 如果 h != t && s == null 说明有一个元素将要作为AQS的第一个节点入队列（具体查看enq()方法会先创建一个哨兵头节点），返回 true
        // 如果 h != t && s != null && s.thread != Thread.currentThread() 说明队列里面的第一个元素不是当前线程，返回 true
        // 当前队列不为空 && （队列里只有一个哨兵头结点 || 哨兵头结点的下一个节点线程不是当前线程）
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods
    // 检测和监控方法

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     * 返回等待获取线程数的估计值。该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。设计此方法是用于监控系统状态，而不用于同步控制。
     *
     * @return the estimated number of threads waiting to acquire
     *         等待获取的估计线程数
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     * 返回一个包含可能正在等待获取线程的集合。因为实际的线程集在构造此结果时可能会动态变化，所以返回的集合只是一个尽最大努力的估计。返回的集合元素没有特定的顺序。设计此方法是为了促进子类的构造，可以提供更广泛的监测设施。
     *
     * @return the collection of threads
     *         线程集合
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     * 返回一个包含可能正在等待以独占模式获取的线程的集合。该方法与 getQueuedThreads 方法有相同的属性，除了它只返回由于独占获取而等待的那些线程
     *
     * @return the collection of threads
     *         线程集合
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     * 返回一个包含可能正在等待以共享模式获取的线程的集合。该方法与 getQueuedThreads 方法有相同的属性，除了它只返回由于共享获取而等待的那些线程
     *
     * @return the collection of threads
     *         线程集合
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     * 返回一个标识此同步器及其状态的字符串。括号中，state 包含在字符串 "State =" 里，后跟 getState 方法的当前值，以及 “nonempty” 或 “empty” 取决于队列是否为空。
     *
     * @return a string identifying this synchronizer, as well as its state
     *         返回一个标识此同步器及其状态的字符串
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions
    // Conditions 的内部支持方法

    // 判断node节点是否在同步队列中
    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * 如果一个节点（总是一个最初放置在条件队列中的节点）现在正在同步队列中等待去重新获取，则返回 true。
     * @param node the node 节点
     * @return true if is reacquiring 如果正在重新获取，则返回 true
     */
    final boolean isOnSyncQueue(Node node) {
        // 如果node的状态是CONDITION || node的前驱节点为空（说明node不在同步队列中）
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue 如果有后继节点，则必须在队列中
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         * node.prev 可以为非空，但还没有在队列中，因为 CAS 将它放入队列可能失败。因此，我们必须从 tail 开始遍历以确保它确实做到了。在调用此方法时，它将始终处于 tail 附近，除非 CAS 失败（这不太可能），否则它将一直存在，因此我们几乎不会遍历太多。
         */
        // 从同步队列尾节点开始寻找node，找到返回true，否则返回false
        return findNodeFromTail(node);
    }

    // 从同步队列尾节点开始寻找node，找到返回true，否则返回false
    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * 如果节点在同步队列中（从 tail 向后搜索），则返回true。仅在 isOnSyncQueue 需要时调用。
     * @return true if present 如果存在，则返回 true
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    // 把条件队列节点转移到同步队列中去
    //大概思路：
    //1.把 node 状态从 CONDITION 改为 0，失败直接返回false，成功走到 2
    //2.node 追加到同步队列的队尾
    //3.将 node 的前一个节点状态置为 SIGNAL，成功直接返回，失败直接唤醒
    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * 将节点从条件队列转移到同步队列。如果成功，则返回 true。
     * @param node the node 节点
     * @return true if successfully transferred (else the node was
     * cancelled before signal)
     *         如果成功转移（否则该节点在信号之前被取消），则返回true
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 如果无法更改 waitStatus，则该节点已被取消。
         */
        // 将 node 的状态从 CONDITION 修改成初始化，失败返回 false
        // 只把状态为CONDITION的条件队列节点从条件队列转移到同步队列中去。代码的思路是：第一步要把node状态改为0，而如果状态从CONDITION改为0失败，则说明状态不是CONDITION，那就只能是CANCELLED，而状态是CANCELLED的条件队列节点我们不需要把它从条件队列转移到同步队列中去，所以直接返回false，取下一个条件队列节点来处理。其实一行代码做了两件事，1.判断节点状态，2.如果节点状态是CONDITION则直接改成0。简直666
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /* 到这里，node的状态是0 */

        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         * 拼接到队列上并尝试设置前驱节点的 waitStatus 来表示线程（可能）正在等待。如果取消或尝试设置 waitStatus 失败，请唤醒以重新同步（在这种情况下，waitStatus 可能会短暂而无害地出现错误）。
         */
        // 当前条件队列节点加入到同步队列，返回的 p 是 node 在同步队列中的前一个节点。看命名是 p，实际是 pre 前一个单词的缩写
        Node p = enq(node);
        int ws = p.waitStatus;
        // 状态修改成 SIGNAL，如果成功直接返回
        // 把当前节点的前一个节点修改成 SIGNAL 的原因，是因为 SIGNAL 本身就表示当前节点后面的节点都是需要被唤醒的
        // 如果 p 节点状态是 CANCELLED || 状态不是 CANCELLED 但不能修改成SIGNAL
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            // 直接唤醒
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     * 取消等待后，如有必要，转移节点到同步队列。如果线程在发出信号之前被取消，则返回true。
     *
     * @param node the node 节点
     * @return true if cancelled before the node was signalled 如果在该节点被发信号之前取消，则返回true
     */
    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         * 如果我们输给了 signal()，那么直到它完成 enq() 之前我们都无法继续进行。在不完全的传输中取消是罕见且短暂的，因此只需自旋即可。
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * 用当前 state 的值调用 release 方法；返回保存状态。取消节点并在失败时抛异常
     * @param node the condition node for this wait 此等待的条件节点
     * @return previous sync state 先前的同步状态
     */
    //Invokes release with current state value; returns saved state. Cancels node and throws exception on failure.
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            // 释放锁
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions
    // 条件的检测方法

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     * 查询给定的 ConditionObject 是否使用此同步器作为其锁定。
     *
     * @param condition the condition 条件
     * @return {@code true} if owned 如果拥有，则返回 true
     * @throws NullPointerException if the condition is null 如果 condition 为空，则抛出 NullPointerException
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     * 查询是否有任何线程正在等待与此同步器关联的给定条件。请注意，由于超时和中断随时可能发生，因此返回 true 并不能保证将来的 signal 会唤醒任何线程。此方法主要设计用于系统状态的监控。
     *
     * @param condition the condition 条件
     * @return {@code true} if there are any waiting threads 如果有任何等待线程，则返回 true
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     *         如果独占的同步没有被保持，则抛出 IllegalMonitorStateException
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     *         如果给定条件与此同步器不相关，则抛出 IllegalArgumentException
     * @throws NullPointerException if the condition is null
     *         如果 condition 为空，则抛出 NullPointerException
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     * 返回正在等待与此同步器关联的给定条件的线程数的估计值。请注意，由于超时和中断随时可能发生，因此估算值仅作为 waiters 实际数量的上限。此方法设计用于系统状态的监控，而不用于同步控制。
     *
     * @param condition the condition 条件
     * @return the estimated number of waiting threads 估计的等待线程数
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     *         如果独占的同步没有被保持，则抛出 IllegalMonitorStateException
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     *         如果给定条件与此同步器不相关，则抛出 IllegalArgumentException
     * @throws NullPointerException if the condition is null
     *         如果 condition 为空，则抛出 NullPointerException
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     * 返回一个包含那些可能正在等待与此同步器关联的给定条件的线程集合。因为实际的线程集在构造此结果时可能会动态变化，所以返回的集合只是一个尽最大努力的估计。返回的集合元素没有特定的顺序。
     *
     * @param condition the condition 条件
     * @return the collection of threads 线程集合
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     *         如果独占的同步没有被保持，则抛出 IllegalMonitorStateException
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     *         如果给定条件与此同步器不相关，则抛出 IllegalArgumentException
     * @throws NullPointerException if the condition is null
     *         如果 condition 为空，则抛出 NullPointerException
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    // 条件队列，从基础属性上可以看出是链表队列结构
    // 单向链表
    // 用来存放调用条件变量的await方法后被阻塞的线程
    /**
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     * 作为 Lock 实现的基础为 AbstractQueuedSynchronizer 的 Condition 实现服务。
     *
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     * 该类的方法文档描述了机制，而不是从锁和 Condition 用户的角度来描述行为规范。这个类的导出版本通常需要伴随着描述依赖于关联的 AbstractQueuedSynchronizer 的条件语义的文档。
     *
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     * 这个类是可序列化的，但是所有字段都是 transient，因此反序列化的条件没有 waiters。
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /**
         * First node of condition queue.
         * 条件队列中第一个 node
         */
        private transient Node firstWaiter;
        /**
         * Last node of condition queue.
         * 条件队列中最后一个 node
         */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         * 创建一个新的 ConditionObject 实例。
         */
        public ConditionObject() { }

        // Internal methods
        // 内部方法

        // 增加新的 waiter 到队列中，返回新添加的 waiter
        // 如果尾节点状态不是 CONDITION 状态，删除条件队列中所有状态不是 CONDITION 的节点
        // 如果队列为空，新增节点作为队列头节点，否则追加到尾节点上
        /**
         * Adds a new waiter to wait queue.
         * 添加新的 waiter 到等待队列。
         *
         * @return its new wait node 它的新的等待节点
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            // 如果lastWaiter被取消，清除它。
            // 代码意思是如果不是 CONDITION 状态，代表那就是 CANCELLED 状态。这也说明了条件队列的节点状态只有CONDITION和CANCELLED
            // 如果 尾节点不为空 && 尾节点状态是CANCELLED
            if (t != null && t.waitStatus != Node.CONDITION) {
                // 删除条件队列中所有状态不是CONDITION的节点
                unlinkCancelledWaiters();
                // 再把t指向条件队列的尾节点，保证t指向的条件队列节点一定是存在且状态为CONDITION的节点
                t = lastWaiter;
            }
            // 创建一个新的类型为 Node.CONDITION 的 node 节点
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            // 如果队列是空的，直接放到队列头
            if (t == null)
                firstWaiter = node;
                // 如果队列不为空，直接放到队列尾
            else
                // 如上所说，在条件队列中，nextWaiter 就是表示其下一个节点元素
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        // 把条件队列头节点转移到同步队列去
        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         * 删除和传输节点，直到遇到未取消节点或 null。从信号中分离出来，部分鼓励编译器内联没有 waiters 的情况
         * @param first (non-null) the first node on condition queue （非空）条件队列上的第一个节点
         */
        private void doSignal(Node first) {
            do {
                // nextWaiter为空，说明到队尾了。代码思路为：如果条件队列循环完，则此时firstWaiter指向null，那么把lastWaiter也指向null
                if ((firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                // 从条件队列头部开始唤醒，所以直接把头结点.next 置为 null。其实就是把 node 从条件队列中移除了
                // 这里有个重要的点是，每次唤醒都是从队列头部开始唤醒，所以把 next 置为 null 没有关系，如果唤醒是从任意节点开始唤醒的话，就会有问题，容易造成链表的割裂
                first.nextWaiter = null;
                // transferForSignal 方法会把条件节点转移到同步队列中去。transferForSignal 只有一种情况会返回false，那就是处理的节点状态是CANCELLED，否则会返回true，循环结束
                // 条件队列的 node 不用管它的状态，因为在 await 的时候，会通过 unlinkCancelledWaiters 方法自动清除状态不是 CONDITION 的节点
                // (first = firstWaiter) != null 为 true 的，表示还可以继续循环，为 false 说明队列中的元素已经循环完了
            } while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }

        // 把等待队列所有节点依次转移到同步队列去
        // 本质就是 for 循环调用 transferForSignal 方法，将条件队列中的节点循环转移到同步队列中去
        /**
         * Removes and transfers all nodes.
         * 删除和传输所有的节点
         * @param first (non-null) the first node on condition queue （非空）条件队列上的第一个节点
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                // 拿出条件队列队列头节点的下一个节点
                Node next = first.nextWaiter;
                // 把头节点从条件队列中删除
                first.nextWaiter = null;
                // 头节点转移到同步队列中去，循环调用transferForSignal()
                transferForSignal(first);
                // 开始循环头节点的下一个节点
                first = next;
            } while (first != null);
        }

        // 会检查尾部的 waiter 是不是已经不是CONDITION状态，如果不是，删除这些 waiter
        // unlink：分开；分离；拆开
        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         * 从条件队列中解除已取消的 waiter 节点的连接。仅在持有锁时调用。当在条件等待期间发生取消时，以及当看到 lastWaiter 已被取消时插入一个新 waiter 时，都会调用此函数。在没有信号的情况下，需要使用这个方法来避免垃圾保留。因此，即使它可能需要一个完整的遍历，它也只有在没有信号的情况下出现超时或取消时才发挥作用。它会遍历所有节点，而不是在某个特定目标上停止，以断开所有指向垃圾节点的指针的连接，而不需要在取消风暴期间进行多次重新遍历。
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            // trail 表示条件队列中上一个状态为CONDITION的node，这个字段作用非常大，可以把状态都是 CONDITION 的 node 串联起来，即使 node 之间有其他节点都可以
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                // 当前node的状态不是CONDITION，删除自己
                if (t.waitStatus != Node.CONDITION) {
                    // 删除当前node
                    t.nextWaiter = null;
                    // 如果 trail 是空的，咱们循环又是从头开始的，说明从头到当前节点的状态都不是 CONDITION
                    // 都已经被删除了，所以移动队列头结点到当前节点的下一个节点
                    if (trail == null)
                        firstWaiter = next;
                        // 如果找到上次状态是CONDITION的节点的话，先把当前节点删掉，然后把自己挂到上一个状态是 CONDITION 的节点上
                    else
                        trail.nextWaiter = next;
                    // 遍历结束，最后一次找到的CONDITION节点就是尾节点
                    if (next == null)
                        lastWaiter = trail;
                }
                // 状态是 CONDITION 的 Node
                else
                    trail = t;
                // 继续循环，循环顺序从头到尾
                t = next;
            }
        }

        // public methods
        // 公共方法

        // 唤醒阻塞在条件队列中的节点
        // 主要步骤都在 transferForSignal 方法里
        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         * 将等待时间最长的线程(如果存在的话)从该条件的等待队列移动到拥有锁的等待队列。
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false} 如果 isHeldExclusively 方法返回 false 抛出 IllegalMonitorStateException
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 拿到条件队列头节点
            Node first = firstWaiter;
            if (first != null)
                // 把条件队列中的节点转移到同步队列中去，调用一次transferForSignal()
                doSignal(first);
        }

        // 唤醒条件队列中的全部节点
        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         * 将所有线程从该条件的等待队列移动到拥有锁的等待队列。
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false} 如果 isHeldExclusively 方法返回 false 抛出 IllegalMonitorStateException
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 拿到条件队列头节点
            Node first = firstWaiter;
            if (first != null)
                // 从条件队列头节点开始唤醒条件队列中所有的节点，循环调用transferForSignal()
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * 实现不中断的条件等待。
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         *      保存 getState 方法返回的 lock 状态。
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         *      以保存的状态作为参数调用 release 方法，如果失败则抛出 IllegalMonitorStateException。
         * <li> Block until signalled.
         *      阻塞直到发信号为止。
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         *      通过以保存的状态作为参数调用 acquire 方法的专门版本来重新获取。
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         * 对于可中断的等待我们需要跟踪，如果在有条件的情况下被阻塞而被中断，是否抛出 InterruptedException；如果在等待重新获取的情况下被阻塞而被中断，则与当前线程重新中断。
         */

        /**
         * Mode meaning to reinterrupt on exit from wait
         * 模式意味着在等待退出时重新中断
         */
        private static final int REINTERRUPT =  1;
        /**
         * Mode meaning to throw InterruptedException on exit from wait
         * 模式意味着在等待退出时抛出 InterruptedException
         */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         * 检查是否有中断，如果在发出信号之前被中断，则返回 THROW_IE；如果在发出信号之后，则返回 REINTERRUPT；如果没有中断，返回0。
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         * 抛出 InterruptedException，重新中断当前线程，或者什么也不做，具体取决于模式。
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * Implements interruptible condition wait.
         * 实现可中断条件等待。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         *      如果当前线程被中断，则抛出 InterruptedException。
         * <li> Save lock state returned by {@link #getState}.
         *      保存 getState 方法返回的 lock 状态。
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         *      以保存的状态作为参数调用 release 方法，如果失败则抛出 IllegalMonitorStateException。
         * <li> Block until signalled or interrupted.
         *      阻塞直到发信号或被打断为止。
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         *      通过以保存的状态作为参数调用 acquire 方法的专门版本来重新获取。
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         *      如果在第 4 步中被阻塞而被中断，则抛出 InterruptedException。
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            /* 第一部分：节点插入到条件队列队尾；释放锁；判断如果节点不在同步队列上就阻塞 */
            // 内部会创建一个新的类型为Node.CONDITION的node节点，然后将该节点插入到条件队列的队尾
            Node node = addConditionWaiter();
            // 加入条件队列后，需要释放当前线程获取的锁（底层调用了release方法）。因为自己马上就要阻塞了，必须马上释放之前lock的资源，不然自己不被唤醒的话，别的线程就永远得不到该共享资源
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            // isOnSyncQueue(node)：判断node节点是否在同步队列中
            // 确认node不在同步队列上再阻塞，如果 node 在同步队列上，是不能够在条件队列上阻塞挂起当前线程的
            // 目前想到的只有两种可能：
            // 1：node 刚被加入到条件队列中，立马就被其他线程 signal 转移到同步队列中去了
            // 2：线程之前在条件队列中沉睡，被唤醒后加入到同步队列中去
            while (!isOnSyncQueue(node)) {
                // this = AbstractQueuedSynchronizer$ConditionObject，这也说明了可以有多个条件队列
                // 在条件队列上阻塞挂起当前线程
                LockSupport.park(this);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            /* 第二部分：到这里，说明节点已经在同步队列上 */
            // 其他线程通过 signal 的方法已经把 node 从条件队列中转移到同步队列中去了；所以这里可以直接跳过 acquire() 去调用 acquireQueued() 去获取锁
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled 如果取消则清理
                // 删除条件队列中所有状态不是CONDITION的节点
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * 实现定时条件等待。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         *      如果当前线程被中断，则抛出 InterruptedException。
         * <li> Save lock state returned by {@link #getState}.
         *      保存 getState 方法返回的 lock 状态。
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         *      以保存的状态作为参数调用 release 方法，如果失败则抛出 IllegalMonitorStateException。
         * <li> Block until signalled, interrupted, or timed out.
         *      阻塞直到发信号，被打断或超时为止。
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         *      通过以保存的状态作为参数调用 acquire 方法的专门版本来重新获取。
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         *      如果在第 4 步中被阻塞而被中断，则抛出 InterruptedException。
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * 实现绝对定时条件等待。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         *      如果当前线程被中断，则抛出 InterruptedException。
         * <li> Save lock state returned by {@link #getState}.
         *      保存 getState 方法返回的 lock 状态。
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         *      以保存的状态作为参数调用 release 方法，如果失败则抛出 IllegalMonitorStateException。
         * <li> Block until signalled, interrupted, or timed out.
         *      阻塞直到发信号，被打断或超时为止。
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         *      通过以保存的状态作为参数调用 acquire 方法的专门版本来重新获取。
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         *      如果在第 4 步中被阻塞而被中断，则抛出 InterruptedException。
         * <li> If timed out while blocked in step 4, return false, else true.
         *      如果在第 4 步中被阻塞而超时，返回 false，否则返回 true
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * 实现定时条件等待。
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         *      如果当前线程被中断，则抛出 InterruptedException。
         * <li> Save lock state returned by {@link #getState}.
         *      保存 getState 方法返回的 lock 状态。
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         *      以保存的状态作为参数调用 release 方法，如果失败则抛出 IllegalMonitorStateException。
         * <li> Block until signalled, interrupted, or timed out.
         *      阻塞直到发信号，被打断或超时为止。
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         *      通过以保存的状态作为参数调用 acquire 方法的专门版本来重新获取。
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         *      如果在第 4 步中被阻塞而被中断，则抛出 InterruptedException。
         * <li> If timed out while blocked in step 4, return false, else true.
         *      如果在第 4 步中被阻塞而超时，返回 false，否则返回 true
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation
        // 支持检测（仪表）

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         * 如果此条件是由给定的同步对象创建，则返回true。
         *
         * @return {@code true} if owned 如果拥有返回 true
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         * 查询是否有任何线程在这种情况下等待。实现 AbstractQueuedSynchronizer#hasWaiters(ConditionObject)。
         *
         * @return {@code true} if there are any waiting threads 如果有等待线程，则返回 true
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         *         如果 isHeldExclusively 方法返回 false，则抛出 IllegalMonitorStateException
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         * 返回在此条件下等待的线程数的估计值。
         * 实现 AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)
         *
         * @return the estimated number of waiting threads 等待线程数的估计值
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         *         如果 isHeldExclusively 方法返回 false，则抛出 IllegalMonitorStateException
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         * 返回一个包含可能正在等待此条件的线程的集合。
         * 实现 AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)
         *
         * @return the collection of threads 线程集合
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         *         如果 isHeldExclusively 方法返回 false，则抛出 IllegalMonitorStateException
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     * 设置为支持compareAndSet。我们需要在此处本地实现：为了允许将来进行增强，我们不能显式地继承AtomicInteger的子类，否则该子类将是高效且有用的。因此，我们使用 hotspot 内在的 API 在本地实现，两害相权取其轻。而当我们这样做时，我们对其他的 CASable 字段执行相同的操作(否则可以使用原子字段更新程序来完成)。
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     * CAS 头字段。仅由 enq 方法使用。
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     * CAS 尾字段。仅由 enq 方法使用。
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     * CAS 一个节点的 waitStatus 字段。
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     * CAS 一个节点的 next 字段。
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
