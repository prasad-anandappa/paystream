package com.prasad.paystream.core.repository;

import com.prasad.paystream.core.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
}
