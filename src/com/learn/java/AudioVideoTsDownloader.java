package com.learn.java;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AudioVideoTsDownloader {
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static int IDX_START = 1;
    public static int IDX_END = 4; //1143;
    public static int GROUP_SIZE = 2; //200;

    private static final String AUDIO_URL = "";
    private static final String VIDEO_URL = "";
    private static final String PREFIX_PATTERN = ".+segment";
    private static final String POSTFIX_PATTERN = ".ts.+";

    public static final String AUDIO_FOLDER = "G:/Projects/ts/temp/audio";
    public static final String VIDEO_FOLDER = "G:/Projects/ts/temp/video";
    public static final String TEMP_FOLDER = "G:/Projects/ts/temp";
    public static final String OUTPUT_TEMP_FOLDER = "G:/Projects/ts/output/temp";
    public static final String OUTPUT_FOLDER = "G:/Projects/ts/output";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var downloader = new AudioVideoTsDownloader();
        downloader.execute();
    }

    private void execute() throws ExecutionException, InterruptedException {
        prepareStorageFolder();
        download(true);
        download(false);
        mergeAudioVideo();
        mergeLastTime();
    }

    private void mergeLastTime() {
        System.out.println("===========[ Start to merge final File ]=======================");

        mergeVideo(new File(TEMP_FOLDER), new File(OUTPUT_TEMP_FOLDER));
        mergeVideo(new File(OUTPUT_TEMP_FOLDER), new File(OUTPUT_FOLDER));

        System.out.println("===========[ Finish to merge final File ]=======================");
    }

    private void mergeVideo(File sourceFolder, File outputFolder) {
        // Get files in output folder and then order by fileName asc
        var allFiles = Stream.of(sourceFolder.listFiles())
                .filter(File::isFile)
                .sorted((f1, f2) -> {
                    var f1Name = f1.getName();
                    var f2Name = f2.getName();

                    var f1Idx = Integer.parseInt(f1Name.split("\\.")[0]);
                    var f2Idx = Integer.parseInt(f2Name.split("\\.")[0]);

                    return  f1Idx - f2Idx;
                })
                .collect(Collectors.toList());

        if (allFiles.size() == 1) return;

        StringBuilder allPath = new StringBuilder();
        List<String> mergedPaths = new ArrayList<>();
        int runner = 0;

        // concat by "|" per 200 files
        for(int i = 0; i < allFiles.size(); i++) {
            var filePath = allFiles.get(i).getPath();
            allPath.append(filePath).append("|");
            runner ++;

            var isResetPath = (runner == GROUP_SIZE) || (i == allFiles.size() - 1);
            if (isResetPath) {
                mergedPaths.add(allPath.substring(0, allPath.length() - 1));
                allPath = new StringBuilder();
                runner  = 0;
            }
        }

        List<String> commands = new ArrayList<>();
        for(int i = 0; i < mergedPaths.size(); i++) {
            commands.add(
                    "ffmpeg -i \"concat:" + mergedPaths.get(i) + "\" -c copy \"" + outputFolder.getPath() + "\\" + i + ".ts\""
            );
        }
        runCommands(commands);
    }

    private void mergeAudioVideo() {
        System.out.println("===========[ Start to merge Audio & Video files ]=======================");

        var commands = buildMergeAudioVideoCommand();
        runCommands(commands);

        System.out.println("===========[ Finish to merge Audio & Video files ]=======================");
    }

    private void runCommands(List<String> commands) {
        commands.forEach(command -> {
            try {
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.redirectErrorStream(true);
                builder.start();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private List<String> buildMergeAudioVideoCommand() {
        List<String> commandList = new ArrayList<>();
        for (int i = IDX_START; i <= IDX_END; i++) {
            var command = String.format("ffmpeg -i %s/%d.ts -i %s/%d.ts -c:v copy -c:a aac %s/%d.ts", AUDIO_FOLDER, i, VIDEO_FOLDER, i, TEMP_FOLDER, i);
            commandList.add(command);
        }
        return commandList;
    }

    private void download(boolean isAudio) throws ExecutionException, InterruptedException {
        String baseUrl = "";
        String storedFolder = "";
        if (isAudio) {
            baseUrl = AUDIO_URL;
            storedFolder = AUDIO_FOLDER;
        } else {
            baseUrl = VIDEO_URL;
            storedFolder = VIDEO_FOLDER;
        }

        var urls = getUrlList(baseUrl);
        download(urls, storedFolder);
    }

    private void download(List<String> urls, String storedFolder) throws ExecutionException, InterruptedException {
        var futureList = new ArrayList<CompletableFuture<Void>>();

        for(int i = 0; i < urls.size(); i++) {
            int idx    = i + 1;
            String url = urls.get(i);

            var result = CompletableFuture.runAsync(() -> {
                try {
                    download(url, idx, storedFolder);
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            });
            futureList.add(result);
        }

        var result = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        result.get();
        System.out.printf("======[ All files are stored at: %s ]=====================\n", storedFolder);
    }

    private void download(String url, int idx, String storedFolder) throws Exception {
        File file = new File(storedFolder + "/" + idx + ".ts");

        URL tsUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) tsUrl.openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = input.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    private void prepareStorageFolder() {
        var audioFolder = new File(AUDIO_FOLDER);
        var videoFolder = new File(VIDEO_FOLDER);
        var tempFolder = new File(TEMP_FOLDER);
        var outputFolderTemp = new File(OUTPUT_TEMP_FOLDER);

        var folders = List.of(audioFolder, videoFolder, tempFolder, outputFolderTemp);
        folders.forEach(folder -> {
            if (!folder.exists()) {
                folder.mkdirs();
            } else {
                var insideFiles = List.of(folder.listFiles());
                insideFiles.forEach(file -> {
                    if (file.exists() && file.isFile()) {
                        file.delete();
                    }
                });
            }
        });
    }

    private List<String> getUrlList(String baseUrl) {
        var urls = new ArrayList<String>();
        for(int i = IDX_START; i <= IDX_END; i++) {
            var url = buildUrlWithIdx(baseUrl, i);
            urls.add(url);
        }

        return urls;
    }

    private String buildUrlWithIdx(String url, int idx) {
        String firstPath = null;
        String secondPath = null;

        var regex = Pattern.compile(PREFIX_PATTERN);
        var matcher = regex.matcher(url);
        if (matcher.find()) {
            firstPath = url.substring(matcher.start(), matcher.end());
        }
        matcher = Pattern.compile(POSTFIX_PATTERN).matcher(url);
        if (matcher.find()) {
            secondPath = url.substring(matcher.start(), matcher.end());
        }

        return firstPath + idx + secondPath;
    }
}
