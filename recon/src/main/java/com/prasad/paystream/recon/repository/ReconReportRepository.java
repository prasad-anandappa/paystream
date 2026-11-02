package com.prasad.paystream.recon.repository;

import com.prasad.paystream.recon.domain.ReconReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReconReportRepository extends JpaRepository<ReconReport, Long> {

    Optional<ReconReport> findTopByOrderByGeneratedAtDesc();
}
