package com.prasad.paystream.api.controller;

import com.prasad.paystream.recon.domain.ReconReport;
import com.prasad.paystream.recon.service.ReconciliationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    public record ReconReportResponse(Long id, Instant generatedAt, int accountsChecked,
                                      int discrepancyCount, long totalDebitsMinor, long totalCreditsMinor,
                                      boolean balanced, String details) {
        static ReconReportResponse from(ReconReport report) {
            return new ReconReportResponse(report.getId(), report.getGeneratedAt(),
                    report.getAccountsChecked(), report.getDiscrepancyCount(),
                    report.getTotalDebitsMinor(), report.getTotalCreditsMinor(),
                    report.isBalanced(), report.getDetails());
        }
    }

    /** Runs a reconciliation on demand (the scheduler also runs it periodically). */
    @PostMapping("/run")
    public ReconReportResponse run() {
        return ReconReportResponse.from(reconciliationService.runReconciliation());
    }

    @GetMapping("/latest")
    public ReconReportResponse latest() {
        return ReconReportResponse.from(reconciliationService.latestReport());
    }
}
