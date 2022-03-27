package com.learn.java;

import java.util.ArrayList;
import java.util.function.Function;

// Functional interface
@FunctionalInterface
interface Action {
    void alert();

    default void alert2() {
        System.out.println("Default_X2");
    }

    default void alert3() {
        System.out.println("Default_X3");
    }

    default void alert4() {
        System.out.println("Default_X4");
    }
}

class ActionImpl implements Action {

    public Function<String, String> func(String v) {
        return t -> "Value = " + v + "&t = " + t;
    }

    @Override
    public void alert() {
        System.out.println("Here ActionImpl");
    }
}

class Action2 extends ActionImpl {

    @Override
    public void alert() {
        alert2();
    }
}

public class HelloWorld {

    public void alert() {
        System.out.println("==============[ Here alert ]====================");
    }

    public static void main(String[] args) {
        System.out.println("Hello World");

        //-------[ Sum ]----------------
        var array = new ArrayList<Integer>();
        array.add(1); array.add(2); array.add(3);

        var total = array.stream().mapToInt(v -> v).sum();
        System.out.println("Total = " + total);

        var totalEven = array.stream().filter(v -> v % 2 == 0).mapToInt(v -> v).sum();
        System.out.println("Total Even = " + totalEven);

        var firstEven = array.stream().filter(v -> v % 2 == 0).findFirst();
        if (firstEven.isPresent()) {
            System.out.println("Value = " + firstEven.get());
        } else {
            System.out.println("No value");
        }

        var hello = new HelloWorld();
        hello.alert();

        Action action = () -> System.out.println("Hello_X");
        action.alert();
        action.alert2();
        action.alert3();
        action.alert4();

        var a2 = new ActionImpl();
        var x = a2.func("a2hihi");
        var y = x.andThen(a2.func("after"));
        x = x.compose(a2.func("before"));

        System.out.println(x.apply("bibi"));
        System.out.println(y.apply("bibi"));
    }
}
