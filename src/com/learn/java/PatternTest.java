package com.learn.java;

import java.util.regex.Pattern;

public class PatternTest {
    public static void main(String[] args) {
        var p = Pattern.compile("(\\w+)ASC$|(\\w+)DES$");
        System.out.println(p.matcher(null).find());
    }
}
