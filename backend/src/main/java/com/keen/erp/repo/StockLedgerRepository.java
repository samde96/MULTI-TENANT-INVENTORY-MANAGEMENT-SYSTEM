package com.keen.erp.repo;

import com.keen.erp.entity.StockLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, Long> {
    List<StockLedgerEntry> findTop100ByOrderByCreatedAtDesc();
}
