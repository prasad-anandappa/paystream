package com.prasad.paystream.core.service;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.AccountType;
import com.prasad.paystream.core.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TransferServiceTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferRepository transferRepository;

    private Account fundedAccount(String name, long fundMinor) {
        Account funding = ledgerService.createAccount(name + "-funding", AccountType.ASSET, "USD", true);
        Account account = ledgerService.createAccount(name, AccountType.LIABILITY, "USD", false);
        transferService.transfer("fund-" + name, funding.getId(), account.getId(), fundMinor, "USD");
        return account;
    }

    @Test
    void transferMovesMoneyExactlyOnce() {
        Account alice = fundedAccount("Alice", 10_000);
        Account bob = ledgerService.createAccount("Bob", AccountType.LIABILITY, "USD", false);

        var outcome = transferService.transfer("key-1", alice.getId(), bob.getId(), 2_500, "USD");

        assertThat(outcome.created()).isTrue();
        assertThat(ledgerService.getAccount(alice.getId()).getBalanceMinor()).isEqualTo(7_500);
        assertThat(ledgerService.getAccount(bob.getId()).getBalanceMinor()).isEqualTo(2_500);
    }

    @Test
    void replayingTheSameIdempotencyKeyReturnsTheOriginalAndMovesNothing() {
        Account alice = fundedAccount("Carol", 10_000);
        Account bob = ledgerService.createAccount("Dave", AccountType.LIABILITY, "USD", false);

        var first = transferService.transfer("key-dup", alice.getId(), bob.getId(), 4_000, "USD");
        var replay = transferService.transfer("key-dup", alice.getId(), bob.getId(), 4_000, "USD");

        assertThat(replay.created()).isFalse();
        assertThat(replay.transfer().getId()).isEqualTo(first.transfer().getId());
        assertThat(transferRepository.findByIdempotencyKey("key-dup")).isPresent();
        assertThat(ledgerService.getAccount(alice.getId()).getBalanceMinor()).isEqualTo(6_000);
        assertThat(ledgerService.getAccount(bob.getId()).getBalanceMinor()).isEqualTo(4_000);
    }

    @Test
    void insufficientFundsAreRejected() {
        Account alice = fundedAccount("Erin", 1_000);
        Account bob = ledgerService.createAccount("Frank", AccountType.LIABILITY, "USD", false);

        assertThatThrownBy(() -> transferService.transfer("key-poor", alice.getId(), bob.getId(), 1_001, "USD"))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(ledgerService.getAccount(bob.getId()).getBalanceMinor()).isZero();
    }

    @Test
    void transferToSelfIsRejected() {
        Account alice = fundedAccount("Grace", 1_000);

        assertThatThrownBy(() -> transferService.transfer("key-self", alice.getId(), alice.getId(), 100, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
