package com.tcspring.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot context anchor for tests.
 * Required in modules that have no production @SpringBootApplication.
 * Real microservices already have a main class and do not need this.
 */
@SpringBootApplication
public class TestApplication {
}
