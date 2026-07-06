package com.keen.erp.repo;

import com.keen.erp.entity.InventoryBalance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {
    @EntityGraph(attributePaths = {"product", "location"})
    Optional<InventoryBalance> findByProduct_IdAndLocation_Id(Long productId, Long locationId);

    @EntityGraph(attributePaths = {"product", "location"})
    List<InventoryBalance> findByLocation_IdOrderByProduct_NameAsc(Long locationId);

    @EntityGraph(attributePaths = {"product", "location"})
    List<InventoryBalance> findByProduct_IdOrderByLocation_NameAsc(Long productId);

    @EntityGraph(attributePaths = {"product", "location"})
    List<InventoryBalance> findAllByOrderByLocation_NameAscProduct_NameAsc();
}
