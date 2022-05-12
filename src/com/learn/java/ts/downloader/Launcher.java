package com.learn.java.ts.downloader;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Launcher {
    private final static String AUDIO_FOLDER = "C:/ts/audio";
    private final static String VIDEO_FOLDER = "C:/ts/video";
    public final static String OUTPUT_FOLDER = "C:/ts/output";

    private static String AUDIO_URL = "";
    private static String VIDEO_URL = "";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        UrlBuilder urlBuilder = new UrlBuilderImpl();
        DownloadManager downloadManager = new DownloadManagerImpl();
        CommandBuilder commandBuilder = new CommandBuilderImpl();
        MergeManager mergeManager = new MergeManagerImpl(commandBuilder);

        var audioUrlConfig = new UrlConfig(AUDIO_URL, 1024, 1241);
        var videoUrlConfig = new UrlConfig(VIDEO_URL , 1015, 1231);
        var threadpool = Executors.newFixedThreadPool(32);

        clearData();

        var audioF = CompletableFuture
                .supplyAsync(() -> urlBuilder.build(audioUrlConfig), threadpool)
                .thenCompose(urls -> downloadManager.download(urls, AUDIO_FOLDER))
                .thenApply(files -> mergeManager.merge(files, AUDIO_FOLDER));
        var videoF = CompletableFuture
                .supplyAsync(() -> urlBuilder.build(videoUrlConfig), threadpool)
                .thenCompose(urls -> downloadManager.download(urls, VIDEO_FOLDER))
                .thenApply(files -> mergeManager.merge(files, VIDEO_FOLDER));
        videoF.join();

        CompletableFuture.allOf(audioF, videoF).get();
        mergeManager.mergeAudioAndVideo(audioF.get(), videoF.get(), OUTPUT_FOLDER);
        System.out.println("DONE");
        threadpool.shutdown();
    }

    private static void clearData() {
        var f1 = new File(AUDIO_FOLDER);
        var f2 = new File(VIDEO_FOLDER);
        var f3 = new File(OUTPUT_FOLDER);

        List.of(f1, f2, f3).forEach(f -> Launcher.deleteDir(f, true));
    }

    private static void deleteDir(File file, boolean isFirst) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f, false);
            }
        }
        if (!isFirst) file.delete();
    }
}
