package com.example.jobdispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for the Job Dispatcher service.
 * 
 * This application provides a REST API for submitting and managing jobs:
 * - One-time jobs that execute once
 * - Repetitive jobs that execute on a schedule
 * 
 * Jobs are dispatched to appropriate thread pools for execution.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class JobDispatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobDispatcherApplication.class, args);
    }
}
