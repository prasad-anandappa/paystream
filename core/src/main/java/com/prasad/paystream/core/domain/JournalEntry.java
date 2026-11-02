package com.prasad.paystream.core.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One balanced accounting event. A journal entry is only ever persisted with
 * postings whose total debits equal total credits — the ledger service enforces
 * that invariant before anything is written.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reference")
    private String reference;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Posting> postings = new ArrayList<>();

    protected JournalEntry() {
        // JPA
    }

    public JournalEntry(String reference, String description) {
        this.reference = reference;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addPosting(Posting posting) {
        postings.add(posting);
        posting.setJournalEntry(this);
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Posting> getPostings() {
        return postings;
    }
}
