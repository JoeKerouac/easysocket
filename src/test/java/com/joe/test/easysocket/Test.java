package com.joe.test.easysocket;

import com.joe.concurrent.ThreadUtil;

/**
 * @author joe
 */
public class Test {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {

            while (true) {
                System.out.println(123);
                try {
                    ThreadUtil.sleep(2);
                } catch (Exception e) {
                    System.out.println("中断了");
                    throw e;
                }
            }
        });
        thread.start();
        ThreadUtil.sleep(2);
        System.out.println(thread.isAlive());
        System.out.println(thread.isInterrupted());
        thread.interrupt();
        ThreadUtil.sleep(3);
        System.out.println(thread.isAlive());
        System.out.println(thread.isInterrupted());

    }
}
