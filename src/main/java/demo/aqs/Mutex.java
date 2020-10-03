package demo.aqs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Mutex implements Lock, java.io.Serializable {

    // 我们的内部帮助类
    private static class Sync extends AbstractQueuedSynchronizer {
        // 报告是否处于锁定状态
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // 如果 state 为零，则获取锁
        @Override
        public boolean tryAcquire(int acquires) {
            assert acquires == 1; // 否则未使用
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 通过将 state 设置为零来释放锁
        @Override
        protected boolean tryRelease(int releases) {
            assert releases == 1; // 否则未使用
            if (getState() == 0) throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        // 提供一个 Condition
        Condition newCondition() {
            return new ConditionObject();
        }

        // 正确反序列化
        private void readObject(ObjectInputStream s)
                throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // 重置为 unlocked 状态
        }
    }

    // 同步对象完成了所有的困难工作。我们只是期待它。
    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}
