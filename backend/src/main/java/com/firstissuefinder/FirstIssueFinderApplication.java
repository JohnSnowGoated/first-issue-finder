package com.firstissuefinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FirstIssueFinderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirstIssueFinderApplication.class, args);
    }
}
