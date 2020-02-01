package demo.custom.workflow;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import demo.custom.workflow.enums.StageEnum;
import demo.custom.workflow.service.DomainAbilityBean;
import demo.custom.workflow.service.impl.BeanOne;
import demo.custom.workflow.service.impl.BeanTwo;
import demo.custom.workflow.util.ApplicationContextHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:14
 * @description
 */
@Slf4j
@Component
public class FlowCenter {

    /**
     * 共享变量，方便访问，并且是 ConcurrentHashMap
     */
    public static final Map<String, Map<StageEnum, List<DomainAbilityBean>>> FLOW_MAP = Maps.newConcurrentMap();

    @PostConstruct
    public void init() {
        // 初始化 flowMap，可能是从数据库，或者 xml 文件中加载 map
        Map<StageEnum, List<DomainAbilityBean>> stageMap = FLOW_MAP.getOrDefault("flow1", Maps.newConcurrentMap());
        for (StageEnum value : StageEnum.values()) {
            List<DomainAbilityBean> domainAbilitys = stageMap.getOrDefault(value, Lists.newCopyOnWriteArrayList());
            if(CollectionUtils.isEmpty(domainAbilitys)){
                domainAbilitys.addAll(ImmutableList.of(
                        ApplicationContextHelper.getBean(BeanOne.class),
                        ApplicationContextHelper.getBean(BeanTwo.class)
                ));
                stageMap.put(value,domainAbilitys);
            }
        }
        FLOW_MAP.put("flow1",stageMap);
        // 打印出加载完成之后的数据结果
        log.info("init success,flowMap is {}", JSON.toJSONString(FLOW_MAP));
    }
}
