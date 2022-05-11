package com.learn.java.ts.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
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
        System.out.println("========[ START TO MERGE FILE LIST ]============");
        // Setting
        var rdValue = UUID.randomUUID().toString();
        var storageFolder = new File(baseFolder + "/" + rdValue);
        if (!storageFolder.exists()) storageFolder.mkdirs();

        // Process
        var commands = commandBuilder.getMergedCommand(files, storageFolder);
        commands.forEach(this::runCommand);
//        var futureResults = runCommandAsync(commands);
//        return futureResults.

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
        var storageFolder = new File(baseFolder);
        var command = commandBuilder.getMergedCommand(video, audio, storageFolder);
        try {
            return runCommandAsync(command).get();
        } catch (Exception e) {
            return null;
        }
    }

    private CompletableFuture<List<Void>> runCommandAsync(List<String> commands) {
        var futures = commands.stream().map(this::runCommandAsync).collect(Collectors.toList());
        return sequence(futures);
    }

    private CompletableFuture<Void> runCommandAsync(String command) {
        return CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.redirectErrorStream(true);
                Process p = builder.start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                do {
                    line = r.readLine();
                } while (line != null);
            } catch (Exception e) {
                System.out.println("ERROR");
            }
        }, Executors.newFixedThreadPool(8));
    }

    private void runCommand(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            do {
                line = r.readLine();
            } while (line != null);
        } catch (Exception e) {
            System.out.println("ERROR");
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
