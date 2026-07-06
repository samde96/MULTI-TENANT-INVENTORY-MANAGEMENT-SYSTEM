package com.keen.erp.repo;

import com.keen.erp.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    @EntityGraph(attributePaths = {"items", "items.product", "location", "cashier"})
    List<Sale> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.product", "location", "cashier"})
    List<Sale> findByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt);
}
