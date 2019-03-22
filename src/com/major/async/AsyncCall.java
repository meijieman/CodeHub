package com.major.async;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 参考
 * 5种必会的Java异步调用转同步的方法你会几种 https://blog.csdn.net/m0_37595562/article/details/81638445
 */
public class AsyncCall {

    private Random random = new Random(System.currentTimeMillis());
    private ExecutorService tp = Executors.newSingleThreadExecutor();

    //demo1,2,4,5调用方法
    public void call(BaseDemo demo) {

        new Thread(() -> {
            long res = random.nextInt(5);
            System.out.println("睡眠 " + res + " s");
            try {
                Thread.sleep(res * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            demo.callback(res);
        }).start();


    }

    //demo3调用方法
    public Future<Long> futureCall() {

        return tp.submit(() -> {
            long res = random.nextInt(10);

            try {
                Thread.sleep(res * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return res;
        });

    }

    public void shutdown() {
        tp.shutdown();

    }
}
