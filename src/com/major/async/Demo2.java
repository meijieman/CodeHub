package com.major.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Demo2 extends BaseDemo {

    // 条件锁
    private final Lock lock = new ReentrantLock();
    private final Condition con = lock.newCondition();

    @Override
    public void callback(long response) {
        System.out.println("得到结果 " + response);
        lock.lock();
        try {
            con.signal();
        } finally {
            lock.unlock();
        }

    }

    public static void main(String[] args) {
        Demo2 demo2 = new Demo2();
        demo2.call();

        demo2.lock.lock();

        try {
            demo2.con.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            demo2.lock.unlock();
        }
        System.out.println("主线程内容");
    }
}
