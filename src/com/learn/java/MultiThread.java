package com.learn.java;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class MultiThread {
    public static void main(String[] args) throws Exception {
        var f1 = CompletableFuture.runAsync(() -> {
            System.out.println("Run multi-thread");
        });

        var f2 = CompletableFuture
                .supplyAsync(() -> "Hello_World")
                .thenApply(t -> {
                    if (t.equals("Hello_World")) {
                        System.out.println("=======[ Hello_World ]==========");
                        return t + "__XX";
                    } else {
                        return t;
                    }
                }).thenAccept(t -> {
                    System.out.println("=========/ End Chain /==========");
                }).thenRun(() -> {
                    System.out.println("=========/ Then Run /============");
                });

        var f3 = CompletableFuture
                .supplyAsync(() -> 4)
                .thenCompose(t -> CompletableFuture.supplyAsync(() -> t * 2))
                .thenAccept(v -> System.out.println("Value Compose: " + v));

        var f4 = CompletableFuture
                .supplyAsync(() -> 4)
                .thenCombine(
                        CompletableFuture.supplyAsync(() -> 8),
                        Integer::sum
                )
                .thenCombine(CompletableFuture.supplyAsync(() -> 8), Integer::sum)
                .thenAccept(t -> System.out.println("Value Combine: " + t));


        Stream.of(
                CompletableFuture.supplyAsync(() -> 4),
                CompletableFuture.supplyAsync(() -> 5)
                ).map(CompletableFuture::join).reduce(Integer::sum).ifPresent(v -> System.out.println("Value_Total_Join: " + v));

        var f5 = CompletableFuture
                .allOf(f1, f4)
                .thenRun(() -> System.out.println("Done All Futures"));

        var f6 = Stream.of(1, 2, 3).map(v -> CompletableFuture.supplyAsync(() -> v * 2))
                        .reduce(
                                CompletableFuture.supplyAsync(() -> 0),
                                (x, y) -> x.thenCombine(y, Integer::sum)
                        ).thenAccept(t -> System.out.println("Total: " + t))
                        .exceptionally(ex -> {
                            System.out.println("Error happened with msg: " + ex.getMessage());
                            return null;
                        });


        f5.get();
        f1.get();
        f2.get();
        f4.get();
        f6.get();
    }

    public synchronized void add(int value){
        System.out.println("dsdsd");
    }
}
