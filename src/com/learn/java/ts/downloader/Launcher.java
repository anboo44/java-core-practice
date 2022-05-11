package com.learn.java.ts.downloader;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class Launcher {
    private final static String AUDIO_FOLDER = "G:/Projects/ts/audio";
    private final static String VIDEO_FOLDER = "G:/Projects/ts/video";
    public final static String OUTPUT_FOLDER = "G:/Projects/ts/output";

    private static String AUDIO_URL = "https://bcbolt3bf711a4-a.akamaihd.net/media/v1/hls/v4/clear/6303723056001/fedac6f2-f090-4146-a8ae-1a1ea3537003/d4f718d1-43bd-405c-99a9-1cd7158e3579/3x/segment3.ts?akamai_token=exp=1652320775~acl=/media/v1/hls/v4/clear/6303723056001/fedac6f2-f090-4146-a8ae-1a1ea3537003/d4f718d1-43bd-405c-99a9-1cd7158e3579/*~hmac=9d0058bfa501a6d057756dc05cde8f1da0f54338c3f690f58ba3a9afa8e36732";
    private static String VIDEO_URL = "https://bcbolt3bf711a4-a.akamaihd.net/media/v1/hls/v4/clear/6303723056001/fedac6f2-f090-4146-a8ae-1a1ea3537003/fa318b30-c4f3-4b4d-b4a6-95f958040f63/3x/segment2.ts?akamai_token=exp=1652320774~acl=/media/v1/hls/v4/clear/6303723056001/fedac6f2-f090-4146-a8ae-1a1ea3537003/fa318b30-c4f3-4b4d-b4a6-95f958040f63/*~hmac=66940f4c0663e8fa53705e18e1cd9fd9704d8a87d543ad7129f4d3373b79553a";

    public static void main(String[] args) {
        UrlBuilder urlBuilder = new UrlBuilderImpl();
        DownloadManager downloadManager = new DownloadManagerImpl();
        CommandBuilder commandBuilder = new CommandBuilderImpl();
        MergeManager mergeManager = new MergeManagerImpl(commandBuilder);

        var audioUrlConfig = new UrlConfig(AUDIO_URL, 0, 146);
        var videoUrlConfig = new UrlConfig(VIDEO_URL , 0, 144);

        clearData();

        var audioF = CompletableFuture
                .supplyAsync(() -> urlBuilder.build(audioUrlConfig))
                .thenCompose(urls -> downloadManager.download(urls, AUDIO_FOLDER))
                .thenApply(files -> mergeManager.merge(files, AUDIO_FOLDER));
        var videoF = CompletableFuture
                .supplyAsync(() -> urlBuilder.build(videoUrlConfig))
                .thenCompose(urls -> downloadManager.download(urls, VIDEO_FOLDER))
                .thenApply(files -> mergeManager.merge(files, VIDEO_FOLDER));
        videoF.join();

        var result = videoF.thenCombine(audioF, (video, audio) -> {
            return mergeManager.mergeAudioAndVideo(video, audio, OUTPUT_FOLDER);
        });
        result.join();
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
