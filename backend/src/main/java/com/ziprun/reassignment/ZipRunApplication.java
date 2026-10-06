package com.ziprun.reassignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // CRITICAL: Without this, @Async @EventListener runs synchronously and blocks PATCH
public class ZipRunApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZipRunApplication.class, args);
    }
}
