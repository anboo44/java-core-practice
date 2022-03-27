package com.learn.java;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VideoTsDownloader {
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static final String PART_FOLDER_PATH = "G:\\Projects\\ts\\out";
    public static final String RESULT_FOLDER_PATH = "G:\\Projects\\ts\\result";

    public static String firstPath = "dsds";
    public static String lastPath = ".ts?e=1648409903&l=0&h=121c06d581339e0cad6cade0bd605f7a";

    public static int START = 17;
    public static int END   = 73;
    public static int PER   = 200;

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        var runner = new VideoTsDownloader();
        runner.execute();
    }

    public void execute() throws Exception {
        cleanResultFolder(PART_FOLDER_PATH);
        cleanResultFolder(RESULT_FOLDER_PATH);
        System.out.println("======[ Clean successfully ]==============");

        downloadAndStore(getUrlList());
        runCmd(makeCommand(PART_FOLDER_PATH, ""), 0);
    }

    private void cleanResultFolder(String path) {
        var file = new File(path);
        var files = file.listFiles();
        for(int i = 0; i < files.length; i++) {
            if (files[i].exists()) {
                files[i].delete();
            }
        }
    }

    private List<String> makeCommand(String folder, String tag) {
        File file = new File(folder);
        var allFiles = Arrays.stream(file.listFiles()).sorted((t1, t2) -> {
            var t1Name = t1.getName();
            var t2Name = t2.getName();

            var t1Idx = Integer.parseInt(t1Name.split("\\.")[0]);
            var t2Idx = Integer.parseInt(t2Name.split("\\.")[0]);

            return  t1Idx - t2Idx;
        }).collect(Collectors.toList());

        String allPath = "";
        List<String> mergedPaths = new ArrayList<>();
        int runner = 0;

        for(int i = 0; i < allFiles.size(); i++) {
            allPath += allFiles.get(i).getPath() + "|";
            runner ++;

            if (runner == PER || i == (allFiles.size() - 1)) {
                mergedPaths.add(allPath.substring(0, allPath.length() - 1));
                allPath = "";
                runner = 0;
            }
        }

        List<String> commands = new ArrayList<>();
        System.out.println("=========== [ Size: " + mergedPaths.size());
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

        if (flag < 2) {
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
    }

    private void downloadAndStore(List<String> urls) throws Exception {
        var futureList = new ArrayList<CompletableFuture<Void>>();

        System.out.println("=========[ Start Download ]======================");
        for(int i = 0; i < urls.size(); i++) {
            int idx = i + 1;
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

        long urlFileLength = connection.getContentLength();
        InputStream input = connection.getInputStream();

        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = input.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    private List<String> getUrlList() {
        var urls = new ArrayList<String>();
        for(int i = START; i <= END; i++) {
            var url = firstPath + i + lastPath;
            urls.add(url);
        }

        return urls;
    }
}
