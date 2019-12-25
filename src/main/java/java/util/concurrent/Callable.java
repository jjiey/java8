/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * A task that returns a result and may throw an exception.
 * Implementors define a single method with no arguments called
 * {@code call}.
 * 翻译：一个返回结果的任务，可能会抛出异常。实现者定义一个没有参数调用的方法
 *
 * 一个可以返回结果的任务
 *
 * <p>The {@code Callable} interface is similar to {@link
 * java.lang.Runnable}, in that both are designed for classes whose
 * instances are potentially executed by another thread.  A
 * {@code Runnable}, however, does not return a result and cannot
 * throw a checked exception.
 * 翻译：Callable接口和Runnable接口很像，它们都是为那些实例可能由另一个线程执行的类设计。但是，Runnable不能返回结果，也不能抛出检查异常。
 *
 * <p>The {@link Executors} class contains utility methods to
 * convert from other common forms to {@code Callable} classes.
 * 翻译：Executors类包含了一些工具方法可以将其他常用的形式转换为Callable类
 *
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> the result type of method {@code call} 方法的返回类型
 */
@FunctionalInterface
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     * 翻译：计算一个结果，或在无法这样做时抛出异常。
     *
     * @return computed result 返回计算的结果
     * @throws Exception if unable to compute a result 如果不能计算结果则抛出Exception
     */
    V call() throws Exception;
}
