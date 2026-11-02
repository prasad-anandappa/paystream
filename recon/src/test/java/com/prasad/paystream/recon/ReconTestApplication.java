package com.prasad.paystream.recon;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Test bootstrap for the reconciliation module. */
@SpringBootApplication(scanBasePackages = "com.prasad.paystream")
@EntityScan(basePackages = "com.prasad.paystream")
@EnableJpaRepositories(basePackages = "com.prasad.paystream")
public class ReconTestApplication {
}
