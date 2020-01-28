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

/**
 * 一个同步器，被一个线程拥有
 * A synchronizer that may be exclusively owned by a thread.  This
 * class provides a basis for creating locks and related synchronizers
 * that may entail a notion of ownership.  The
 * {@code AbstractOwnableSynchronizer} class itself does not manage or
 * use this information. However, subclasses and tools may use
 * appropriately maintained values to help control and monitor access
 * and provide diagnostics.
 * 翻译：一个可以被一个线程独占的同步器。该类提供了创建锁和相关同步器的基础，这些同步器可能包含一个所有权的概念。AbstractOwnableSynchronizer类自身不管理或使用这些信息。但是，子类和工具可以适当的维护值来帮助控制和监控访问并提供诊断。
 *
 * AbstractOwnableSynchronizer的作用就是为了知道当前是那个线程获得了锁,方便监控用的
 *
 * @since 1.6
 * @author Doug Lea
 */
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** Use serial ID even though all fields transient. */
    private static final long serialVersionUID = 3737899427754241961L;

    /**
     * Empty constructor for use by subclasses.
     * 翻译：为子类使用的空构造函数。
     */
    protected AbstractOwnableSynchronizer() { }

    /**
     * The current owner of exclusive mode synchronization.
     * 翻译：独占模式同步的当前所有者。
     */
    private transient Thread exclusiveOwnerThread;

    /**
     * Sets the thread that currently owns exclusive access.
     * A {@code null} argument indicates that no thread owns access.
     * This method does not otherwise impose any synchronization or
     * {@code volatile} field accesses.
     * @param thread the owner thread 所有者线程
     * 翻译：设置当前拥有独占访问权的线程。参数null表明没有线程拥有访问权限。此方法不强制任何同步或volatile字段访问。
     *
     * 获得锁的线程 set 进来就好了
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    /**
     * Returns the thread last set by {@code setExclusiveOwnerThread},
     * or {@code null} if never set.  This method does not otherwise
     * impose any synchronization or {@code volatile} field accesses.
     * @return the owner thread 所有者线程
     * 翻译：返回最近通过setExclusiveOwnerThread方法set的线程，如果没有set返回null。此方法不强制任何同步或volatile字段访问。
     */
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
