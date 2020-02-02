package demo.custom.workflow;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * @author yangjie
 * @date Created in 2020/2/2 22:23
 * @description 自定义任务
 */
@Slf4j
@Getter
public class AsyncRunnable implements Runnable {

    private Runnable runnable;
    /**
     * 临时存储数据
     */
    private HashMap hashMap = Maps.newHashMap();

    public AsyncRunnable(Runnable runnable) {
        this.runnable = runnable;
        copy(runnable);
    }

    @Override
    public void run() {
        try {
            ContextCache.putAllAttribute(hashMap);
            if (null != runnable) {
                runnable.run();
            }
        } catch (Exception e) {
            log.error("[AsyncRunnable-run] has error", e);
            throw new RuntimeException(e);
        } finally {
            hashMap = Maps.newHashMap();
            ContextCache.clean();
        }
    }

    /**
     * 把提交任务的ThreadLocal拷贝到hashMap中
     * @param runnable
     */
    private void copy(Runnable runnable) {
        try {
            hashMap.putAll(ContextCache.getMap());
        } catch (Exception e) {
            log.error("[AsyncRunnable-copy] has error,runnable is {}",
                    runnable.toString(), e);
            throw new RuntimeException(e);
        }
    }
}
