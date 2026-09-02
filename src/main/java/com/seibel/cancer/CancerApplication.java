package com.seibel.cancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling powers DiagnosisIntakeSessionStore's expired-session sweep - the only
// @Scheduled method anywhere in the application.
@SpringBootApplication
@EnableScheduling
public class CancerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CancerApplication.class, args);
    }

}
