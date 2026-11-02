package com.prasad.paystream.core.service;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.Direction;
import com.prasad.paystream.core.domain.JournalEntry;
import com.prasad.paystream.core.domain.Transfer;
import com.prasad.paystream.core.domain.TransferStatus;
import com.prasad.paystream.core.port.DomainEventPublisher;
import com.prasad.paystream.core.repository.TransferRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Moves money between accounts as a balanced journal entry (CREDIT the source,
 * DEBIT the destination, under the debit-positive convention) and records the
 * transfer under a client-supplied idempotency key.
 *
 * <p>Safety properties:</p>
 * <ul>
 *   <li>Replaying the same key returns the original transfer — money never moves twice.</li>
 *   <li>The unique constraint on the key is the backstop: if two identical requests
 *       race, the loser's insert fails and it re-reads the winner's transfer.</li>
 *   <li>A {@code transfer.completed} event is announced through the
 *       {@link DomainEventPublisher} port in the same transaction, so with the
 *       outbox adapter installed, the event can never be lost or duplicated
 *       relative to the state change.</li>
 * </ul>
 */
@Service
public class TransferService {

    private final LedgerService ledgerService;
    private final TransferRepository transferRepository;
    private final ObjectProvider<DomainEventPublisher> eventPublisher;

    public TransferService(LedgerService ledgerService,
                           TransferRepository transferRepository,
                           ObjectProvider<DomainEventPublisher> eventPublisher) {
        this.ledgerService = ledgerService;
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    /** The transfer plus whether this call created it ({@code false} = idempotent replay). */
    public record TransferOutcome(Transfer transfer, boolean created) {
    }

    @Transactional
    public TransferOutcome transfer(String idempotencyKey, Long fromAccountId, Long toAccountId,
                                    long amountMinor, String currency) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("An idempotency key is required");
        }
        var existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new TransferOutcome(existing.get(), false);
        }
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive minor units");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }

        Account from = ledgerService.getAccount(fromAccountId);
        Account to = ledgerService.getAccount(toAccountId);
        if (!from.getCurrency().equalsIgnoreCase(currency) || !to.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency mismatch between accounts and transfer");
        }
        if (!from.isAllowNegative() && from.getBalanceMinor() < amountMinor) {
            throw new InsufficientFundsException(fromAccountId, from.getBalanceMinor(), amountMinor);
        }

        JournalEntry entry = ledgerService.postEntry(
                "TRF:" + idempotencyKey,
                "Transfer of " + amountMinor + " " + currency.toUpperCase()
                        + " from account " + fromAccountId + " to account " + toAccountId,
                List.of(
                        new LedgerService.PostingLine(fromAccountId, Direction.CREDIT, amountMinor),
                        new LedgerService.PostingLine(toAccountId, Direction.DEBIT, amountMinor)));

        Transfer transfer = new Transfer(idempotencyKey, fromAccountId, toAccountId, amountMinor,
                currency.toUpperCase(), TransferStatus.COMPLETED, entry.getId());
        try {
            transfer = transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException race) {
            // A concurrent request with the same key won the insert. Return its transfer.
            return transferRepository.findByIdempotencyKey(idempotencyKey)
                    .map(winner -> new TransferOutcome(winner, false))
                    .orElseThrow(() -> race);
        }

        DomainEventPublisher publisher = eventPublisher.getIfAvailable();
        if (publisher != null) {
            publisher.publish("Transfer", String.valueOf(transfer.getId()), "transfer.completed",
                    "{\"transferId\":" + transfer.getId()
                            + ",\"fromAccountId\":" + fromAccountId
                            + ",\"toAccountId\":" + toAccountId
                            + ",\"amountMinor\":" + amountMinor
                            + ",\"currency\":\"" + currency.toUpperCase() + "\"}");
        }
        return new TransferOutcome(transfer, true);
    }

    @Transactional(readOnly = true)
    public Transfer getTransfer(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    }
}
