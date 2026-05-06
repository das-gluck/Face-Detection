package com.demo.facedetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync   // enables @Async on VideoService for non-blocking processing
public class FaceDetectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(FaceDetectionApplication.class, args);
    }
}
