package com.learn.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Java11Features {
    public static void main(String[] args) throws IOException {
        var path = new File("hell.txt").toPath();
        var file = Files.writeString(path, "Hello_World");
        var r = Files.readString(path);
        System.out.println(r);
    }
}
