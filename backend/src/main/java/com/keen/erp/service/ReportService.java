package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.entity.InventoryBalance;
import com.keen.erp.entity.Location;
import com.keen.erp.entity.RequestStatus;
import com.keen.erp.entity.Sale;
import com.keen.erp.entity.TransferStatus;
import com.keen.erp.repo.InventoryBalanceRepository;
import com.keen.erp.repo.LocationRepository;
import com.keen.erp.repo.ProductRepository;
import com.keen.erp.repo.SaleRepository;
import com.keen.erp.repo.StockRequestRepository;
import com.keen.erp.repo.StockTransferRepository;
import com.keen.erp.security.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final SaleRepository saleRepository;
    private final StockRequestRepository stockRequestRepository;
    private final StockTransferRepository stockTransferRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public ReportService(InventoryBalanceRepository inventoryBalanceRepository,
                         SaleRepository saleRepository,
                         StockRequestRepository stockRequestRepository,
                         StockTransferRepository stockTransferRepository,
                         ProductRepository productRepository,
                         LocationRepository locationRepository,
                         NotificationService notificationService,
                         CurrentUserService currentUserService) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.saleRepository = saleRepository;
        this.stockRequestRepository = stockRequestRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ApiDtos.DashboardSummaryResponse dashboard() {
        currentUserService.requirePermission("reports:view");
        boolean admin = currentUserService.isAdmin();
        Set<Long> allowedLocationIds = currentUserService.currentUserLocationIds();

        List<InventoryBalance> balances = inventoryBalanceRepository.findAll().stream()
                .filter(balance -> admin || (balance.getLocation() != null && allowedLocationIds.contains(balance.getLocation().getId())))
                .toList();

        BigDecimal totalStockValue = balances.stream()
                .filter(balance -> balance.getProduct() != null)
                .map(balance -> balance.getProduct().getCostPrice().multiply(BigDecimal.valueOf(balance.getQuantityOnHand())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant startOfToday = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Sale> salesToday = saleRepository.findByCreatedAtAfterOrderByCreatedAtDesc(startOfToday);
        if (!admin) {
            salesToday = salesToday.stream()
                    .filter(sale -> sale.getLocation() != null && allowedLocationIds.contains(sale.getLocation().getId()))
                    .toList();
        }
        BigDecimal totalSalesToday = salesToday.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeProducts = admin
                ? productRepository.findAll().stream().filter(product -> product.isActive()).count()
                : balances.stream()
                .filter(balance -> balance.getProduct() != null && balance.getProduct().isActive())
                .map(balance -> balance.getProduct().getId())
                .distinct()
                .count();
        long totalLocations = admin ? locationRepository.count() : allowedLocationIds.size();
        long pendingRequests = stockRequestRepository.findAll().stream()
                .filter(request -> request.getStatus() == RequestStatus.SUBMITTED)
                .filter(request -> admin
                        || (request.getSourceLocation() != null && allowedLocationIds.contains(request.getSourceLocation().getId()))
                        || (request.getDestinationLocation() != null && allowedLocationIds.contains(request.getDestinationLocation().getId())))
                .count();
        long inTransitTransfers = stockTransferRepository.findAll().stream()
                .filter(transfer -> transfer.getStatus() == TransferStatus.IN_TRANSIT || transfer.getStatus() == TransferStatus.DISPATCHED)
                .filter(transfer -> admin
                        || (transfer.getSourceLocation() != null && allowedLocationIds.contains(transfer.getSourceLocation().getId()))
                        || (transfer.getDestinationLocation() != null && allowedLocationIds.contains(transfer.getDestinationLocation().getId())))
                .count();
        long lowStockBalances = balances.stream()
                .filter(balance -> balance.getProduct() != null)
                .filter(balance -> balance.getQuantityOnHand() <= balance.getProduct().getReorderLevel())
                .count();
        long unreadNotifications = notificationService.countUnreadVisible();

        return new ApiDtos.DashboardSummaryResponse(
                totalStockValue.setScale(2, RoundingMode.HALF_UP),
                totalSalesToday.setScale(2, RoundingMode.HALF_UP),
                activeProducts,
                totalLocations,
                pendingRequests,
                inTransitTransfers,
                lowStockBalances,
                salesToday.size(),
                unreadNotifications
        );
    }
}
