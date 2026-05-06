package com.demo.facedetection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    // Allow React dev server to call the backend
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")   // React dev server
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }

    // Thread pool for @Async video processing jobs
    @Bean(name = "videoProcessingExecutor")
    public Executor videoProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("video-proc-");
        executor.initialize();
        return executor;
    }
}
