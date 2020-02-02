package demo.custom.workflow.service.impl;

import demo.custom.workflow.ContextCache;
import demo.custom.workflow.FlowContent;
import demo.custom.workflow.anno.AsyncComponent;
import demo.custom.workflow.service.DomainAbilityBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author yangjie
 * @date Created in 2020/2/2 22:12
 * @description
 */
@AsyncComponent
@Slf4j
@Component
public class BeanFive implements DomainAbilityBean {

    @Override
    public FlowContent invoke(FlowContent content) {
        String value = ContextCache.getAttribute("key1");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("get 线程名称为 {}, value is {}", Thread.currentThread().getName(), value);
        log.info("------------------");
        return null;
    }
}
