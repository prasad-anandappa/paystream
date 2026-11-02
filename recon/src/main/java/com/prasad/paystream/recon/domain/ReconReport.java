package com.prasad.paystream.recon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The persisted result of one reconciliation run. A run is "balanced" only if
 * every cached account balance matches the balance recomputed from postings
 * AND the platform-wide trial balance holds (total debits == total credits).
 */
@Entity
@Table(name = "recon_reports")
public class ReconReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "accounts_checked", nullable = false)
    private int accountsChecked;

    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount;

    @Column(name = "total_debits_minor", nullable = false)
    private long totalDebitsMinor;

    @Column(name = "total_credits_minor", nullable = false)
    private long totalCreditsMinor;

    @Column(name = "balanced", nullable = false)
    private boolean balanced;

    @Column(name = "details", nullable = false)
    private String details;

    protected ReconReport() {
        // JPA
    }

    public ReconReport(int accountsChecked, int discrepancyCount, long totalDebitsMinor,
                       long totalCreditsMinor, boolean balanced, String details) {
        this.accountsChecked = accountsChecked;
        this.discrepancyCount = discrepancyCount;
        this.totalDebitsMinor = totalDebitsMinor;
        this.totalCreditsMinor = totalCreditsMinor;
        this.balanced = balanced;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public int getAccountsChecked() {
        return accountsChecked;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public long getTotalDebitsMinor() {
        return totalDebitsMinor;
    }

    public long getTotalCreditsMinor() {
        return totalCreditsMinor;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public String getDetails() {
        return details;
    }
}
