package com.prasad.paystream.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * A ledger account. The balance is a cached value maintained transactionally by
 * the ledger service using one sign convention for every account:
 * <b>balance = sum(DEBIT postings) - sum(CREDIT postings)</b> (debit-positive).
 * The reconciliation module independently recomputes balances from postings and
 * flags any drift, so the cache can never silently lie.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AccountType type;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
        // JPA
    }

    public Account(String name, AccountType type, String currency, boolean allowNegative) {
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.allowNegative = allowNegative;
        this.balanceMinor = 0L;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Applies a signed delta under the debit-positive convention. */
    public void applyDelta(long deltaMinor) {
        this.balanceMinor += deltaMinor;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public void setBalanceMinor(long balanceMinor) {
        this.balanceMinor = balanceMinor;
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
