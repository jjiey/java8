package demo.custom.workflow.service.impl;

import demo.custom.workflow.ContextCache;
import demo.custom.workflow.FlowContent;
import demo.custom.workflow.service.DomainAbilityBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author yangjie
 * @date Created in 2020/2/2 22:08
 * @description
 */
@Slf4j
@Component
public class BeanThree implements DomainAbilityBean {

    @Override
    public FlowContent invoke(FlowContent content) {
        int i = new Random().nextInt(1000);
        ContextCache.putAttribute("key1", "value" + i);
        log.info("put key1, value{} to ContextCache success", i);
        return null;
    }
}
