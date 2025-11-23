package dev.scx.exception.test;

import dev.scx.exception.ScxWrappedException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class ScxWrappedExceptionTest {

    public static void main(String[] args) {
        test1();
        test2();
        test3();
    }

    public static void read(Func<byte[], Exception> bytesConsumer, int length) throws ScxWrappedException, IOException {
        if (length > 2048) {
            throw new IOException("length too big");
        }
        try {
            bytesConsumer.apply(new byte[]{1, 2, 3});
        } catch (Exception e) {
            throw new ScxWrappedException(e);
        }
    }

    @Test
    public static void test1() {
        try {
            read(bytes -> {
                throw new IOException("lambda error");
            }, 4096);
        } catch (IOException e) {
            Assert.assertEquals(e.getMessage(), "length too big");
        }
    }

    @Test
    public static void test2() {
        // 我们能很好的区分 异常来源
        try {
            read(bytes -> {
                throw new IOException("lambda error");
            }, 1024);
        } catch (IOException e) {
            Assert.fail();
        } catch (ScxWrappedException e) {
            Assert.assertEquals(e.getRootCause().getMessage(), "lambda error");
        }
    }

    @Test
    public static void test3() {
        // 多层包装
        try {
            read(bytes -> {
                read(bytes2 -> {
                    throw new IOException("lambda error");
                }, 1024);
            }, 1024);
        } catch (IOException e) {
            Assert.fail();
        } catch (ScxWrappedException e) {
            Assert.assertEquals(e.getRootCause().getMessage(), "lambda error");
        }
    }

    @FunctionalInterface
    public interface Func<A, X extends Throwable> {

        void apply(A a) throws X;

    }

}
