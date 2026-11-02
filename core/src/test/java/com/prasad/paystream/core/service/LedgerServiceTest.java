package com.prasad.paystream.core.service;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.AccountType;
import com.prasad.paystream.core.domain.Direction;
import com.prasad.paystream.core.domain.JournalEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LedgerServiceTest {

    @Autowired
    private LedgerService ledgerService;

    @Test
    void balancedEntryMovesBalancesUnderDebitPositiveConvention() {
        Account cash = ledgerService.createAccount("Cash", AccountType.ASSET, "USD", true);
        Account revenue = ledgerService.createAccount("Revenue", AccountType.REVENUE, "USD", false);

        JournalEntry entry = ledgerService.postEntry("TEST-1", "Sale", List.of(
                new LedgerService.PostingLine(cash.getId(), Direction.DEBIT, 5_000),
                new LedgerService.PostingLine(revenue.getId(), Direction.CREDIT, 5_000)));

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getPostings()).hasSize(2);
        assertThat(ledgerService.getAccount(cash.getId()).getBalanceMinor()).isEqualTo(5_000);
        assertThat(ledgerService.getAccount(revenue.getId()).getBalanceMinor()).isEqualTo(-5_000);
    }

    @Test
    void unbalancedEntryIsRejectedAndNothingMoves() {
        Account a = ledgerService.createAccount("A", AccountType.ASSET, "USD", true);
        Account b = ledgerService.createAccount("B", AccountType.ASSET, "USD", true);

        assertThatThrownBy(() -> ledgerService.postEntry("TEST-2", "Broken", List.of(
                new LedgerService.PostingLine(a.getId(), Direction.DEBIT, 1_000),
                new LedgerService.PostingLine(b.getId(), Direction.CREDIT, 999))))
                .isInstanceOf(UnbalancedEntryException.class);

        assertThat(ledgerService.getAccount(a.getId()).getBalanceMinor()).isZero();
        assertThat(ledgerService.getAccount(b.getId()).getBalanceMinor()).isZero();
    }

    @Test
    void nonPositivePostingAmountsAreRejected() {
        Account a = ledgerService.createAccount("A2", AccountType.ASSET, "USD", true);
        Account b = ledgerService.createAccount("B2", AccountType.ASSET, "USD", true);

        assertThatThrownBy(() -> ledgerService.postEntry("TEST-3", "Zero", List.of(
                new LedgerService.PostingLine(a.getId(), Direction.DEBIT, 0),
                new LedgerService.PostingLine(b.getId(), Direction.CREDIT, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
