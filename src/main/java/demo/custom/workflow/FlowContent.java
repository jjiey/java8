package demo.custom.workflow;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yangjie
 * @date Created in 2020/2/2 0:11
 * @description 流程上下文
 */
@Data
public class FlowContent implements Serializable {

    /**
     * 入参数
     */
    private Object request;

    /**
     * 出参
     */
    private Object response;
}
