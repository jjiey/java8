/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * The abstract class {@code Number} is the superclass of platform
 * classes representing numeric values that are convertible to the
 * primitive types {@code byte}, {@code double}, {@code float}, {@code
 * int}, {@code long}, and {@code short}.
 * 抽象类 Number 是平台类的超类，这些类表示可转换为基本类型 byte、 double、 float、 int、 long 和 short 的数值。
 *
 * The specific semantics of the conversion from the numeric value of
 * a particular {@code Number} implementation to a given primitive
 * type is defined by the {@code Number} implementation in question.
 * 从一个特定的 Number 实现类的数值转换到给定的基本类型的特定语义由相关的 Number 实现定义
 *
 * For platform classes, the conversion is often analogous to a
 * narrowing primitive conversion or a widening primitive conversion
 * as defining in <cite>The Java&trade; Language Specification</cite>
 * for converting between primitive types.  Therefore, conversions may
 * lose information about the overall magnitude of a numeric value, may
 * lose precision, and may even return a result of a different sign
 * than the input.
 * 对于平台类，转换通常类似于在 The Java™ Language Specification(Java 语言规范) 中定义的用于在原始类型之间转换的缩窄原语转换或扩展原语转换。因此，转换可能会丢失关于数值总体大小的信息，可能会丢失精度，甚至可能返回与输入符号不同的结果。
 *
 * See the documentation of a given {@code Number} implementation for
 * conversion details.
 * 有关转换细节，请参阅给定的 Number 实现的文档。
 *
 * @author      Lee Boynton
 * @author      Arthur van Hoff
 * @jls 5.1.2 Widening Primitive Conversions
 * @jls 5.1.3 Narrowing Primitive Conversions
 * @since   JDK1.0
 */
public abstract class Number implements java.io.Serializable {
    /**
     * Returns the value of the specified number as an {@code int},
     * which may involve rounding or truncation.
     * 以 int 的形式返回指定数字的值。这可能涉及四舍五入或截断。
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code int}.
     *          转换为 int 类型后此对象表示的数值。
     */
    public abstract int intValue();

    /**
     * Returns the value of the specified number as a {@code long},
     * which may involve rounding or truncation.
     * 以 long 的形式返回指定数字的值。这可能涉及四舍五入或截断。
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code long}.
     *          转换为 long 类型后此对象表示的数值。
     */
    public abstract long longValue();

    /**
     * Returns the value of the specified number as a {@code float},
     * which may involve rounding.
     * 以 float 的形式返回指定数字的值。这可能涉及四舍五入。
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code float}.
     *          转换为 float 类型后此对象表示的数值。
     */
    public abstract float floatValue();

    /**
     * Returns the value of the specified number as a {@code double},
     * which may involve rounding.
     * 以 double 的形式返回指定数字的值。这可能涉及四舍五入。
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code double}.
     *          转换为 double 类型后此对象表示的数值。
     */
    public abstract double doubleValue();

    /**
     * Returns the value of the specified number as a {@code byte},
     * which may involve rounding or truncation.
     * 以 byte 的形式返回指定数字的值。这可能涉及四舍五入或截断。
     *
     * <p>This implementation returns the result of {@link #intValue} cast
     * to a {@code byte}.
     * 该实现将 intValue() 的结果强转为一个 byte 返回。
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code byte}.
     *          转换为 byte 类型后此对象表示的数值。
     * @since   JDK1.1
     */
    public byte byteValue() {
        return (byte)intValue();
    }

    /**
     * Returns the value of the specified number as a {@code short},
     * which may involve rounding or truncation.
     * 以 short 的形式返回指定数字的值。这可能涉及四舍五入或截断。
     *
     * <p>This implementation returns the result of {@link #intValue} cast
     * to a {@code short}.
     * 该实现将 intValue() 的结果强转为一个 short 返回。
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code short}.
     *          转换为 short 类型后此对象表示的数值。
     * @since   JDK1.1
     */
    public short shortValue() {
        return (short)intValue();
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -8742448824652078965L;
}
