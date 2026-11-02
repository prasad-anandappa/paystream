package com.prasad.paystream.recon.service;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.AccountType;
import com.prasad.paystream.core.repository.AccountRepository;
import com.prasad.paystream.core.service.LedgerService;
import com.prasad.paystream.core.service.TransferService;
import com.prasad.paystream.recon.domain.ReconReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReconciliationServiceTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ReconciliationService reconciliationService;

    @Test
    void healthyBooksReconcileClean() {
        Account funding = ledgerService.createAccount("Funding", AccountType.ASSET, "USD", true);
        Account alice = ledgerService.createAccount("Alice", AccountType.LIABILITY, "USD", false);
        Account bob = ledgerService.createAccount("Bob", AccountType.LIABILITY, "USD", false);
        transferService.transfer("recon-fund", funding.getId(), alice.getId(), 9_000, "USD");
        transferService.transfer("recon-move", alice.getId(), bob.getId(), 3_250, "USD");

        ReconReport report = reconciliationService.runReconciliation();

        assertThat(report.isBalanced()).isTrue();
        assertThat(report.getDiscrepancyCount()).isZero();
        // Seeded SYSTEM_CASH + the 3 accounts created above.
        assertThat(report.getAccountsChecked()).isEqualTo(4);
        assertThat(report.getTotalDebitsMinor()).isEqualTo(report.getTotalCreditsMinor());
        assertThat(report.getTotalDebitsMinor()).isEqualTo(9_000 + 3_250);
    }

    @Test
    void tamperedCachedBalanceIsCaughtAndNamed() {
        Account funding = ledgerService.createAccount("Funding2", AccountType.ASSET, "USD", true);
        Account alice = ledgerService.createAccount("Alice2", AccountType.LIABILITY, "USD", false);
        transferService.transfer("recon-fund-2", funding.getId(), alice.getId(), 5_000, "USD");

        // Simulate drift/corruption: change the cached balance without any posting.
        Account stored = accountRepository.findById(alice.getId()).orElseThrow();
        stored.setBalanceMinor(4_999);
        accountRepository.saveAndFlush(stored);

        ReconReport report = reconciliationService.runReconciliation();

        assertThat(report.isBalanced()).isFalse();
        assertThat(report.getDiscrepancyCount()).isEqualTo(1);
        assertThat(report.getDetails()).contains("MISMATCH");
    }
}
