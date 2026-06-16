package com.example.reminder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class NotificationExecutorConfig {
    @Bean
    public Executor notificationExecutor(
            @Value("${app.notify.worker-threads:4}") int workerThreads,
            @Value("${app.notify.queue-capacity:100}") int queueCapacity
    ){
        int poolSize = Math.max(workerThreads, 1);
        int effectiveQueueCapacity = Math.max(queueCapacity, 1);

        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(effectiveQueueCapacity);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }
}
