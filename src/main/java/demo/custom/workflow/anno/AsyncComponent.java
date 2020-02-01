package demo.custom.workflow.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yangjie
 * @date Created in 2020/2/1 22:30
 * @description 异步 SpringBean 执行注解，需要异步执行的话，就打上该注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncComponent {
}
