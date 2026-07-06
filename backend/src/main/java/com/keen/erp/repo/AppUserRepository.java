package com.keen.erp.repo;

import com.keen.erp.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "locations"})
    Optional<AppUser> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "locations"})
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "locations"})
    List<AppUser> findAllByActiveFalseOrderByCreatedAtDesc();
}
