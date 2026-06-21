package com.aihealthcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiHealthCoachBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHealthCoachBackendApplication.class, args);
    }
}
