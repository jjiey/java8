package demo.countDownLatch;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author yangjie
 * @date Created in 2020/1/30 18:24
 * @description 模拟批量退款
 */
@Slf4j
public class BatchRefundDemo {

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(20));

    /**
     * 批量退款数量
     */
    private static final int INIT_REFUND_COUNT = 30;

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(INIT_REFUND_COUNT);
        // 准备批量商品
        List<Long> items = Lists.newArrayListWithCapacity(INIT_REFUND_COUNT);
        for (int i = 0; i < INIT_REFUND_COUNT; i++) {
            items.add(Long.valueOf(i + ""));
        }
        // 准备开始批量退款
        List<Future> futures = Lists.newArrayListWithCapacity(INIT_REFUND_COUNT);
        items.forEach(item -> {
            // 使用 Callable，因为我们需要等到返回值
            Future<Boolean> future = EXECUTOR_SERVICE.submit(() -> {
                boolean result = refundByItem(item);
                // 每个子线程都会执行 countDown，使 state - 1 ，但只有最后一个才能真的唤醒主线程
                countDownLatch.countDown();
                return result;
            });
            // 收集批量退款的结果
            futures.add(future);
        });
        log.info("{} 个商品已经在退款中", INIT_REFUND_COUNT);
        // 使主线程阻塞，一直等待所有商品都退款完成，才能继续执行
        countDownLatch.await();
        log.info("{} 个商品已经退款完成", INIT_REFUND_COUNT);
        // 拿到所有结果进行分析
        List<Boolean> result = futures.stream().map(fu-> {
            try {
                // get 的超时时间设置的是 1 毫秒，是为了说明此时所有的子线程都已经执行完成了
                return (Boolean) fu.get(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
            return false;
        }).collect(Collectors.toList());
        // 打印结果统计
        long success = result.stream().filter(r -> r.equals(true)).count();
        log.info("执行结果成功{},失败{}", success, result.size() - success);
    }

    /**
     * 根据商品 ID 进行退款
     * @param itemId 商品 ID
     * @return 退款结果
     */
    private static boolean refundByItem(Long itemId) {
        try {
            // 线程沉睡 30 毫秒，模拟单个商品退款过程
            Thread.sleep(30);
            log.info("refund success,itemId is {}", itemId);
            return true;
        } catch (Exception e) {
            log.error("refundByItemError,itemId is {}", itemId);
            return false;
        }
    }
}
