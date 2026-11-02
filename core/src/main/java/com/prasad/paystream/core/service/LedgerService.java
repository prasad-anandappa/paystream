package com.prasad.paystream.core.service;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.AccountType;
import com.prasad.paystream.core.domain.Direction;
import com.prasad.paystream.core.domain.JournalEntry;
import com.prasad.paystream.core.domain.Posting;
import com.prasad.paystream.core.repository.AccountRepository;
import com.prasad.paystream.core.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The double-entry ledger. Every write goes through {@link #postEntry}, which
 * enforces the fundamental invariant — total debits == total credits — before
 * anything is persisted, and updates account balances in the same transaction
 * using the debit-positive convention (DEBIT increases a balance, CREDIT
 * decreases it). Optimistic locking on {@code Account.version} turns concurrent
 * balance updates into a visible conflict instead of a silent lost update.
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public LedgerService(AccountRepository accountRepository,
                         JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    /** One posting line of a journal entry: which account, which side, how much (minor units, &gt; 0). */
    public record PostingLine(Long accountId, Direction direction, long amountMinor) {
    }

    @Transactional
    public Account createAccount(String name, AccountType type, String currency, boolean allowNegative) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name is required");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
        return accountRepository.save(new Account(name, type, currency.toUpperCase(), allowNegative));
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Posts one balanced journal entry and applies its balance effects atomically.
     *
     * @throws UnbalancedEntryException if total debits != total credits (nothing is persisted)
     * @throws AccountNotFoundException if any posting references a missing account
     */
    @Transactional
    public JournalEntry postEntry(String reference, String description, List<PostingLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry needs at least two posting lines");
        }
        long totalDebits = 0L;
        long totalCredits = 0L;
        for (PostingLine line : lines) {
            if (line.amountMinor() <= 0) {
                throw new IllegalArgumentException("Posting amounts must be positive minor units");
            }
            if (line.direction() == Direction.DEBIT) {
                totalDebits += line.amountMinor();
            } else {
                totalCredits += line.amountMinor();
            }
        }
        if (totalDebits != totalCredits) {
            throw new UnbalancedEntryException(totalDebits, totalCredits);
        }

        JournalEntry entry = new JournalEntry(reference, description);
        for (PostingLine line : lines) {
            Account account = getAccount(line.accountId());
            long delta = line.direction() == Direction.DEBIT ? line.amountMinor() : -line.amountMinor();
            account.applyDelta(delta);
            accountRepository.save(account);
            entry.addPosting(new Posting(account, line.direction(), line.amountMinor()));
        }
        return journalEntryRepository.save(entry);
    }
}
