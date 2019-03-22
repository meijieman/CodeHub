package com.major.ioc;

@Controller
public class CatController implements IController {

    @RequestMapping(value = "/test1")
    public void test1() {
        System.out.println("CatController->test1()");
    }

    @RequestMapping(value = "/test2")
    public void test2() {
        System.out.println("CatController->test2()");
    }

    @Override
    public void eat() {
        System.out.println("猫吃鱼");
    }
}
