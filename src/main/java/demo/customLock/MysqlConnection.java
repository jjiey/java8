package demo.customLock;

import java.sql.Connection;

/**
 * @author yangjie
 * @date Created in 2020/1/30 18:16
 * @description Mysql连接
 */
public class MysqlConnection {

    private final ShareLock lock;

    // maxConnectionSize 表示最大链接数
    public MysqlConnection(int maxConnectionSize) {
        lock = new ShareLock(maxConnectionSize);
    }

    // 得到一个 mysql 链接，底层实现省略
    private Connection getConnection(){
        // todo 获取mysql连接
        return null;
    }

    // 对外获取 mysql 链接的接口
    // 这里不用try finally 的结构，获得锁实现底层不会有异常
    // 即使出现未知异常，也无需释放锁
    public Connection getLimitConnection() {
        if (lock.lock()) {
            return getConnection();
        }
        return null;
    }

    // 对外释放 mysql 链接的接口
    public boolean releaseLimitConnection() {
        return lock.unLock();
    }
}
