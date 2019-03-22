package com.major.ioc;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://blog.csdn.net/u010889616/article/details/52739653
 */
public class Main {

    @AutoWired
    IController mController;

    @AutoWired
    DogController mDogController;


    public static void main(String[] args) {


        long start = System.currentTimeMillis();
        List<Class<?>> classesList = AnnoManageUtil.getPackageController("com.major.ioc", Controller.class);
        System.out.println("classesList " + classesList);

        AnnoManageUtil.initAutoWired("com.major.ioc");

        Map<String, ExecutorBean> mmap = new HashMap<>();
        AnnoManageUtil.getRequestMappingMethod(classesList, mmap);

        ExecutorBean bean = mmap.get("/test1");
        try {
            bean.getMethod().invoke(bean.getObject());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        System.out.println("time used: " + (System.currentTimeMillis() - start));
    }
}
