package demo.custom.workflow.service.impl;

import demo.custom.workflow.FlowContent;
import demo.custom.workflow.service.DomainAbilityBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:31
 * @description
 */
@Slf4j
@Component
public class BeanOne implements DomainAbilityBean {

    @Override
    public FlowContent invoke(FlowContent content) {
        log.info("BeanOne is running, thread name is {}", Thread.currentThread().getName());
        return content;
    }
}
