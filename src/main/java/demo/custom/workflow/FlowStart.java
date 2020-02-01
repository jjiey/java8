package demo.custom.workflow;

import com.google.common.collect.Maps;
import demo.custom.workflow.enums.StageEnum;
import demo.custom.workflow.service.DomainAbilityBean;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:17
 * @description 流程引擎对外的 API
 */
public class FlowStart {

    /**
     * 流程引擎开始
     * @param flowName 流程的名字
     * @param content
     */
    public void start(String flowName, FlowContent content) {
        invokeParamValid(flowName, content);
        invokeBusinessValid(flowName, content);
        invokeInTramsactionValid(flowName, content);
        invokeAfterTramsactionValid(flowName, content);
    }

    /**
     * 执行参数校验
     * @param flowName 流程的名字
     * @param content
     */
    private void invokeParamValid(String flowName, FlowContent content) {
        stageInvoke(flowName, StageEnum.PARAM_VALID, content);
    }

    /**
     * 执行业务校验
     * @param flowName 流程的名字
     * @param content
     */
    private void invokeBusinessValid(String flowName, FlowContent content) {
        stageInvoke(flowName, StageEnum.BUSINESS_VALID, content);
    }

    /**
     * 执行事务中
     * @param flowName 流程的名字
     * @param content
     */
    private void invokeInTramsactionValid(String flowName, FlowContent content) {
        stageInvoke(flowName, StageEnum.IN_TRANSACTION, content);
    }

    /**
     * 执行事务后
     * @param flowName 流程的名字
     * @param content
     */
    private void invokeAfterTramsactionValid(String flowName, FlowContent content) {
        stageInvoke(flowName, StageEnum.AFTER_TRANSACTION, content);
    }

    /**
     * 批量执行 Spring Bean
     * @param flowName 流程的名字
     * @param stage 流程引擎中的阶段或步骤
     * @param content
     */
    private void stageInvoke(String flowName, StageEnum stage, FlowContent content) {
        List<DomainAbilityBean> domainAbilitys = FlowCenter.FLOW_MAP.getOrDefault(flowName, Maps.newHashMap()).get(stage);
        if (CollectionUtils.isEmpty(domainAbilitys)) {
            throw new RuntimeException("找不到该流程对应的领域行为" + flowName);
        }
        for (DomainAbilityBean domainAbility : domainAbilitys) {
//            domainAbility.invoke(content);
            ComponentExecutor.run(domainAbility, content);
        }
    }
}
