package im.logi.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * @author logi
 * @version 1.0
 * @time 2022/1/5 16:08
 */
public class BigInteger implements Comparable<BigInteger>, Cloneable {
    private static final BigInteger ZERO = new BigInteger("0");
    private static final BigInteger ONE = new BigInteger("1");

    private boolean positive = true;
    private StringBuilder value;

    private BigInteger() {
        value = new StringBuilder();
    }

    public BigInteger(String valueString) {
        int beginIndex = 0;
        if (valueString.charAt(0) == '-') {
            beginIndex = 1;
            positive = false;
        } else if (valueString.charAt(0) == '+') beginIndex = 1;
        while (beginIndex < valueString.length()
                && valueString.charAt(beginIndex) == '0') beginIndex++;

        // 仅是一个符号或全部为零，回退
        if (beginIndex == valueString.length()) beginIndex--;

        value = new StringBuilder(valueString.length() - beginIndex);
        for (int i = beginIndex; i < valueString.length(); i++) {
            char c = valueString.charAt(i);
            if (c < '0' || c > '9') throw new IllegalArgumentException();
            value.append(c);
        }

        setZeroPositive();
    }

    // 只存正零
    private void setZeroPositive() {
        if (value.length() == 1 && value.charAt(0) == '0')
            positive = true;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        BigInteger clone = (BigInteger) super.clone();
        clone.positive = positive;
        clone.value = new StringBuilder(value);
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BigInteger o)
            return positive == o.positive
                    && value.toString().equals(o.value.toString());

        return false;
    }

    @Override
    public int compareTo(BigInteger o) {
        if (positive != o.positive) return positive ? 1 : -1;

        int absRet = value.length() - o.value.length();
        if (absRet != 0) return positive ? absRet : -absRet;

        for (int i = 0; i < value.length(); i++) {
            absRet = get(i) - o.get(i);
            if (absRet != 0) break;
        }

        return positive ? absRet : -absRet;
    }

    @Override
    public String toString() {
        return (positive ? "" : "-") + value;
    }

    // 相反数，拷贝返回
    public BigInteger negative() {
        BigInteger negative;
        try {
            negative = (BigInteger) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }
        negative.positive = !positive;
        negative.setZeroPositive();
        return negative;
    }

    // 绝对值，拷贝返回
    public BigInteger absolute() {
        BigInteger absolute = negative();
        absolute.positive = true;
        return absolute;
    }

    private int get(int index) {
        return value.charAt(index) - '0';
    }

    private void set(int index, int place) {
        value.setCharAt(index, (char) (place + '0'));
    }

    // 去除前导零，时间复杂度 O(n)
    private void trimZero() {
        int i = 0;
        // 唯一零不处理
        //noinspection StatementWithEmptyBody
        for (; i < value.length() - 1 && get(i) == 0; i++) ;
        value.delete(0, i);
    }

    // 加
    public BigInteger add(BigInteger second) {
        if (positive != second.positive) {
            return positive
                    ? subtract(second.negative())
                    : second.subtract(negative());
        }

        // 以下为同号情况
        BigInteger sum = new BigInteger();
        sum.positive = positive;

        BigInteger longer = this;
        BigInteger shorter = second;
        if (value.length() < second.value.length()) {
            longer = second;
            shorter = this;
        }

        // 多开一位
        for (int i = 0; i <= longer.value.length(); i++) sum.value.append(0);

        int i = longer.value.length() - 1;
        int j = shorter.value.length() - 1;
        for (; j >= 0; i--, j--) {
            int k = i + 1;
            int s = longer.get(i) + shorter.get(j) + sum.get(k);
            sum.set(k, s % 10);
            sum.set(i, s / 10);
        }
        for (; i >= 0; i--) {
            int k = i + 1;
            int s = longer.get(i) + sum.get(k);
            sum.set(k, s % 10);
            sum.set(i, s / 10);
        }

        sum.trimZero();
        return sum;
    }

    // 减
    public BigInteger subtract(BigInteger second) {
        if (equals(second)) return ZERO;

        if (positive != second.positive) {
            return positive
                    ? add(second.negative())
                    : second.add(negative()).negative();
        }

        BigInteger abs = absolute();
        BigInteger secAbs = second.absolute();
        if (abs.compareTo(secAbs) < 0) {
            BigInteger diff = secAbs.subtract(abs);
            diff.positive = !positive;
            return diff;
        } else if (!positive) {
            BigInteger diff = abs.subtract(secAbs);
            diff.positive = false;
            return diff;
        }

        // 以下为全正且 this > second 的情况
        BigInteger diff = new BigInteger();
        if (value.length() == 1) {
            diff.value.append(get(0) - second.get(0));
            return diff;
        }

        // 多开一位
        diff.value.append(0);
        diff.value.append((char) ('0' - 1));
        for (int i = 2; i < value.length(); i++) diff.value.append(9);
        diff.value.append((char) ('9' + 1));

        int i = value.length() - 1;
        int j = second.value.length() - 1;
        for (; j >= 0; i--, j--) {
            int k = i + 1;
            int d = get(i) - second.get(j) + diff.get(k);
            diff.set(k, d % 10);
            diff.set(i, d / 10 + diff.get(i));
        }
        for (; i >= 0; i--) {
            int k = i + 1;
            int d = get(i) + diff.get(k);
            diff.set(k, d % 10);
            diff.set(i, d / 10 + diff.get(i));
        }

        diff.trimZero();
        return diff;
    }

    // 乘
    public BigInteger multiply(BigInteger second) {
        if (equals(ZERO) || second.equals(ZERO)) return ZERO;

        BigInteger oneMul = null;
        if (absolute().equals(ONE)) oneMul = second;
        else if (second.absolute().equals(ONE)) oneMul = this;
        if (oneMul != null)
            return positive == second.positive
                    ? oneMul.absolute()
                    : oneMul.absolute().negative();


        BigInteger product = new BigInteger();
        product.positive = positive == second.positive;

        for (int i = 0; i < value.length() + second.value.length(); i++)
            product.value.append(0);

        for (int i = value.length() - 1; i >= 0; i--) {
            for (int j = second.value.length() - 1; j >= 0; j--) {
                int h = i + j;
                int l = h + 1;
                int p = get(i) * second.get(j) + product.get(l);

                product.set(l, p % 10);
                product.set(h, p / 10 + product.get(h));
            }
        }

        product.trimZero();
        return product;
    }

    // 除
    public BigInteger divide(BigInteger second) {
        if (second.equals(ZERO)) throw new ArithmeticException();
//        if (equals(ZERO)) return ZERO;
        if (absolute().compareTo(second.absolute()) < 0) return ZERO;
        if (second.absolute().equals(ONE))
            return positive == second.positive
                    ? absolute()
                    : absolute().negative();
        if (absolute().equals(second.absolute()))
            return positive == second.positive
                    ? ONE
                    : ONE.negative();
        if (positive != second.positive)
            return absolute().divide(second.absolute()).negative();
        if (!positive)
            return absolute().divide(second.absolute());

        // 以下为全正且 this > second 的情况
        BigInteger quotient = new BigInteger();
        quotient.positive = true;

        BigInteger dividend = this;
        outer:
        while (true) {
            BigInteger diff = new BigInteger();
            int i = 0;
            // this > second，总能取到 diff > second，不会越界
            do {
                diff.value.append(dividend.get(i++));
            } while (diff.compareTo(second) < 0);

            // diff >= second 首次成立, 商至少为 1 且不大于 9
            int c = 1;
            //noinspection StatementWithEmptyBody
            for (; (diff = diff.subtract(second)).compareTo(second) >= 0; c++) ;
            quotient.value.append(c);

            if (i >= dividend.value.length()) break;

            BigInteger rest = new BigInteger();
            if (diff.equals(ZERO)) {
                // 除尽，检查后续若干零
                while (dividend.get(i++) == 0) {
                    quotient.value.append(0);
                    if (i >= dividend.value.length()) {
                        break outer;
                    }
                }
            } else {
                // 未除尽，余数作下一轮被除数前缀
                rest.value.append(diff.value);
            }

            // 若必要，商补若干零
            do {
                rest.value.append(dividend.get(i++));
                if (rest.compareTo(second) < 0) quotient.value.append(0);
                else break;
                if (i >= dividend.value.length()) break outer;
            } while (true);

            rest.value.append(dividend.value.substring(i));

            if (rest.compareTo(second) < 0) break;

            dividend = rest;
        }

        return quotient;
    }

    public static void testOperation(
            BiFunction<BigInteger, BigInteger, BigInteger> testedOperation,
            BiFunction<java.math.BigInteger, java.math.BigInteger,
                    java.math.BigInteger> standardOperation,
            char operator,
            String a,
            String b
    ) {
        Exception testedException = null;
        BigInteger testedResult = null;
        try {
            testedResult = testedOperation.apply(new BigInteger(a),
                    new BigInteger(b));
        } catch (Exception e) {
            testedException = e;
        }

        Exception expectedException = null;
        java.math.BigInteger expectedResult = null;
        try {
            expectedResult = standardOperation.apply(new java.math.BigInteger(a),
                    new java.math.BigInteger(b));
        } catch (Exception e) {
            expectedException = e;
        }

        if (expectedException != null && testedException != null) return;

        if (expectedException != null) {
            System.out.printf("The result is wrong: %s %s %s = %s, got %s\n",
                    a, operator, b, expectedException, testedResult);
            throw new RuntimeException();
        }

        if (testedException != null) {
            System.out.printf("The result is wrong: %s %s %s = %s, got %s\n",
                    a, operator, b, expectedResult, testedException);
            throw new RuntimeException();
        }

        if (!testedResult.toString().equals(expectedResult.toString())) {
            System.out.printf("The result is wrong: %s %s %s = %s, got %s\n",
                    a, operator, b, expectedResult, testedResult);
            throw new RuntimeException();
        }
    }

    public static void testOne(List<String> pair) {
        testOperation(BigInteger::add, java.math.BigInteger::add,
                '+', pair.get(0), pair.get(1));
        testOperation(BigInteger::subtract, java.math.BigInteger::subtract,
                '-', pair.get(0), pair.get(1));
        testOperation(BigInteger::multiply, java.math.BigInteger::multiply,
                'x', pair.get(0), pair.get(1));
        testOperation(BigInteger::divide, java.math.BigInteger::divide,
                '/', pair.get(0), pair.get(1));
    }

    public static void testAll(List<String> pair) {
        List<List<String>> cases = List.of(
                List.of(pair.get(0), pair.get(1)),
                List.of(pair.get(0), "-" + pair.get(1)),
                List.of("-" + pair.get(0), pair.get(1)),
                List.of("-" + pair.get(0), "-" + pair.get(1))
        );
        cases.forEach(BigInteger::testOne);

        if (pair.get(0).equals(pair.get(1))) return;

        cases.stream().map(p -> List.of(p.get(1), p.get(0)))
                .forEach(BigInteger::testOne);
    }

    public static String generateRandomBigInteger(int spaceBound) {
        spaceBound = Math.max(spaceBound, 1001);
        Random r = new Random();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < r.nextInt(spaceBound - 1000, spaceBound); i++) {
            s.append(r.nextInt(0, 10));
        }
        return s.toString();
    }

    public static void main(String[] args) {
        // If no exception thrown during execution, test will pass.
        int spaceBound = 10000;
        List<List<String>> cases = new ArrayList<>(List.of(
                List.of("0", "0"),
                List.of("0", "34291743"),
                List.of("1", "1"),
                List.of("1", "3247091"),
                List.of("7", "392"),
                List.of("000007", "392"),
                List.of("7", "00000392"),
                List.of("0007", "00000392"),
                List.of("0007", "sa23bcd"),
                List.of("---3427190", "sa23bcd"),
                List.of("++34721", "32147"),
                List.of("432134", "842097")
        ));
        IntStream.range(0, 50).forEach(_ignored -> cases.add(
                List.of(generateRandomBigInteger(spaceBound),
                        generateRandomBigInteger(spaceBound))));
        cases.forEach(BigInteger::testAll);
    }
}
