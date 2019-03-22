package com.major.ioc;

@Controller
public class DogController implements IController {

    @RequestMapping(value = "/test3")
    public void test3() {
        System.out.println("DogController->test3()");
    }

    @RequestMapping(value = "/test4")
    public void test4() {
        System.out.println("DogController->test4()");
    }

    @Override
    public void eat() {
        System.out.println("狗吃肉");
    }
}
