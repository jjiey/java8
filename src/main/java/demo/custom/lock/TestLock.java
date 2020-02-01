package demo.custom.lock;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yangjie
 * @date Created in 2020/1/30 18:18
 * @description 测试用例
 */
@Slf4j
public class TestLock {

    public static void main(String[] args) {
        log.info("模仿开始获得 mysql 链接");
        MysqlConnection mysqlConnection = new MysqlConnection(10);
        log.info("初始化 Mysql 链接最大只能获取 10 个");
        for(int i = 0; i < 12; i++) {
            if(null != mysqlConnection.getLimitConnection()){
                log.info("获得第{}个数据库链接成功",i+1);
            } else {
                log.info("获得第{}个数据库链接失败：数据库连接池已满",i+1);
            }
        }
        log.info("模仿开始释放 mysql 链接");
        for(int i = 0; i < 12; i++) {
            if(mysqlConnection.releaseLimitConnection()){
                log.info("释放第{}个数据库链接成功",i+1);
            } else {
                log.info("释放第{}个数据库链接失败",i+1);
            }
        }
        log.info("模仿结束");
    }
}
