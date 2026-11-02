package com.prasad.paystream.api.controller;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.AccountType;
import com.prasad.paystream.core.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final LedgerService ledgerService;

    public AccountController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull AccountType type,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            boolean allowNegative) {
    }

    public record AccountResponse(Long id, String name, AccountType type, String currency,
                                  long balanceMinor, boolean allowNegative) {
        static AccountResponse from(Account account) {
            return new AccountResponse(account.getId(), account.getName(), account.getType(),
                    account.getCurrency(), account.getBalanceMinor(), account.isAllowNegative());
        }
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = ledgerService.createAccount(
                request.name(), request.type(), request.currency(), request.allowNegative());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return AccountResponse.from(ledgerService.getAccount(id));
    }

    @GetMapping
    public List<AccountResponse> listAccounts() {
        return ledgerService.listAccounts().stream().map(AccountResponse::from).toList();
    }
}
