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
 * 提供了一种框架，通过先进先出的同步队列来实现阻塞锁。
 * 对于大多数同步器来说，设计的关键在于依赖安全的atomic value来表示状态，
 * 子类需要实现方法来改变状态，并且定义那些状态代表acquired 或者 released
 * Provides a framework for implementing(实施) blocking locks(阻塞锁) and related
 * synchronizers(同步器) (semaphores, events, etc) that rely on(依赖)
 * first-in-first-out (FIFO) wait queues.  This class is designed(设计) to
 * be a useful basis(有用的基础) for most kinds of(大多数) synchronizers that rely on a
 * single atomic value to represent(代表) state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these(考虑到这些), the other methods in this class carry
 * out all(执行所有) queuing and blocking mechanics. Subclasses can maintain(维持)
 * other state fields, but only the atomically updated
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 * 翻译：提供了一种框架依赖先进先出等待队列来实现阻塞锁和相关的同步器。该类被设计用来为大多数依赖一个单个的原子值来代表状态的同步器作为一个有用的基础。子类（待续。。。以后完善）
 *
 *  可以去实现非公开的内部类，去实现同步性能
 *   Subclasses should be defined as non-public internal helper
 * classes(非公开的内部类) that are used to implement the synchronization properties(同步性能)
 * of their enclosing class.  Class
 *  AbstractQueuedSynchronizer does not implement any
 * synchronization interface.  Instead(相反) it defines methods such as
 * acquireInterruptibly that can be invoked(被调用) as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 *
 * 提供了排他模式和共享模式,两者都是先进先出队列
 * 排他模式下：其他试图acquires的线程不会成功，共享模式可以
 * 子类ReadWriteLock实现了两种模式
 *   This class supports either or both a default exclusive(排他)
 * mode and a shared mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not understand these differences except(除了) in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 *
 * 内部类ConditionObject可以被用作Condition
 *
 * This class defines a nested ConditionObject class that
 * can be used as a  Condition implementation by subclasses
 * supporting(支持) exclusive mode for which method
 * isHeldExclusively reports whether synchronization is exclusively
 * held with respect to the current thread, method release
 * invoked with the current getState value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually(最后) restores(恢复) this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of ConditionObject depends of course on the
 * semantics of its synchronizer implementation.
 *
 * <p>This class provides inspection(视察), instrumentation(仪表盘), and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an  AbstractQueuedSynchronizer for their
 * synchronization mechanics.
 *
 * <p>Serialization of this class stores only the underlying(根本的) atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 *
 * <h3>Usage</h3>
 *
 * To use this class as the basis of a synchronizer, redefine(重新定义) the
 * following methods, as applicable, by inspecting(检查) and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}//是否被锁住
 * </ul>
 * tryAcquire、tryRelease、tryAcquireShared、tryReleaseShared、isHeldExclusively 这些方法默认都会抛 UnsupportedOperationException
 * 并且是线程安全的，一般来说会很快，并且不会阻塞
 * Each of these methods by default throws
 * UnsupportedOperationException.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 *
 * 继承 AbstractOwnableSynchronizer 是为了方便跟踪独占 synchronizer 的线程
 * 这些可以帮忙监控和诊断工具识别哪些线程是持有锁的。
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 *
 * 虽然是基于先进先出队列，但并不会自动的按照先进先出执行
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically(自动的) enforce(实施) FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        <em>possibly block current thread</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 * </pre>
 *
 * (Shared mode is similar(相似) but may involve(设计) cascading(层叠,串接) signals(信号).)
 *
 * 入队之前需要先检查能否acquire
 *
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly(最近，新的) acquiring thread may barge(驳船,闯入) ahead of(在……之前)
 * others that are blocked and queued.  However, you can, if desired(渴望的，要求的),
 * define  tryAcquire and/or tryAcquireShared to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby(从而) providing a <em>fair</em> FIFO acquisition order.
 * In particular(特别), most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 *
 *
 * <p>Throughput and scalability are generally highest for the default barging (also known as <em>greedy</em>,
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
 *
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>Here is a non-reentrant mutual exclusion lock(非可重入的互斥锁) class that uses
 * the value zero to represent the unlocked state(0代表没有锁), and one to
 * represent the locked state(1代表锁住). While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
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
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    //初始化时，同步状态是0
    protected AbstractQueuedSynchronizer() { }

    /**
     * 同步队列：获取不到锁时，让线程等待的等待队列
     * 同步队列节点定义，线程进入队列时，会被 Node 所包装
     * 同步队列本身就是一个队列，想想队列章节很多队列的实现
     * 入队时会加上 CLH lock(自旋实现)，被加到队尾
     * 出队时，取队头
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     */
    static final class Node {

        /**
         * 同步队列单独的属性
         */
        // node 是共享模式
        // 标记该线程是获取【共享】资源时被阻塞挂起后放入AQS队列的
        static final Node SHARED = new Node();

        // node 是排它模式
        // 标记该线程是获取【独占】资源时被阻塞挂起后放入AQS队列的
        static final Node EXCLUSIVE = null;

        // 当前节点的前驱节点
        // 节点被 acquire 成功后就会变成head
        // head 节点不能被 cancelled
        volatile Node prev;

        // 当前节点的后继节点
        volatile Node next;

        /**
         * 同步队列和条件队列共享的属性
         */
        // 记录当前节点（线程）的等待状态，通过节点的状态来控制节点的行为
        // 普通同步节点，就是 0 ，条件节点是 CONDITION = -2
        volatile int waitStatus;

        // waitStatus 的状态有以下几种
        // 线程被取消
        static final int CANCELLED =  1;

        // 线程需要被唤醒。该状态的意义：同步队列中的节点在自旋获取锁的时候，如果前一个节点的状态是 SIGNAL，那么自己就可以阻塞休息了，否则自己会一直自旋尝试获得锁
        static final int SIGNAL    = -1;

        // 表示当前 node（线程）正在条件队列里面等待，当有节点从同步队列转移到条件队列时，状态就会被赋值（更改）成 CONDITION
        static final int CONDITION = -2;

        // 释放共享资源时需要通知其他节点。
        // 无条件传播，共享模式下，该状态的进程处于可运行状态
        static final int PROPAGATE = -3;

        // 当前节点的线程
        volatile Thread thread;

        // 在同步队列中，nextWaiter 并不表示其下一个节点元素，用 next 表示其下一个节点元素，nextWaiter 只是表示当前 Node 是排他模式还是共享模式
        // 在条件队列中，nextWaiter 就是表示其下一个节点元素
        Node nextWaiter;

        // 是否是共享模式
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         * 翻译：返回前一个节点，如果前一个节点为null抛出NullPointerException。当前驱节点不为空时使用。可以省略null检查，但它是用来帮助VM的。
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

        Node(Thread thread, Node mode) {     // Used by addWaiter 被addWaiter方法调用
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition 被 条件队列 调用
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }


    /**
     * 条件队列，从基础属性上可以看出是链表队列结构
     * 单向链表
     * 用来存放调用条件变量的await方法后被阻塞的线程
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /**
         * 条件队列中第一个 node
         */
        private transient Node firstWaiter;
        /**
         * 条件队列中最后一个 node
         */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        /**
         * 增加新的 waiter 到队列中，返回新添加的 waiter
         * 如果尾节点状态不是 CONDITION 状态，删除条件队列中所有状态不是 CONDITION 的节点
         * 如果队列为空，新增节点作为队列头节点，否则追加到尾节点上
         * @return 新添加的 waiter
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // If lastWaiter is cancelled, clean out.
            // 翻译：如果尾部的 waiter 是 CANCELLED 状态了，删除
            // 不是 CONDITION 状态 == 是 CANCELLED 状态，这也说明了条件队列的节点状态只有CONDITION和CANCELLED
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
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

        /**
         * 把条件队列头节点转移到同步队列去
         * @param first 条件队列头节点
         */
        private void doSignal(Node first) {
            do {
                // nextWaiter为空，说明到队尾了。代码思路为：如果条件队列循环完，则此时firstWaiter指向null，那么把lastWaiter也指向null
                if ((firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                // 从条件队列头部开始唤醒，所以直接把头结点.next 置为 null。代码思路其实就是把 node 从条件队列中移除了
                // 这里有个重要的点是，每次唤醒都是从队列头部开始唤醒，所以把 next 置为 null 没有关系，如果唤醒是从任意节点开始唤醒的话，就会有问题，容易造成链表的割裂
                first.nextWaiter = null;
                // transferForSignal 方法会把条件节点转移到同步队列中去。transferForSignal 只有一种情况会返回false，那就是处理的节点状态是CANCELLED，否则会返回true，循环结束
                // 条件队列的 node 不用管它的状态，因为在 await 的时候，会通过 unlinkCancelledWaiters 方法自动清除状态不是 CONDITION 的节点
                // (first = firstWaiter) != null 为 true 的，表示还可以继续循环，为 false 说明队列中的元素已经循环完了
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

        /**
         * 把等待队列所有节点依次转移到同步队列去
         * 本质就是 for 循环调用 transferForSignal 方法，将条件队列中的节点循环转移到同步队列中去
         * @param first 条件队列头节点
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                // 拿出条件队列队列头节点的下一个节点
                Node next = first.nextWaiter;
                // 把头节点从条件队列中删除
                first.nextWaiter = null;
                // 头节点转移到同步队列中去
                transferForSignal(first);
                // 开始循环头节点的下一个节点
                first = next;
            } while (first != null);
        }

        /**
         * 会检查尾部的 waiter 是不是已经不是CONDITION状态，如果不是，删除这些 waiter
         * unlink：分开；分离；拆开
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred(发生) during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
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

        /**
         * 唤醒阻塞在条件队列中的节点
         * 主要步骤都在 transferForSignal 方法里
         *
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signal() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 从头节点开始唤醒
            Node first = firstWaiter;
            if (first != null)
                // doSignal 方法会把条件队列中的节点转移到同步队列中去
                doSignal(first);
        }

        /**
         * 唤醒条件队列中的全部节点
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            // 拿到条件队列头节点
            Node first = firstWaiter;
            if (first != null)
                // 从条件队列头节点开始唤醒条件队列中所有的节点
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
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
         */

        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                   (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                   0;
        }

        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
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
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            // 内部会创建一个新的类型为Node.CONDITION的node节点，然后将该节点插入到条件队列的队尾
            Node node = addConditionWaiter();
            // 加入条件队列后，会释放当前线程获取的锁（也就是会操作锁对应的state变量的值），释放lock时申请的资源，唤醒同步队列队列头的节点（底层调用了release方法）。因为自己马上就要阻塞了，必须马上释放之前lock的资源，不然自己不被唤醒的话，别的线程就永远得不到该共享资源
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
            // 其他线程通过 signal 的方法已经把 node 从条件队列中转移到同步队列中去了；所以这里节点苏醒了之后，直接尝试 acquireQueued 去获取锁
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) // clean up if cancelled
                // 删除条件队列中所有状态不是CONDITION的节点
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
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
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
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
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
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

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
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
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
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
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
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
     * 同步队列的头
     * 公平的锁先入先出
     */
    private transient volatile Node head;

    /**
     * 等待队列的尾
     */
    private transient volatile Node tail;

    /**
     * 同步器的状态，子类会根据当前状态字段进行判断是否可以获得锁
     * 比如 CAS 成功给 state 赋值 1 算得到锁，赋值失败为得不到锁，CAS 成功给 state 赋值 0 算释放锁，赋值失败为释放失败
     * 如果当前state是0，那么可以获得锁
     * 可重入锁，每次获得锁 +1，每次释放锁 -1
     * 最重要的属性，所有继承 AQS 的锁都是通过这个字段来判断能不能获得锁，能不能释放锁
     */
    private volatile int state;

    /**
     * 自旋超时阀值，单位纳秒
     * 当设置等待时间时才会用到这个属性
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * @return current state value
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    // CAS 设置同步器的状态。
    // 和其他 CAS 设置一样，这里并不是修改 state 的值，而是设置 stateOffset 的值
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }


    /**
     * 方法主要目的：node 追加到同步队列的队尾
     * 主要思路（双向链表）：
     * 1.新 node.prev = 队尾
     * 2.队尾.next = 新 node
     * @param mode 表示 Node 的模式（排它模式还是共享模式）
     * @return 新增的 node
     */
    private Node addWaiter(Node mode) {
        // 初始化 Node
        Node node = new Node(Thread.currentThread(), mode);
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

    /**
     * 线程加入同步队列中方法，追加到队尾
     * 这里需要重点注意的是，返回值是添加 node 的前一个节点
     * @param node the node
     * @return 添加 node 的前一个节点
     */
    private Node enq(final Node node) {
        for (;;) {
            // 得到队尾节点
            Node t = tail;
            // 如果队尾为空，说明当前同步队列都没有初始化，进行初始化
            // new Node()为一个哨兵节点
            // tail = head = new Node();
            if (t == null) {
                if (compareAndSetHead(new Node()))
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

    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *
     * @param node the node
     */
    // node 排它模式下获得锁的节点
    // 排他模式下，获得锁的节点，一定会被设置成头节点
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 主要做两件事情
     * 1：把当前节点设置成头节点
     * 2：看看后继节点有无正在等待，并且也是共享模式的，有的话唤醒这些节点
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating(传播、繁殖) if either propagate > 0 or
     * PROPAGATE status was set.
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below 记录老的头节点为了下面的检查
        // 获得锁后，把当前节点设置为同步队列的 head 节点，即当前节点成为了新的哨兵节点（为什么说它也是一个哨兵节点？因为setHead方法里只是把head的指针指向了当前节点，里面的属性都设置为null了）
        setHead(node);
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated(表示指示) by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism(保守) in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        // propagate > 0 表示已经有节点获得共享锁了
        // 从doAcquireShared方法走到这里，propagate本来就是 >= 0
        // 已经有节点获得共享锁 || 老的头节点为空 || 老的头节点状态<0 || 新的头节点为空 || 新的头节点状态<0
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

    /**
     * 释放后继共享节点
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
         */
        for (;;) {
            Node h = head;
            // 判断还没有到队尾，即此时队列中至少有两个节点
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                // 如果队列 head 节点状态是 SIGNAL，说明后续节点都需要唤醒
                if (ws == Node.SIGNAL) {
                    // CAS 保证只有一个节点可以运行唤醒的操作
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    // 进行唤醒操作
                    unparkSuccessor(h);
                }
                // 如果队列 head 节点状态是 0，把状态改成 PROPAGATE
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            // 第一种情况，头节点没有发生移动，结束。
            // 第二种情况，因为此方法可以被两处调用，一次是获得锁的地方，一处是释放锁的地方
            // 加上共享锁的特性就是可以多个线程获得锁，也可以释放锁，这就导致头节点可能会发生变化；如果头节点发生了变化，就继续循环，一直循环到头节点不变化时，结束循环。
            if (h == head)                   // loop if head changed
                break;
        }
    }

    // Utilities for various versions of acquire

    /**
     * Cancels an ongoing attempt to acquire.
     * 翻译：取消一个正在进行的获取尝试。
     * ongoing：仍在进行的；不断前进的；持续存在的
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors 跳过状态为 CANCELLED 的前驱节点
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED;

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
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

            node.next = node; // help GC
        }
    }

    /**
     * 当前线程可以安心阻塞等待的标准，就是前一个节点线程状态是SIGNAL了。
     * 关键操作：
     * 1：确认前驱节点是否有效（状态不是CANCELLED），无效的话，一直往前找到有效的（状态不是CANCELLED）节点。
     * 2：把前置节点状态置为 SIGNAL。
     * 1、2 两步操作，有可能一次就成功，有可能需要外部循环多次才能成功（外面是个无限的 for 循环），但最后一定是可以成功的
     * @param pred 前一个节点
     * @param node 当前节点
     * @return 是否成功把pred节点设置为SIGNAL状态
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        // 如果前一个节点 waitStatus 状态是SIGNAL，直接返回，不需要再自旋了
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             * 翻译：该节点已经设置了请求释放的状态去标识它，所以它可以安全地park。
             */
            return true;
        // 如果前一个节点的状态是 CANCELLED
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             * 翻译：前驱节点是CANCELLED状态。跳过前驱节点并指示重试。
             */
            // 找到前一个状态不是取消的节点，是为了要把当前 node 挂在有效节点后面（双向链表）。因为如果prev节点状态是CANCELLED，就不能作为 node 的前置节点，所以必须要找到有效的prev节点。在这里同时会删除那个状态为CANCELLED的状态
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
             * 翻译：waitStatus必须是0或者PROPAGATE。我们需要一个信号指示它，但是还不能park。调用者将需要去重试来确认它不能在parking之前acquire
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     * 翻译：方便的方法去中断当前线程。
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * 主要做两件事情：
     * 1：通过不断的自旋尝试，使自己前一个节点的状态变成 signal（线程需要被唤醒），然后阻塞自己。
     * 2：如果前一个节点获得锁，并执行完成之后（即获得锁的线程执行完成之后），再释放锁时，会把阻塞的 node 唤醒,node 唤醒之后再次自旋（无限 for 循环）尝试获得锁
     *
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting 返回false表示获得锁成功（没有阻塞当前线程），返回true表示失败（阻塞了当前线程）
     */
    final boolean acquireQueued(final Node node, int arg) {
        // 是否获取锁失败
        boolean failed = true;
        try {
            // 是否阻塞了当前线程
            boolean interrupted = false;
            // 自旋
            for (;;) {
                // 取该节点的前驱节点
                final Node p = node.predecessor();
                /**
                 * 有两种情况会走到 p == head（该节点的前驱节点 == AQS队列头节点）：
                 * 1:node 之前没有获得锁，进入 acquireQueued 方法时，才发现他的前置节点就是头节点，于是尝试获得一次锁（tryAcquire）；如果自己 tryAcquire 成功，就立马把自己设置成 head，把上一个节点移除；如果 tryAcquire 失败，尝试进入同步队列；；；；；；继续理解：第一次调用enq方法初始化同步队列时，给头节点设置了一个哨兵节点，也就是说第一次执行acquireQueued方法时会达成p == head的条件
                 * 2:node 之前一直在阻塞沉睡，然后被唤醒，此时唤醒 node 的节点正是其前置节点，也能走到 if，具体见 release 方法
                 */
                if (p == head && tryAcquire(arg)) {
                    // 获得锁后，把当前节点设置为同步队列的 head 节点，即当前节点成为了新的哨兵节点（为什么说它也是一个哨兵节点？因为setHead方法里只是把head的指针指向了当前节点，里面的属性都设置为null了）
                    setHead(node);
                    // p被回收
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }

                // shouldParkAfterFailedAcquire 把node的前一个节点状态置为SIGNAL；只要前一个节点状态是SIGNAL了，那么自己就可以阻塞(park)了
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // parkAndCheckInterrupt 方法可以阻塞当前线程（LockSupport.park），所以线程如果醒来后，它仍然在自旋 for 循环里，所以就能再次去自旋尝试获得锁
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            // 如果获得node的锁失败，将node从队列中移除
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in exclusive interruptible mode.
     * @param arg the acquire argument
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
                    p.next = null; // help GC
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
                    p.next = null; // help GC
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

    /**
     * 大部分逻辑和 acquireQueued 一致的
     *
     * Acquires in shared uninterruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireShared(int arg) {
        // 把当前线程以【共享】模式追加到同步队列的队尾
        final Node node = addWaiter(Node.SHARED);
        // 是否获取锁失败
        boolean failed = true;
        try {
            // 是否阻塞了当前线程
            boolean interrupted = false;
            // 自旋
            for (;;) {
                // 取该节点的前驱节点
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    // 大于 0 说明成功获得锁
                    if (r >= 0) {
                        // 此处和排它锁不同，排它锁使用的是 setHead，这里的 setHeadAndPropagate 方法不仅仅是把当前节点设置成 head，还会唤醒头节点的后继节点。然后后继节点会再自旋来获取共享锁
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                // 这里都和排他锁逻辑是一致的
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            // 这里都和排他锁逻辑是一致的
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
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
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
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

    /**
     * Acquires in shared timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
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
                        p.next = null; // help GC
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


    // 排他模式下，根据状态来判断是否能够获得锁
    protected boolean tryAcquire(int arg) {
        // 直接抛出一个异常，表明需要子类去实现
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    // 返回表示 true 已经加锁
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 排他（独占）模式下，尝试获得锁
     * 主要步骤是：
     * 1.尝试执行一次 tryAcquire 获取锁，如果成功直接返回，失败走第 2 步；tryAcquire 交给子类去实现
     * 2.线程尝试进入同步队列，首先调用 addWaiter 方法，把当前线程以【排他（独占）】模式追加到同步队列的队尾
     * 3.接着调用 acquireQueued 方法，两个作用，1：阻塞当前节点，2：节点被唤醒时，使其能够继续尝试获得锁（自旋）
     * 4.如果 2、3 步失败了，打断当前线程
     */
    public final void acquire(int arg) {
        // tryAcquire方法是需要实现类去实现的，实现思路一般都是 cas 给 stats 赋值来决定是否能获得锁
        if (!tryAcquire(arg) &&
            // addWaiter 入参 Node.EXCLUSIVE 代表是排他模式
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            // 中断当前线程
            selfInterrupt();
    }



    //可以中断的获得锁
    //一旦当前线程第一次没有获得锁，自旋时也没有获得，并且进入同步队列阻塞后，一旦被唤醒，就会抛出异常
    //方法的使用场景不大，因为异常是在被唤醒之后才会抛出异常，如果想得不到锁就抛异常，不如使用带有
    //等待时间的锁
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
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * unlock的基础方法
     *
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @return the value returned from {@link #tryRelease}
     */
    public final boolean release(int arg) {
        // tryRelease 交给实现类去实现，一般就是用当前同步器状态减去 arg，如果返回 true 说明成功释放锁
        if (tryRelease(arg)) {
            Node h = head;
            // 头节点不为空，并且非初始化状态，就去释放
            if (h != null && h.waitStatus != 0)
                // 从头开始唤醒等待锁的节点
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * 当线程释放锁成功后或者共享模式获取锁后，从 node 开始唤醒同步队列中的节点
     * 通过唤醒机制，保证线程不会一直在同步队列中阻塞等待
     * @param node 当前释放锁的节点，也是同步队列的头节点
     */
    private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        // 如果节点不是CANCELLED状态，把节点的状态置为初始化 0
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        // 取node节点的后继节点
        Node s = node.next;
        // 队列中没有其他等待的节点 || node节点的后继节点s的状态是CANCELLED
        // 就从队尾开始，向前遍历，找到第一个 waitStatus 不是 CANCELLED 的
        if (s == null || s.waitStatus > 0) {
            s = null;
            // 这里使用尾迭代，而不是从头开始迭代的原因：主要是因为节点被阻塞时是在 acquireQueued 方法（parkAndCheckInterrupt 方法）里面被阻塞的，唤醒时也一定会在 acquireQueued 方法里面被唤醒，唤醒之后获取锁判断的条件是，当前节点的前置节点是否是头节点（p == head），这里是判断当前节点的前置节点，所以这里必须使用从尾到头的迭代顺序才行，目的就是为了过滤掉无效的前置节点，不然节点被唤醒时，发现其前置节点还是无效节点，就又会陷入阻塞；；；；；；继续理解：如果node节点的后继节点的状态是CANCELLED，那么它就无需被唤醒，所以不能直接唤醒node.next，而要使用尾迭代来找到node后边第一个状态不是CANCELLED的节点，然后唤醒它；唤醒它之后，在acquireQueued方法里第一次自旋循环在shouldParkAfterFailedAcquire方法里会把这个（或者node节点的后继连续多个）状态是CANCELLED的节点给删掉，然后第二次自旋循环就能去获取锁了；如果直接唤醒状态是CANCELLED的后继节点node.next，看源码里好像倒是也能唤醒，但是CANCELLED这个状态本身好像就没有意义了
            for (Node t = tail; t != null && t != node; t = t.prev)
                // t.waitStatus <= 0 说明 t 没有被取消，肯定还在等待被唤醒
                if (t.waitStatus <= 0)
                    s = t;
        }
        // 唤醒以上代码找到的线程
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * 共享模式下，尝试获得锁
     * tryAcquireShared 首先尝试获得锁，返回值小于 0 表示没有获得锁
     * 共享锁和排他锁最大的不同在于：对于同一个共享资源
     * 排他锁只能让一个线程获得，共享锁可以让多个线程获得
     * arg 可以被子类当做任意参数，比如当做可获得锁线程的最大个数
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    public final void acquireShared(int arg) {
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
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
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
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
            doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * 共享模式下，释放当前线程的共享锁
     *
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            // 这个方法也是线程在获得锁时，唤醒后续节点时调用的方法（doAcquireShared-setHeadAndPropagate-doReleaseShared）
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
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
     *
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue
     * @throws NullPointerException if the thread is null
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
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * 该方法是公平锁加锁时tryAcquire()方法里实现公平性的关键
     * 会判断当前线程是不是属于同步队列的头节点的下一个节点(头节点是释放锁的节点)，如果是(返回false)，符合先进先出的原则，可以获得锁；如果不是(返回true)，则继续等待
     *
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
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
     *
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
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

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting to acquire
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
     *
     * @return the collection of threads
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
     *
     * @return the collection of threads
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
     *
     * @return the collection of threads
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
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * 判断node节点是否在同步队列中
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     * @param node the node
     * @return true if is reacquiring
     */
    final boolean isOnSyncQueue(Node node) {
        // 如果node的状态是CONDITION || node的前驱节点为空
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        if (node.next != null) // If has successor, it must be on queue 如果有后继节点，它必须在队列中
            return true;
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        return findNodeFromTail(node);
    }

    /**
     * 从同步队列尾节点开始寻找node，找到返回true，否则返回false
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     * @return true if present
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

    /**
     * 把条件队列节点转移到同步队列中去
     * 大概思路：
     * 1.把 node 状态从 CONDITION 改为 0，失败直接返回false，成功走到 2
     * 2.node 追加到同步队列的队尾
     * 3.将 node 的前一个节点状态置为 SIGNAL，成功直接返回，失败直接唤醒
     *
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     * @param node the node
     * @return true if successfully transferred (else the node was
     * cancelled before signal) 返回 true 表示转移成功， false 失败
     */
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 翻译：如果无法更改waitStatus，则节点已被取消。
         */
        // 将 node 的状态从 CONDITION 修改成初始化，失败返回 false
        // 只把状态为CONDITION的条件队列节点从条件队列转移到同步队列中去。代码的思路是：第一步要把node状态改为0，而如果状态从CONDITION改为0失败，则说明状态不是CONDITION，那就只能是CANCELLED，而状态是CANCELLED的条件队列节点我们不需要把它从条件队列转移到同步队列中去，所以直接返回false，取下一个条件队列节点来处理。其实一行代码做了两件事，1.判断节点状态，2.如果节点状态是CONDITION则直接改成0。简直666
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /*
         * 当前条件队列节点加入到同步队列，返回的 p 是 node 在同步队列中的前一个节点
         * 看命名是 p，实际是 pre 前一个单词的缩写
         * Splice onto queue and try to set waitStatus of predecessor(前任) to
         * indicate(表明) that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        // 状态修改成 SIGNAL，如果成功直接返回
        // 把当前节点的前一个节点修改成 SIGNAL 的原因，是因为 SIGNAL 本身就表示当前节点后面的节点都是需要被唤醒的
        // 如果 p 节点 状态为CANCELLED || 状态不能修改成SIGNAL
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     * @return true if cancelled before the node was signalled
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
         */
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }

    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     * @param node the condition node for this wait
     * @return previous sync state
     */
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

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
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
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
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
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
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
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }



    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
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
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
