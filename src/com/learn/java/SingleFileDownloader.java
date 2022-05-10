package com.learn.java;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SingleFileDownloader {

    public static final String OUTPUT_FOLDER = "G:/Projects/ts/output";

    private static void download(String url) throws Exception {
        File file = new File(OUTPUT_FOLDER + "/" + "video" + ".mp4");

        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .version(HttpClient.Version.HTTP_2)
                .GET()
                .build();

        HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofFile(file.toPath()));
    }

    public static void main(String[] args) throws Exception {
    }
}
