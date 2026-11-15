package com.saas.platform.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * User Service - Patrón CQRS
 * 
 * Arquitectura:
 * - Commands: Operaciones de escritura (Create, Update, Delete)
 * - Queries: Operaciones de lectura (Get, Find, List)
 * 
 * Características:
 * - Spring Boot 3.2
 * - Spring Security 6 con JWT
 * - PostgreSQL + JPA
 * - Docker ready
 * - Configuración YML
 * - Async command processing
 */
@SpringBootApplication
@EnableAsync
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
