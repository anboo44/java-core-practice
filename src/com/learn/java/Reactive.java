package com.learn.java;

import java.util.Arrays;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

class Message {
    public final String message = "Hello_World";
}

class Pub implements Flow.Publisher<Message> {

    @Override
    public void subscribe(Flow.Subscriber<? super Message> subscriber) {
        System.out.println("Publisher: subscriber");
    }
}

class Consumer implements Flow.Subscriber<Message> {

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.println("===========[ ON_SUBSCRIBE ]=============");
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(Message item) {
        System.out.println("==========[ Message: " + item.message + " ]=============");
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}

public class Reactive {
    public static void main(String[] args) throws InterruptedException {
        var c = new Consumer();
        var p = new Pub();

        var publisher = new SubmissionPublisher<Message>();
        publisher.subscribe(c);
        p.subscribe(c);

        Arrays.asList(1, 2, 4, 5, 6, 7).forEach(v -> {
            publisher.submit(new Message());
        });

        Thread.sleep(10000);
    }
}
