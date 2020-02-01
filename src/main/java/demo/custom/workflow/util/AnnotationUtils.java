package demo.custom.workflow.util;

import java.lang.annotation.Annotation;

/**
 * @author yangjie
 * @date Created in 2020/2/2 0:09
 * @description 注解工具类
 */
public class AnnotationUtils {

    /**
     * 判断注解是不是当前类的注解
     * @param targetAnnotation
     * @param annotatedType
     * @return boolean
     */
    public static boolean isAnnotationPresent(final Class<? extends Annotation> targetAnnotation, final Class<?> annotatedType) {
        return findAnnotation(targetAnnotation, annotatedType) != null;
    }

    /**
     * 从当前类中找到指定的注解
     * @param targetAnnotation
     * @param annotatedType
     * @param <A>
     * @return Annotation
     */
    public static <A extends Annotation> A findAnnotation(final Class<A> targetAnnotation, final Class<?> annotatedType) {
        A foundAnnotation = annotatedType.getAnnotation(targetAnnotation);
        if (foundAnnotation == null) {
            return foundAnnotation;
        }
        for (Annotation annotation : annotatedType.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(targetAnnotation)) {
                foundAnnotation = annotationType.getAnnotation(targetAnnotation);
                break;
            }
        }
        return foundAnnotation;
    }
}
