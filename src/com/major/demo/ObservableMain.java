package com.major.demo;

import java.util.Observable;
import java.util.Observer;

public class ObservableMain {

    public static void main(String[] args) {
        method();
    }

    private static void method() {
        Publish observable = new Publish();

        Observer observer = (o, arg) -> System.out.println("observer 收到了消息 " + arg);
        Observer observer2 = (o, arg) -> System.out.println("observer2 收到了消息 " + arg);
        observable.addObserver(observer);
        observable.addObserver(observer2);
        // 后添加的先通知

        observable.publish("过年啦");
        observable.publish();
    }
}

class Publish extends Observable {

    public void publish() {
        // 必须要调用这个
        setChanged();
        notifyObservers();
    }

    public void publish(String msg) {
        // 必须要调用这个
        setChanged();
        notifyObservers(msg);
    }
}


