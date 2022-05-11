package com.learn.java.ts.downloader;

import java.io.File;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface DownloadManager {
    CompletableFuture<List<File>> download(List<URL> urls, String baseFolder);
}

class DownloadManagerImpl implements DownloadManager {

    private final static String POSTFIX_PATH = "temp";

    @Override
    public CompletableFuture<List<File>> download(List<URL> urls, String baseFolder) {
        System.out.println("=============[ START TO DOWNLOAD FILES ]==============");
        // Setting
        var asyncFileList = new ArrayList<CompletableFuture<File>>();
        var storageFolder = createStorageFolder(baseFolder);
        if (storageFolder == null) return CompletableFuture.supplyAsync(List::of);

        // Process
        for(int i = 0; i < urls.size(); i++) {
            var asyncFile = downloadAsync(i, urls.get(i), storageFolder);
            asyncFileList.add(asyncFile);
        }

        return sequence(asyncFileList).thenApply(files -> {
            var nullFiles = files.stream().filter(Objects::isNull).collect(Collectors.toList());
            if (nullFiles.size() > 0) return List.of();
            else                      return files;
        });
    }

    private CompletableFuture<File> downloadAsync(int index, URL url, File folder) {
        return CompletableFuture.supplyAsync(() -> download(index, url, folder));
    }

    private File download(int index, URL url, File folder) {
        try {
            File tsFile = new File(folder + "/" + index + ".ts");

            var request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .version(HttpClient.Version.HTTP_2)
                    .GET()
                    .build();

            HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofFile(tsFile.toPath()));

            System.out.println("Download single file: " + index);
            return tsFile;
        } catch (Exception e) {
            return null;
        }
    }

    private File createStorageFolder(String baseFolder) {
        try {
            var containFolder = new File(baseFolder + "/" + POSTFIX_PATH);
            if (!containFolder.isDirectory()) {
                containFolder.delete();
            }
            if (!containFolder.exists()) {
                containFolder.mkdirs();
            }

            return containFolder;
        } catch (Exception e) {
            return null;
        }
    }

    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> com) {
        return CompletableFuture.allOf(com.toArray(new CompletableFuture<?>[0]))
                .thenApply(v -> com.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }
}
