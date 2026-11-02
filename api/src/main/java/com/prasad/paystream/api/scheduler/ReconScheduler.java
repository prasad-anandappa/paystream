package com.prasad.paystream.api.scheduler;

import com.prasad.paystream.recon.domain.ReconReport;
import com.prasad.paystream.recon.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs reconciliation on a fixed cadence (default every 5 minutes) so balance
 * drift is detected by the system itself, not by a customer complaint.
 */
@Component
public class ReconScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconScheduler.class);

    private final ReconciliationService reconciliationService;

    public ReconScheduler(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(fixedDelayString = "${paystream.recon.fixed-delay-ms:300000}")
    public void scheduledReconciliation() {
        try {
            ReconReport report = reconciliationService.runReconciliation();
            if (report.isBalanced()) {
                log.info("Reconciliation #{} OK: {} accounts checked, trial balance {} minor units",
                        report.getId(), report.getAccountsChecked(), report.getTotalDebitsMinor());
            } else {
                log.error("Reconciliation #{} FOUND PROBLEMS: {} discrepancies — {}",
                        report.getId(), report.getDiscrepancyCount(), report.getDetails());
            }
        } catch (Exception e) {
            // Never let a failed run kill the scheduler thread.
            log.error("Scheduled reconciliation failed", e);
        }
    }
}
