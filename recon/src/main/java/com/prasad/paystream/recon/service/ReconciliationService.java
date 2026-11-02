package com.prasad.paystream.recon.service;

import com.prasad.paystream.core.domain.Account;
import com.prasad.paystream.core.domain.Direction;
import com.prasad.paystream.core.repository.AccountRepository;
import com.prasad.paystream.core.repository.PostingRepository;
import com.prasad.paystream.recon.domain.ReconReport;
import com.prasad.paystream.recon.repository.ReconReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reconciliation is the ledger's independent auditor. It trusts nothing the
 * write path cached: for every account it recomputes the balance from the
 * posting history (debit-positive: debits minus credits) and compares it to
 * the stored balance, then checks the trial balance across the whole platform.
 * Any drift is named in the persisted report — this is the control that turns
 * "the API said so" into "the books provably agree".
 */
@Service
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final PostingRepository postingRepository;
    private final ReconReportRepository reportRepository;

    public ReconciliationService(AccountRepository accountRepository,
                                 PostingRepository postingRepository,
                                 ReconReportRepository reportRepository) {
        this.accountRepository = accountRepository;
        this.postingRepository = postingRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public ReconReport runReconciliation() {
        List<Account> accounts = accountRepository.findAll();
        StringBuilder details = new StringBuilder();
        int discrepancies = 0;

        for (Account account : accounts) {
            long debits = postingRepository.sumByAccountAndDirection(account.getId(), Direction.DEBIT);
            long credits = postingRepository.sumByAccountAndDirection(account.getId(), Direction.CREDIT);
            long recomputed = debits - credits;
            if (recomputed != account.getBalanceMinor()) {
                discrepancies++;
                details.append("MISMATCH account ").append(account.getId())
                        .append(" (").append(account.getName()).append("): stored=")
                        .append(account.getBalanceMinor())
                        .append(" recomputed=").append(recomputed).append("; ");
            }
        }

        long totalDebits = postingRepository.sumByDirection(Direction.DEBIT);
        long totalCredits = postingRepository.sumByDirection(Direction.CREDIT);
        boolean trialBalanced = totalDebits == totalCredits;
        if (!trialBalanced) {
            details.append("TRIAL BALANCE BROKEN: debits=").append(totalDebits)
                    .append(" credits=").append(totalCredits).append("; ");
        }

        boolean balanced = trialBalanced && discrepancies == 0;
        if (balanced) {
            details.append("All ").append(accounts.size())
                    .append(" account balances match their posting history; trial balance holds.");
        }

        ReconReport report = new ReconReport(accounts.size(), discrepancies,
                totalDebits, totalCredits, balanced, details.toString());
        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public ReconReport latestReport() {
        return reportRepository.findTopByOrderByGeneratedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No reconciliation has run yet"));
    }
}
