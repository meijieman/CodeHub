package com.major.async;

public class Demo1 extends BaseDemo {

    private final Object lock = new Object();

    @Override
    public void callback(long response) {
        System.out.println("得到结果=== " + response);

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public static void main(String[] args) {

        Demo1 demo1 = new Demo1();
        demo1.call();

        synchronized (demo1.lock) {
            try {
                demo1.lock.wait(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("主线程内容");

    }
}
