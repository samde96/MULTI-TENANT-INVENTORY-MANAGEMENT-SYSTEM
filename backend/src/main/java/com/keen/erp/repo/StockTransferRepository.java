package com.keen.erp.repo;

import com.keen.erp.entity.StockTransfer;
import com.keen.erp.entity.TransferStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    @EntityGraph(attributePaths = {"items", "items.product", "request", "sourceLocation", "destinationLocation", "approvedBy", "dispatchedBy", "receivedBy"})
    List<StockTransfer> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.product", "request", "sourceLocation", "destinationLocation", "approvedBy", "dispatchedBy", "receivedBy"})
    Optional<StockTransfer> findById(Long id);

    long countByStatus(TransferStatus status);
}
