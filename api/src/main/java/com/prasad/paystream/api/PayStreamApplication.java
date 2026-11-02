package com.prasad.paystream.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PayStream — event-driven payments platform.
 * Wires the core ledger, the outbox events module and the reconciliation
 * module into one runnable service.
 */
@SpringBootApplication(scanBasePackages = "com.prasad.paystream")
@EntityScan(basePackages = "com.prasad.paystream")
@EnableJpaRepositories(basePackages = "com.prasad.paystream")
@EnableScheduling
public class PayStreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayStreamApplication.class, args);
    }
}
