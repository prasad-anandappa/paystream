package com.prasad.paystream.events;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Test bootstrap for the events module (scheduling intentionally not enabled). */
@SpringBootApplication(scanBasePackages = "com.prasad.paystream")
@EntityScan(basePackages = "com.prasad.paystream")
@EnableJpaRepositories(basePackages = "com.prasad.paystream")
public class EventsTestApplication {
}
