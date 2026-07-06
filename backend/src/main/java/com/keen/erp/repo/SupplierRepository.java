package com.keen.erp.repo;

import com.keen.erp.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Supplier> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
