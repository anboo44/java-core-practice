package com.learn.java;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * =========================================
 * Download .ts video files for only Window.
 * It needs FFmpeg to merge .ts files
 * =========================================
 * Default of `CompletableFuture` use ForkJoinPool
 * It's a parallelism level and determined by your system settings or based on your current amount of processors.
 * If core size = 1, a new Thread is created to run each task
 * =========================================
 * Custom pool-size by set execution context to runAsync(runnable, executor) or supplyAsync(runnable, executor)
 * var executor = Executors.newFixedThreadPool(8)
 */
public class VideoTsDownloader {
    public static final int DEFAULT_BUFFER_SIZE = 8192;

//    public static final String PART_FOLDER_PATH = "G:\\Projects\\ts\\out";
    public static final String PART_FOLDER_PATH = "/tmp/out";
    public static final String RESULT_FOLDER_PATH = "G:\\Projects\\ts\\result";

    public static String firstURLPath = "dsds";
    public static String lastURLPath = ".ts?e=1648409903&l=0&h=121c06d581339e0cad6cade0bd605f7a";

    public static int LINK_IDX_START = 17;
    public static int LINK_IDX_END   = 73;
    public static int GROUP_SIZE     = 200;

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        var runner = new VideoTsDownloader();
        runner.execute();
    }

    public void execute() throws Exception {
        cleanResultFolder(PART_FOLDER_PATH);
        cleanResultFolder(RESULT_FOLDER_PATH);
        System.out.println("======[ Clean successfully ]==============");

        System.out.println("=========[ Start Download, Store & Merge ]======================");
        downloadAndStore(getUrlList());
        runCmd(makeCommand(PART_FOLDER_PATH, ""), 0);
    }

    private void cleanResultFolder(String path) {
        List.of(new File(path).listFiles()).forEach(file -> {
            if (file.exists()) {
                file.delete();
            }
        });
    }

    private List<String> makeCommand(String folder, String tag) {
        var allFiles = Arrays.stream(new File(folder).listFiles()).sorted((f1, f2) -> {
            var f1Name = f1.getName();
            var f2Name = f2.getName();

            var f1Idx = Integer.parseInt(f1Name.split("\\.")[0]);
            var f2Idx = Integer.parseInt(f2Name.split("\\.")[0]);

            return  f1Idx - f2Idx;
        }).collect(Collectors.toList());

        StringBuilder allPath = new StringBuilder();
        List<String> mergedPaths = new ArrayList<>();
        int runner = 0;

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
               "ffmpeg -i \"concat:" + mergedPaths.get(i) + "\" -c copy \"" + RESULT_FOLDER_PATH + "\\" + i + tag +".ts\""
            );
        }
        return commands;
    }

    private void runCmd(List<String> commands, int flag) {
        System.out.println("===========[ Start to merge files ]=======================");
        System.out.println("========[ Commands Size: " + commands.size());

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
                    System.out.println(line);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });

        if (flag == 1) {
            System.out.println("===========[ DONE ]=============================");
            return;
        }

        System.out.println("==============[ Merge last time ]=====================");
        runCmd(makeCommand(RESULT_FOLDER_PATH, "_LAST"), 1);
    }

    private void downloadAndStore(List<String> urls) throws Exception {
        var futureList = new ArrayList<CompletableFuture<Void>>();

        for(int i = 0; i < urls.size(); i++) {
            int idx    = i + 1;
            String url = urls.get(i);

            var result = CompletableFuture.runAsync(() -> {
                try {
                    download(url, idx);
                    System.out.println("=====[ Counter: " + counter.getAndIncrement() + " ]=======================");;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futureList.add(result);
        }

        var result = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        result.get();
        System.out.println("======[ Finish to download ]=====================");
    }

    private void download(String url, int idx) throws Exception {
        File file = new File(PART_FOLDER_PATH + "\\" + idx + ".ts");

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

    // Using HttpClient In Java 11
    private void downloadV2(String url, int idx) throws Exception {
        File file = new File(PART_FOLDER_PATH + "/" + idx + ".ts");

        var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .version(HttpClient.Version.HTTP_2)
                .GET()
                .build();

        HttpClient.newBuilder()
                  .build()
                  .send(request, HttpResponse.BodyHandlers.ofFile(file.toPath()));
    }

    private List<String> getUrlList() {
        var urls = new ArrayList<String>();
        for(int i = LINK_IDX_START; i <= LINK_IDX_END; i++) {
            var url = firstURLPath + i + lastURLPath;
            urls.add(url);
        }

        return urls;
    }
}
