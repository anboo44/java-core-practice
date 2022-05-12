package com.learn.java.ts.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface MergeManager {
    // all files are same type
    File merge(List<File> files, String baseFolder);

    // file isn't same type
    Void mergeAudioAndVideo(File audio, File video, String baseFolder);
}

class MergeManagerImpl implements MergeManager {

    private CommandBuilder commandBuilder;

    public MergeManagerImpl(CommandBuilder commandBuilder) {
        this.commandBuilder = commandBuilder;
    }

    @Override
    public File merge(List<File> files, String baseFolder) {
        System.out.printf("========[ START TO MERGE FILE LIST: %s ]============\n", baseFolder);
        // Setting
        var rdValue = UUID.randomUUID().toString();
        var storageFolder = new File(baseFolder + "/" + rdValue);
        if (!storageFolder.exists()) storageFolder.mkdirs();

        // Process
        var commands = commandBuilder.getMergedCommand(files, storageFolder);
        runCommandAsync(commands);

        // Handle result
        var mergedFiles = storageFolder.listFiles();
        if (mergedFiles.length > 1) {
            return merge(List.of(mergedFiles), baseFolder);
        } else if (mergedFiles.length == 1) {
            return mergedFiles[0];
        }
        return null;
    }

    @Override
    public Void mergeAudioAndVideo(File audio, File video, String baseFolder) {
        if (audio == null || video == null) return null;

        System.out.println("========[ START TO MERGE AUDIO & VIDEO FILES ]============");
        System.out.printf("==========[ Audio file name: %s ]============\n", audio.getPath());
        System.out.printf("==========[ Video file name: %s ]============\n", video.getPath());
        var storageFolder = new File(baseFolder);
        var command = commandBuilder.getMergedCommand(video, audio, storageFolder);
        try {
            return runCommand(command, true);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Void> runCommandAsync(List<String> commands) {
        var futures = commands.stream().map(v -> runCommandAsync(v , false)).collect(Collectors.toList());
        return sequence(futures).join();
    }

    private CompletableFuture<Void> runCommandAsync(String command, boolean showLog) {
        return CompletableFuture.runAsync(() -> {
            runCommand(command, showLog);
        });
    }

    private Void runCommand(String command, boolean showLog) {
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null || line.isEmpty()) break;
                if (showLog) System.out.println(line);
            }
        } catch (Exception e) {
            System.out.println("ERROR");
        }
        return null;
    }

    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> com) {
        return CompletableFuture.allOf(com.toArray(new CompletableFuture<?>[0]))
                .thenApply(v -> com.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }
}
