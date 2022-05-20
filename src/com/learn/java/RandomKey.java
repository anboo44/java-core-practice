package com.learn.java;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class RandomKey {

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    static String randomString(int len){
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    static String getSaltString(int length) {
        var rd = new Random();
        StringBuilder salt = new StringBuilder();
        while (salt.length() < length) { // length of the random string.
            int index = (int) (rd.nextFloat() * AB.length());
            salt.append(AB.charAt(index));
        }

        return salt.toString();
    }

    public static void main(String[] args) {
        List<String> x = new ArrayList<>();
        for(long i = 0; i <= 1000_000; i++) {
            x.add(randomString(6));
        }
        var y = new HashSet<>(x).size();
        System.out.println("SIZE: " + x.size());
        System.out.println("AFter: " + (y - x.size()));
    }
}
