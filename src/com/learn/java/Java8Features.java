package com.learn.java;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@FunctionalInterface
interface Sayable {
    String say();
}

public class Java8Features {

    public void execute(Sayable say) {
        System.out.println("Sayable: " + say.say());
    }

    public static void main(String[] args) {
        System.out.println("=================/ Lambda Expression /=================");
        Sayable teller = () -> "=====[ Hello Lambda ]========";
        System.out.println(teller.say());

        var array = new int[] { 1, 2, 3 };
        Consumer<Integer> printer = t -> System.out.print(t + ": ");
        Arrays.stream(array).forEach(v -> System.out.print(v + ", "));
        Arrays.stream(array).forEach(printer::accept);

        var neededSort = Arrays.asList(1, 2, 5, 0, 3);
        neededSort.sort((t1, t2) -> t2 - t1);
        System.out.println();
        neededSort.forEach(v -> System.out.print(v + ", "));

        System.out.println();
        var stringSort = Arrays.asList("b", "a", "d", "c", "a");
        stringSort.forEach(v -> System.out.print(v + ", "));
        System.out.println();
        stringSort.stream().distinct().forEach(v -> System.out.print(v + ", "));
        System.out.println("\n");
    }
}
