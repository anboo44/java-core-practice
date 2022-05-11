package com.learn.java.ts.downloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public interface CommandBuilder {
    List<String> getMergedCommand(List<File> files, File folder);
    String getMergedCommand(File video, File audio, File folder);
}

class CommandBuilderImpl implements CommandBuilder {

    private final int GROUP_SIZE = 200;

    @Override
    public List<String> getMergedCommand(List<File> files, File folder) {
        if (files.size() == 1) return List.of();

        var sortedFiles = files.stream().sorted((f1, f2) -> {
            var f1Name = f1.getName();
            var f2Name = f2.getName();

            var f1Idx = Integer.parseInt(f1Name.split("\\.")[0]);
            var f2Idx = Integer.parseInt(f2Name.split("\\.")[0]);

            return  f1Idx - f2Idx;
        }).collect(Collectors.toList());

        StringBuilder allPath = new StringBuilder();
        List<String> mergedPaths = new ArrayList<>();
        int runner = 0;

        // concat by "|" per 200 files
        for(int i = 0; i < sortedFiles.size(); i++) {
            var filePath = sortedFiles.get(i).getPath();
            allPath.append(filePath).append("|");
            runner ++;

            var isResetPath = (runner == GROUP_SIZE) || (i == sortedFiles.size() - 1);
            if (isResetPath) {
                mergedPaths.add(allPath.substring(0, allPath.length() - 1));
                allPath = new StringBuilder();
                runner  = 0;
            }
        }

        List<String> commands = new ArrayList<>();
        for(int i = 0; i < mergedPaths.size(); i++) {
            commands.add(
                    "ffmpeg -i \"concat:" + mergedPaths.get(i) + "\" -c copy \"" + folder.getPath() + "\\" + i + ".ts\""
            );
        }
        return commands;
    }

    @Override
    public String getMergedCommand(File video, File audio, File folder) {
        var rdValue = UUID.randomUUID().toString();
        var file = new File(folder.getPath() + "/" + rdValue + ".ts");
        return String.format(
                "ffmpeg -i %s -i %s -c:v copy -c:a aac %s",
                video.getPath(), audio.getPath(), file.getPath()
        );
    }
}
