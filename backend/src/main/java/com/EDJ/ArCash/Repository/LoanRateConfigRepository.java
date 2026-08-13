package com.EDJ.ArCash.Repository;

import com.EDJ.ArCash.Models.LoanRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRateConfigRepository extends JpaRepository<LoanRateConfig, Integer> {
    List<LoanRateConfig> findAllByOrderByInstallmentsAsc();
}
