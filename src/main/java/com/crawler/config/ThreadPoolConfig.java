package com.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ForkJoinPool crawlerForkJoinPool() {
        return new ForkJoinPool(
                Runtime.getRuntime().availableProcessors() * 2,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null,
                true
        );
    }

    @Bean
    public ExecutorService ioExecutorService() {
        return Executors.newFixedThreadPool(10, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "io-worker-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "scheduler-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }
}