package com.aihealthcoach.common.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ChatContextTaskExecutorConfig {

    @Bean(name = "chatContextTaskExecutor", defaultCandidate = false)
    public Executor chatContextTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("chat-context-");
        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(7);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
