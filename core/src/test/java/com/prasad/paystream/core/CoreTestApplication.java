package com.prasad.paystream.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Test bootstrap: library modules carry no application class of their own. */
@SpringBootApplication(scanBasePackages = "com.prasad.paystream.core")
@EntityScan(basePackages = "com.prasad.paystream")
@EnableJpaRepositories(basePackages = "com.prasad.paystream")
public class CoreTestApplication {
}
