/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 *
 * 现象：任何 class 只要实现了 Runnable，就能被线程执行。
 * 定义了一个通用的标准，只要想被运行，就实现它。
 * 可以在不开子线程的情况下运行
 *
 * The <code>Runnable</code> interface should be implemented by any
 * class whose instances are intended to be executed by a thread. The
 * class must define a method of no arguments called <code>run</code>.
 * <p>
 * This interface is designed to provide a common protocol for objects that
 * wish to execute code while they are active. For example,
 * <code>Runnable</code> is implemented by class <code>Thread</code>.
 * Being active simply means that a thread has been started and has not yet been stopped.
 * 翻译：Runnable接口应该由其实例打算通过一个线程来执行的任何类来实现。该类必须定义一个无参的方法，方法名称为run。
 * 此接口被设计用来对希望在活动状态下执行代码的对象提供一个公共协议。例如，Thread类实现了Runnable。变为active仅仅意味着一个线程已经启动，而且还没有被停止。
 *
 *
 * <p>
 * In addition, <code>Runnable</code> provides the means for a class to be
 * active while not subclassing <code>Thread</code>. A class that implements
 * <code>Runnable</code> can run without subclassing <code>Thread</code>
 * by instantiating a <code>Thread</code> instance and passing itself in
 * as the target.  In most cases, the <code>Runnable</code> interface should
 * be used if you are only planning to override the <code>run()</code>
 * method and no other <code>Thread</code> methods.
 * This is important because classes should not be subclassed
 * unless the programmer intends on modifying or enhancing the fundamental behavior of the class.
 * 翻译：此外，Runnable提供了当一个类不是Thread的子类时状态仍处于active状态的方法。一个实现了Runnable的类通过实例化一个Thread实例并将自身作为目标传入来运行，不用作为Thread的子类。在大多数情况下，如果你只打算重写run()方法，而不打算重写其他的Thread方法，那么应该使用Runnable接口。这很重要，除非程序员打算修改或增强类的基本行为，否则不应该作为子类
 *
 * @author  Arthur van Hoff
 * @see     java.lang.Thread
 * @see     java.util.concurrent.Callable
 * @since   JDK1.0
 */
@FunctionalInterface
public interface Runnable {
    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     * 翻译：当一个对象实现了Runnable接口被用来创建一个线程时，启动该线程会造成该对象的run方法在单独执行的线程中被调用。
     * run的一般契约（general contract）是无论怎样它都可以做任何操作。
     *
     * @see     java.lang.Thread#run()
     */
    public abstract void run();
}
