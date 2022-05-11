package com.learn.java;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AudioVideoTsDownloader {
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static int IDX_START = 0;
    public static int IDX_END = 841;
    public static int DIFF = 9;
    public static int A_IDX_START = 0;
    public static int A_IDX_END = 0;
    public static int V_IDX_START = 0;
    public static int V_IDX_END = 0;
    public static int GROUP_SIZE = 200; //200;

    private static final String AUDIO_URL = "https://bcboltbde696aa-a.akamaihd.net/media/v1/hls/v4/clear/6303911335001/135a1880-3ae7-405f-9a98-d9febd220684/2ed059b8-2a5f-4ee0-941d-7cd3a2b7128c/3x/segment1146.ts?akamai_token=exp=1652301170~acl=/media/v1/hls/v4/clear/6303911335001/135a1880-3ae7-405f-9a98-d9febd220684/2ed059b8-2a5f-4ee0-941d-7cd3a2b7128c/*~hmac=5eebb69f8faff796feb4a4298cd0e8e5c696b05b18fd72da4a2a141acdd76108";
    private static final String VIDEO_URL = "https://bcboltbde696aa-a.akamaihd.net/media/v1/hls/v4/clear/6303911335001/135a1880-3ae7-405f-9a98-d9febd220684/caf3ad99-c1b1-457a-a3ff-ed95c0b3423d/3x/segment1136.ts?akamai_token=exp=1652301165~acl=/media/v1/hls/v4/clear/6303911335001/135a1880-3ae7-405f-9a98-d9febd220684/caf3ad99-c1b1-457a-a3ff-ed95c0b3423d/*~hmac=18b39b178e172a52f87ff01f8ca0152a404d807031554a1df56bbf542fad0c9f";
    private static final String PREFIX_PATTERN = ".+segment";
    private static final String POSTFIX_PATTERN = ".ts.+";

    public static final String AUDIO_FOLDER_TEMP = "G:/Projects/ts/temp/audio/temp";
    public static final String AUDIO_FOLDER = "G:/Projects/ts/temp/audio";
    public static final String VIDEO_FOLDER_TEMP = "G:/Projects/ts/temp/video/temp";
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
        var result = download(false, new AtomicInteger(0))
            .thenCombineAsync(download(true, new AtomicInteger(0)), (s1, s2) -> {
                try {
                    mergeAudioVideo();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mergeLastTime();
                return "DONE";
            });

        result.get();
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

    private void mergeAudioVideo() throws ExecutionException, InterruptedException {
        System.out.println("===========[ Start to merge Audio & Video files ]=======================");

        var futureList = new ArrayList<CompletableFuture<Void>>();
        var futureAudioList = new ArrayList<CompletableFuture<Void>>();
        var futureVideoList = new ArrayList<CompletableFuture<Void>>();

//        var commands = buildMergeAudioVideoCommand();
        var commands = List.of("");
//        var audioCommands = buildMergeAudioVideoCommand();
//        var videoCommands = buildMergeAudioVideoCommand();


        for (int i = 0; i < commands.size(); i++) {
            var command = commands.get(i);
            var result = CompletableFuture.runAsync(() -> {
                try {
                    runCommands(List.of(command));
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            });
            futureList.add(result);
        }

        var result = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        result.get();
        System.out.println("===========[ Finish to merge Audio & Video files ]=======================");
    }

    private void runCommands(List<String> commands) {
        commands.forEach(command -> {
            try {
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.redirectErrorStream(true);
                Process p = builder.start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while (true) {
                    line = r.readLine();
                    if (line == null) break;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private List<String> buildMergeAudioVideoCommand(boolean isAudio) {
        var start = isAudio ? A_IDX_START: V_IDX_START;
        var end = isAudio ? A_IDX_END: V_IDX_END;

        List<String> commandList = new ArrayList<>();
        for (int i = 0; i < (end - start); i++) {
            var command = String.format("ffmpeg -i %s/%d.ts -i %s/%d.ts -c:v copy -c:a aac %s/%d.ts", AUDIO_FOLDER, i, VIDEO_FOLDER, i, TEMP_FOLDER, i);
            commandList.add(command);
        }
        return commandList;
    }

    private CompletableFuture<Void> download(boolean isAudio, AtomicInteger counter) throws ExecutionException, InterruptedException {
        String baseUrl = "";
        String storedFolder = "";
        if (isAudio) {
            baseUrl = AUDIO_URL;
            storedFolder = AUDIO_FOLDER;
        } else {
            baseUrl = VIDEO_URL;
            storedFolder = VIDEO_FOLDER;
        }

        var urls = getUrlList(baseUrl, isAudio);
        return download(urls, storedFolder, counter);
    }

    private CompletableFuture<Void> download(List<String> urls, String storedFolder, AtomicInteger counter) throws ExecutionException, InterruptedException {
        var futureList = new ArrayList<CompletableFuture<Void>>();

        for(int i = 0; i < urls.size(); i++) {
            int idx    = i;
            String url = urls.get(i);

            var result = CompletableFuture.runAsync(() -> {
                try {
                    download(url, idx, storedFolder);
                    System.out.println("=====[ Counter: " + counter.getAndIncrement() + " ]======");
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            });
            futureList.add(result);
        }

        var result = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        return result.thenRun(() -> {
            System.out.printf("======[ All files are stored at: %s ]=====================\n", storedFolder);
        });
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
        var audioFolderTemp = new File(AUDIO_FOLDER_TEMP);
        var videoFolder = new File(VIDEO_FOLDER);
        var videoFolderTemp = new File(VIDEO_FOLDER_TEMP);
        var tempFolder = new File(TEMP_FOLDER);
        var outputFolderTemp = new File(OUTPUT_TEMP_FOLDER);

        var folders = List.of(audioFolder, videoFolder, tempFolder, outputFolderTemp, audioFolderTemp, videoFolderTemp);
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

    private List<String> getUrlList(String baseUrl, boolean isAudio) {
        var urls = new ArrayList<String>();
        int start = isAudio ? A_IDX_START: V_IDX_START;
        int end = isAudio ? A_IDX_END: V_IDX_END;

        for(int i = start; i <= end; i++) {
            String url = buildUrlWithIdx(baseUrl, i);
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
