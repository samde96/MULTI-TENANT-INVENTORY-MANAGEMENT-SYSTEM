package com.keen.erp.repo;

import com.keen.erp.entity.RequestStatus;
import com.keen.erp.entity.StockRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {
    @EntityGraph(attributePaths = {"items", "items.product", "sourceLocation", "destinationLocation", "requestedBy", "reviewedBy"})
    List<StockRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.product", "sourceLocation", "destinationLocation", "requestedBy", "reviewedBy"})
    Optional<StockRequest> findById(Long id);

    long countByStatus(RequestStatus status);
}
