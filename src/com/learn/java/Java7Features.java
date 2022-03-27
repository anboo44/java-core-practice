package com.learn.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.stream.Stream;

public class Java7Features implements AutoCloseable {
    public static void main(String[] args) {
        //==========/ Multi-cast Exception /===========
        try {
            var x = 1 / 2;
        } catch (NumberFormatException | NullPointerException ex) {
            System.out.println(ex.getMessage());
        }

        //===========/ Try with resource /===================
        try (BufferedReader reader = new BufferedReader(new FileReader(new File("path")))) {
            System.out.println("");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try (Java7Features x = new Java7Features()) {
            System.out.println(x.toString());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        var result = Stream.of("Pham", "The", "Hung").reduce("", (x, y) -> x + " " + y).trim();
        System.out.println(result);
    }

    @Override
    public void close() {
        System.out.println("Custom finally of try..with..resource");
    }
}
