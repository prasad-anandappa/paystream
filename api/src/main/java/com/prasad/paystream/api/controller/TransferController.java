package com.prasad.paystream.api.controller;

import com.prasad.paystream.core.domain.Transfer;
import com.prasad.paystream.core.domain.TransferStatus;
import com.prasad.paystream.core.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    public record CreateTransferRequest(
            @NotBlank @Size(max = 120) String idempotencyKey,
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @Positive long amountMinor,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {
    }

    public record TransferResponse(Long id, String idempotencyKey, Long fromAccountId, Long toAccountId,
                                   long amountMinor, String currency, TransferStatus status,
                                   Long journalEntryId) {
        static TransferResponse from(Transfer transfer) {
            return new TransferResponse(transfer.getId(), transfer.getIdempotencyKey(),
                    transfer.getFromAccountId(), transfer.getToAccountId(), transfer.getAmountMinor(),
                    transfer.getCurrency(), transfer.getStatus(), transfer.getJournalEntryId());
        }
    }

    /**
     * Creates a transfer. Returns 201 the first time a key is seen; replaying the
     * same idempotency key returns 200 with the original transfer — safe retries.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        TransferService.TransferOutcome outcome = transferService.transfer(
                request.idempotencyKey(), request.fromAccountId(), request.toAccountId(),
                request.amountMinor(), request.currency());
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TransferResponse.from(outcome.transfer()));
    }

    @GetMapping("/{id}")
    public TransferResponse getTransfer(@PathVariable Long id) {
        return TransferResponse.from(transferService.getTransfer(id));
    }
}
