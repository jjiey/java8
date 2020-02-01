package demo.custom.workflow.service;

import demo.custom.workflow.FlowContent;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:14
 * @description 领域行为
 */
public interface DomainAbilityBean {

    /**
     * 领域行为的方法入口
     */
    FlowContent invoke(FlowContent content);
}
