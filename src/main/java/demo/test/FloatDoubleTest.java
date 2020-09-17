package demo.test;

import sun.misc.FloatConsts;

import java.math.BigDecimal;

//浮点数运算有误差，浮点数的四则运算不具备结合律和分配率
public class FloatDoubleTest {

    public static void main(String[] args) {
        FloatDoubleTest floatDoubleTest = new FloatDoubleTest();
//        test.test2();
//        System.out.println("---------");
//        test.test4();
//        System.out.println("---------");
//        test.test3();
        floatDoubleTest.floatProperty();
        floatDoubleTest.floatMethods();
    }

    private void floatMethods() {
        // floatToRawIntBits
        System.out.println(Integer.toBinaryString(Float.floatToIntBits(1.0f)));
        System.out.println(Integer.toBinaryString(Float.floatToIntBits(-1.0f)));
        System.out.println(Float.floatToRawIntBits(1.0f));
        System.out.println(Math.pow(2, 23) + Math.pow(2, 24) + Math.pow(2, 25) + Math.pow(2, 26) + Math.pow(2, 27) + Math.pow(2, 28) + Math.pow(2, 29));
        System.out.println((int) (Math.pow(2, 23) + Math.pow(2, 24) + Math.pow(2, 25) + Math.pow(2, 26) + Math.pow(2, 27) + Math.pow(2, 28) + Math.pow(2, 29)));
        System.out.println(Float.floatToRawIntBits(-1.0f));
        System.out.println("========== 3212836864");
        System.out.println("==========Integer.MAX_VALUE: 【" + Integer.MAX_VALUE + "】 , Integer.MIN_VALUE: 【" + Integer.MIN_VALUE + "】 = Integer.MAX_VALUE + 1");
        System.out.println("========== " + (Integer.MIN_VALUE + (3212836864L - Integer.MAX_VALUE) - 1));
        // floatToIntBits   &: 1 & 1 = 1 0 & 1 = 0 1 & 0 = 0
        System.out.println(Float.floatToIntBits(Float.NaN));
        System.out.println("Float.NaN: 0 11111111 10000000000000000000000");
        System.out.println(FloatConsts.EXP_BIT_MASK + " => 0x7F800000 => 0 11111111 00000000000000000000000");
        System.out.println(FloatConsts.SIGNIF_BIT_MASK + " => 0x007FFFFF => 0 00000000 11111111111111111111111");
        // intBitsToFloat
        System.out.println(Float.intBitsToFloat(Float.floatToRawIntBits(10.789f)));
        System.out.println(Float.intBitsToFloat(Float.floatToRawIntBits((float) Math.sqrt(-1.0d))));
        System.out.println(0x7f800000);
        // compare
    }

    private void floatProperty() {
        // 0 11111111 00000000000000000000000
        System.out.println("正无穷: " + Float.POSITIVE_INFINITY);
        // 1 11111111 00000000000000000000000
        System.out.println("负无穷: " + Float.NEGATIVE_INFINITY);
        // 0 11111111 10000000000000000000000
        System.out.println("NaN: " + Float.NaN);
        // 0 11111110 11111111111111111111111
        // (1.11111111111111111111111)₂ * 2¹²⁷ = (2 - 2 ^ -23)₁₀ * 2¹²⁷
        System.out.println("max: " + Float.MAX_VALUE);
        // 0 00000001 00000000000000000000000
        System.out.println("最小正标准值: 2 ^ -126 = " + Float.MIN_NORMAL);
        // 0 00000000 00000000000000000000001
        // 0.00000000000000000000001 * 2 ^ -126 = 1 * 2 ^ -(126 + 23) = 2 ^ -149
        System.out.println("min: 2 ^ -149 = " + Float.MIN_VALUE);
        System.out.println("最大指数: " + Float.MAX_EXPONENT);
        System.out.println("最小指数: " + Float.MIN_EXPONENT);
        System.out.println("位数: " + Float.SIZE);
        System.out.println("字节数: 32(位数) / 8 = " + Float.BYTES);
    }

    private void doubleProperty() {
        System.out.println("正无穷: " + Double.POSITIVE_INFINITY);
        System.out.println("负无穷: " + Double.NEGATIVE_INFINITY);
        System.out.println("NaN: " + Double.NaN);
        System.out.println("max: " + Double.MAX_VALUE);
        System.out.println("最小正标准值: 2 ^ -1022 = " + Double.MIN_NORMAL);
        // 1 * 2 ^ -(1022 + 52) = 2 ^ -1074
        System.out.println("min: 2 ^ -1074 = " + Double.MIN_VALUE);
        System.out.println("最大指数: " + Double.MAX_EXPONENT);
        System.out.println("最小指数: " + Double.MIN_EXPONENT);
        System.out.println("位数: " + Double.SIZE);
        System.out.println("字节数: 64(位数) / 8 = " + Double.BYTES);
    }

    private void floatConvert() {
        printZs(new BigDecimal(Double.toString(0.9999999)));
        printZs(new BigDecimal(Double.toString(0.99999999)));
        printZs(new BigDecimal(Double.toString(0.8888899))); // 25 1
        System.out.println(Integer.toBinaryString(Float.floatToIntBits(1.0f))); // 1.0 * 2 ^ 0
        System.out.println(Integer.toBinaryString(Float.floatToIntBits(0.99999999f))); // 1.1...1 * 2 ^ -1 = 10.00... * 2 ^ -1 = 1.00... * 2 ^ 0
        System.out.println(Float.intBitsToFloat(Integer.valueOf("0 01111111 11100110011001100110100".replace(" ", ""), 2)));
        System.out.println(Integer.toBinaryString(127));
        System.out.println(Integer.valueOf("1111111",2));
    }

    private static void printZs(BigDecimal b) {
        BigDecimal bd = b;
        for (int i = 1; i < 26; i++) {
            bd = bd.multiply(new BigDecimal(2));
            System.out.println(i + " : " + bd.toString().substring(0, bd.toString().indexOf(".")));
            if (bd.compareTo(new BigDecimal(1)) > 0) {
                bd = bd.subtract(new BigDecimal(1));
            }
        }
    }

    private void test1() {
        // 条件判断超预期
        System.out.println(1f == 0.9999999f); // false
        System.out.println(1f == 0.99999999f); // true ?
        // 数据转换超预期
        float f = 1.1f;
        double d = f;
        System.out.println(f); // 1.1
        System.out.println(d); // 1.100000023841858 ?
        // 基本运算超预期
        System.out.println(0.2 + 0.7); // 0.8999999999999999 ?
        // 数据自增超预期
        float f1 = 8455263f;
        for (int i = 0; i < 10; i++) {
            System.out.println(f1);
            f1++;
        }
        // 8455263.0
        // 8455264.0
        // 8455265.0
        // 8455266.0
        // 8455267.0
        // 8455268.0
        // 8455269.0
        // 8455270.0
        // 8455271.0
        // 8455272.0
        float f2 = 84552631f;
        for (int i = 0; i < 10; i++) {
            System.out.println(f2);
            f2++;
        }
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？
        //    8.4552632E7   不是 +1了吗？




        int c = (int) (0.58 * 100);
        System.out.println(c);
        int x = (int) (0.58f * 100.0f);
        System.out.println(x);
    }
}
