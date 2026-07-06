package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.InventoryBalance;
import com.keen.erp.entity.Location;
import com.keen.erp.entity.Product;
import com.keen.erp.entity.StockLedgerEntry;
import com.keen.erp.entity.StockMovementType;
import com.keen.erp.repo.InventoryBalanceRepository;
import com.keen.erp.repo.StockLedgerRepository;
import com.keen.erp.security.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class InventoryService {

    private final InventoryBalanceRepository balanceRepository;
    private final StockLedgerRepository ledgerRepository;
    private final CatalogService catalogService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public InventoryService(InventoryBalanceRepository balanceRepository,
                            StockLedgerRepository ledgerRepository,
                            CatalogService catalogService,
                            CurrentUserService currentUserService,
                            AuditService auditService) {
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.catalogService = catalogService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.InventoryBalanceResponse> listBalances(Long locationId, Long productId) {
        currentUserService.requireAnyPermission("inventory:view", "inventory:receive");
        boolean admin = currentUserService.isAdmin();
        var allowedLocationIds = currentUserService.currentUserLocationIds();
        List<InventoryBalance> balances;
        if (locationId != null) {
            Location location = catalogService.getLocation(locationId);
            currentUserService.requireLocationAccess(location);
            balances = balanceRepository.findByLocation_IdOrderByProduct_NameAsc(locationId);
        } else if (productId != null) {
            balances = balanceRepository.findByProduct_IdOrderByLocation_NameAsc(productId).stream()
                    .filter(balance -> admin || allowedLocationIds.contains(balance.getLocation().getId()))
                    .toList();
        } else if (admin) {
            balances = balanceRepository.findAllByOrderByLocation_NameAscProduct_NameAsc();
        } else {
            balances = balanceRepository.findAllByOrderByLocation_NameAscProduct_NameAsc().stream()
                    .filter(balance -> allowedLocationIds.contains(balance.getLocation().getId()))
                    .toList();
        }
        return balances.stream()
                .map(this::toBalanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.StockLedgerEntryResponse> listLedger() {
        currentUserService.requireAnyPermission("inventory:view", "inventory:receive");
        boolean admin = currentUserService.isAdmin();
        var allowedLocationIds = currentUserService.currentUserLocationIds();
        return ledgerRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(entry -> admin
                        || (entry.getSourceLocation() != null && allowedLocationIds.contains(entry.getSourceLocation().getId()))
                        || (entry.getDestinationLocation() != null && allowedLocationIds.contains(entry.getDestinationLocation().getId())))
                .map(this::toLedgerResponse)
                .toList();
    }

    @Transactional
    public InventoryBalance receiveStock(ApiDtos.ReceiveStockRequest request) {
        currentUserService.requirePermission("inventory:receive");
        Product product = catalogService.getProduct(request.productId());
        Location location = catalogService.getLocation(request.locationId());
        currentUserService.requireLocationAccess(location);

        product.setCostPrice(request.costPrice());

        InventoryBalance balance = adjustBalance(product, location, request.quantity());
        recordLedgerEntry(product, null, location, StockMovementType.SUPPLIER_RECEIVED, request.quantity(),
                "INVENTORY_RECEIPT", "RCV-" + Instant.now().toEpochMilli(), request.note());
        auditService.record("RECEIVE_STOCK", "InventoryBalance", String.valueOf(balance.getId()), null, String.valueOf(balance.getQuantityOnHand()), true);
        return balance;
    }

    @Transactional
    public InventoryBalance adjustBalance(Product product, Location location, int delta) {
        InventoryBalance balance = balanceRepository.findByProduct_IdAndLocation_Id(product.getId(), location.getId())
                .orElseGet(() -> {
                    InventoryBalance created = new InventoryBalance();
                    created.setProduct(product);
                    created.setLocation(location);
                    created.setQuantityOnHand(0);
                    return created;
                });
        int updatedQuantity = balance.getQuantityOnHand() + delta;
        if (updatedQuantity < 0) {
            throw new IllegalStateException("Insufficient stock for " + product.getName() + " at " + location.getName());
        }
        balance.setQuantityOnHand(updatedQuantity);
        return balanceRepository.save(balance);
    }

    @Transactional
    public void moveStock(Product product,
                          Location sourceLocation,
                          Location destinationLocation,
                          int quantity,
                          StockMovementType movementType,
                          String referenceType,
                          String referenceNumber,
                          String note) {
        currentUserService.requireLocationAccess(sourceLocation);
        adjustBalance(product, sourceLocation, -quantity);
        adjustBalance(product, destinationLocation, quantity);
        recordLedgerEntry(product, sourceLocation, destinationLocation, movementType, quantity, referenceType, referenceNumber, note);
    }

    public InventoryBalance getBalance(Product product, Location location) {
        return balanceRepository.findByProduct_IdAndLocation_Id(product.getId(), location.getId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory balance not found"));
    }

    public StockLedgerEntry recordLedgerEntry(Product product,
                                              Location sourceLocation,
                                              Location destinationLocation,
                                              StockMovementType movementType,
                                              int quantity,
                                              String referenceType,
                                              String referenceNumber,
                                              String note) {
        StockLedgerEntry entry = new StockLedgerEntry();
        entry.setProduct(product);
        entry.setSourceLocation(sourceLocation);
        entry.setDestinationLocation(destinationLocation);
        entry.setMovementType(movementType);
        entry.setQuantity(quantity);
        entry.setReferenceType(referenceType);
        entry.setReferenceNumber(referenceNumber);
        entry.setNote(note);
        entry.setActor(currentUserService.currentUser());
        return ledgerRepository.save(entry);
    }

    public List<InventoryBalance> lowStockBalances() {
        return balanceRepository.findAll().stream()
                .filter(balance -> balance.getProduct() != null)
                .filter(balance -> balance.getQuantityOnHand() <= Objects.requireNonNullElse(balance.getProduct().getReorderLevel(), 0))
                .sorted(Comparator.comparing((InventoryBalance balance) -> balance.getLocation().getName())
                        .thenComparing(balance -> balance.getProduct().getName()))
                .toList();
    }

    private ApiDtos.LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return new ApiDtos.LocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getType(),
                location.isActive()
        );
    }

    private ApiDtos.UserSummaryResponse toUserSummary(AppUser user) {
        if (user == null) {
            return null;
        }
        return new ApiDtos.UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName()
        );
    }

    private ApiDtos.ProductSummaryResponse toProductSummary(Product product) {
        if (product == null) {
            return null;
        }
        return new ApiDtos.ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCostPrice(),
                product.getSellingPrice(),
                product.getReorderLevel()
        );
    }

    private ApiDtos.StockLedgerEntryResponse toLedgerResponse(StockLedgerEntry entry) {
        return new ApiDtos.StockLedgerEntryResponse(
                entry.getId(),
                toProductSummary(entry.getProduct()),
                toLocationResponse(entry.getSourceLocation()),
                toLocationResponse(entry.getDestinationLocation()),
                entry.getMovementType(),
                entry.getQuantity(),
                entry.getReferenceType(),
                entry.getReferenceNumber(),
                entry.getNote(),
                toUserSummary(entry.getActor()),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private ApiDtos.InventoryBalanceResponse toBalanceResponse(InventoryBalance balance) {
        return new ApiDtos.InventoryBalanceResponse(
                balance.getId(),
                toProductSummary(balance.getProduct()),
                toLocationResponse(balance.getLocation()),
                balance.getQuantityOnHand(),
                balance.getUpdatedAt()
        );
    }
}
