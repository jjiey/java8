package demo.custom.workflow.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:02
 * @description 流程引擎中的阶段或步骤
 */
@Getter
@AllArgsConstructor
public enum StageEnum {

    PARAM_VALID(1, "PARAM_VALID", "参数校验"),

    BUSINESS_VALID(2, "BUSINESS_VALID", "业务校验"),

    IN_TRANSACTION(3, "IN_TRANSACTION", "事务中落库"),

    AFTER_TRANSACTION(4, "AFTER_TRANSACTION", "事务后事件"),
    ;

    static Map<Integer, StageEnum> map = Maps.newHashMap();

    static {
        for (StageEnum stageEnum : StageEnum.values()) {
            map.put(stageEnum.getType(), stageEnum);
        }
    }

    private int type;

    private String code;

    private String desc;

    public static StageEnum get(int type) {
        return map.get(type);
    }
}
