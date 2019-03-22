package com.geek.ngrok;

import java.util.ArrayList;
import java.util.Collection;

/*
    https://github.com/dosgo/ngrok-java
 */
public class Ngrok {

    public static void main(String args[]) throws Exception {

        //new
        NgrokClient ngclient = new NgrokClient();
        //addtunnel
        ngclient.addTun("172.20.131.130", 80, "http", "", "test1", 0, "");
        //start
        ngclient.start();

        new Ngrok().method();
    }

    void method() {

        Collection<String> strings = new ArrayList<>();
        strings.add("aaa");
        strings.add("bbb");
        strings.add("ccc");
        strings.add("aaa");
        strings.add("abb");

        strings.stream()
                .filter(s -> s.startsWith("a"))
                .distinct()
                .forEach(System.out::println);

    }
}




