package demo.custom.workflow;

import demo.custom.workflow.anno.AsyncComponent;
import demo.custom.workflow.service.DomainAbilityBean;
import demo.custom.workflow.util.AnnotationUtils;
import org.springframework.aop.support.AopUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:46
 * @description 组件执行器
 */
public class ComponentExecutor {

    private static ExecutorService executor = new ThreadPoolExecutor(15, 15, 365L, TimeUnit.DAYS, new LinkedBlockingQueue<>());

    /**
     * 如果 SpringBean 上有 AsyncComponent 注解，表示该 SpringBean 需要异步执行，就丢到线程池中去
     * @param content
     */
    public static void run(DomainAbilityBean domainAbility, FlowContent content) {
        // 判断类上是否有 AsyncComponent 注解
        if (AnnotationUtils.isAnnotationPresent(AsyncComponent.class, AopUtils.getTargetClass(domainAbility))) {
            // 提交到线程池中
//            executor.submit(() -> { domainAbility.invoke(content); });
            executor.submit(new AsyncRunnable(() -> domainAbility.invoke(content)));
            return;
        }
        // 同步 SpringBean 直接执行。
        domainAbility.invoke(content);
    }
}
