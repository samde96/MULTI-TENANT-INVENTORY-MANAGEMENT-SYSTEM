package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.InventoryBalance;
import com.keen.erp.entity.Location;
import com.keen.erp.entity.Product;
import com.keen.erp.entity.RequestStatus;
import com.keen.erp.entity.Sale;
import com.keen.erp.entity.SaleItem;
import com.keen.erp.entity.SaleStatus;
import com.keen.erp.entity.StockMovementType;
import com.keen.erp.entity.StockRequest;
import com.keen.erp.entity.StockRequestItem;
import com.keen.erp.entity.StockTransfer;
import com.keen.erp.entity.StockTransferItem;
import com.keen.erp.entity.TransferStatus;
import com.keen.erp.entity.NotificationType;
import com.keen.erp.repo.SaleRepository;
import com.keen.erp.repo.StockRequestRepository;
import com.keen.erp.repo.StockTransferRepository;
import com.keen.erp.security.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {

    private final StockRequestRepository stockRequestRepository;
    private final StockTransferRepository stockTransferRepository;
    private final SaleRepository saleRepository;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public WorkflowService(StockRequestRepository stockRequestRepository,
                           StockTransferRepository stockTransferRepository,
                           SaleRepository saleRepository,
                           CatalogService catalogService,
                           InventoryService inventoryService,
                           NotificationService notificationService,
                           AuditService auditService,
                           CurrentUserService currentUserService) {
        this.stockRequestRepository = stockRequestRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.saleRepository = saleRepository;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.StockRequestResponse> listRequests() {
        currentUserService.requireAnyPermission("requests:view", "requests:manage");
        boolean admin = currentUserService.isAdmin();
        var allowedLocationIds = currentUserService.currentUserLocationIds();
        return stockRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(request -> admin
                        || (request.getSourceLocation() != null && allowedLocationIds.contains(request.getSourceLocation().getId()))
                        || (request.getDestinationLocation() != null && allowedLocationIds.contains(request.getDestinationLocation().getId())))
                .map(this::toStockRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.StockTransferResponse> listTransfers() {
        currentUserService.requireAnyPermission("transfers:view", "transfers:manage");
        boolean admin = currentUserService.isAdmin();
        var allowedLocationIds = currentUserService.currentUserLocationIds();
        return stockTransferRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(transfer -> admin
                        || (transfer.getSourceLocation() != null && allowedLocationIds.contains(transfer.getSourceLocation().getId()))
                        || (transfer.getDestinationLocation() != null && allowedLocationIds.contains(transfer.getDestinationLocation().getId())))
                .map(this::toStockTransferResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.SaleResponse> listSales() {
        currentUserService.requireAnyPermission("sales:view", "sales:manage");
        boolean admin = currentUserService.isAdmin();
        var allowedLocationIds = currentUserService.currentUserLocationIds();
        return saleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(sale -> admin
                        || (sale.getLocation() != null && allowedLocationIds.contains(sale.getLocation().getId())))
                .map(this::toSaleResponse)
                .toList();
    }

    @Transactional
    public ApiDtos.StockRequestResponse createRequest(ApiDtos.StockRequestCreateRequest request) {
        currentUserService.requirePermission("requests:manage");
        Location source = catalogService.getLocation(request.sourceLocationId());
        Location destination = catalogService.getLocation(request.destinationLocationId());
        currentUserService.requireLocationAccess(destination);

        StockRequest stockRequest = new StockRequest();
        stockRequest.setRequestNumber(code("REQ"));
        stockRequest.setSourceLocation(source);
        stockRequest.setDestinationLocation(destination);
        stockRequest.setRequestedBy(currentUserService.currentUser());
        stockRequest.setStatus(RequestStatus.DRAFT);
        stockRequest.setNote(request.note());

        for (ApiDtos.StockRequestItemRequest itemRequest : request.items()) {
            Product product = catalogService.getProduct(itemRequest.productId());
            StockRequestItem item = new StockRequestItem();
            item.setRequest(stockRequest);
            item.setProduct(product);
            item.setQuantityRequested(itemRequest.quantityRequested());
            stockRequest.getItems().add(item);
        }

        StockRequest saved = stockRequestRepository.save(stockRequest);
        auditService.record("CREATE", "StockRequest", String.valueOf(saved.getId()), null, saved.getRequestNumber(), true);
        return toStockRequestResponse(saved);
    }

    @Transactional
    public ApiDtos.StockRequestResponse submitRequest(Long requestId, String note) {
        currentUserService.requirePermission("requests:manage");
        StockRequest request = stockRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Stock request not found"));
        currentUserService.requireLocationAccess(request.getDestinationLocation());
        request.setStatus(RequestStatus.SUBMITTED);
        if (note != null && !note.isBlank()) {
            request.setNote(note);
        }
        StockRequest saved = stockRequestRepository.save(request);
        notificationService.notifyRole("STORE_MANAGER", NotificationType.INFO,
                "Stock request submitted",
                "Request " + saved.getRequestNumber() + " has been submitted for approval.",
                saved.getRequestNumber());
        auditService.record("SUBMIT", "StockRequest", String.valueOf(saved.getId()), null, saved.getStatus().name(), true);
        return toStockRequestResponse(saved);
    }

    @Transactional
    public ApiDtos.StockTransferResponse approveRequest(Long requestId) {
        currentUserService.requirePermission("requests:manage");
        StockRequest request = stockRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Stock request not found"));
        currentUserService.requireLocationAccess(request.getSourceLocation());

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(currentUserService.currentUser());
        stockRequestRepository.save(request);

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber(code("TRF"));
        transfer.setRequest(request);
        transfer.setSourceLocation(request.getSourceLocation());
        transfer.setDestinationLocation(request.getDestinationLocation());
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setNote(request.getNote());

        for (StockRequestItem requestItem : request.getItems()) {
            StockTransferItem transferItem = new StockTransferItem();
            transferItem.setTransfer(transfer);
            transferItem.setProduct(requestItem.getProduct());
            transferItem.setQuantity(requestItem.getQuantityRequested());
            transferItem.setQuantityReceived(0);
            transferItem.setUnitCost(requestItem.getProduct().getCostPrice());
            transfer.getItems().add(transferItem);
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        notificationService.notifyRole("SHOP_MANAGER", NotificationType.INFO,
                "Stock request approved",
                "Request " + request.getRequestNumber() + " was approved and transfer " + saved.getTransferNumber() + " was created.",
                saved.getTransferNumber());
        auditService.record("APPROVE", "StockRequest", String.valueOf(request.getId()), null, saved.getTransferNumber(), true);
        return toStockTransferResponse(saved);
    }

    @Transactional
    public ApiDtos.StockRequestResponse rejectRequest(Long requestId, String note) {
        currentUserService.requirePermission("requests:manage");
        StockRequest request = stockRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Stock request not found"));
        currentUserService.requireLocationAccess(request.getDestinationLocation());
        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(currentUserService.currentUser());
        if (note != null && !note.isBlank()) {
            request.setNote(note);
        }
        StockRequest saved = stockRequestRepository.save(request);
        notificationService.notifyRole("SHOP_MANAGER", NotificationType.WARNING,
                "Stock request rejected",
                "Request " + saved.getRequestNumber() + " was rejected.",
                saved.getRequestNumber());
        auditService.record("REJECT", "StockRequest", String.valueOf(saved.getId()), null, saved.getStatus().name(), true);
        return toStockRequestResponse(saved);
    }

    @Transactional
    public ApiDtos.StockTransferResponse createTransfer(ApiDtos.TransferCreateRequest request) {
        currentUserService.requirePermission("transfers:manage");
        Location source = catalogService.getLocation(request.sourceLocationId());
        Location destination = catalogService.getLocation(request.destinationLocationId());
        currentUserService.requireLocationAccess(source);

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber(code("TRF"));
        transfer.setSourceLocation(source);
        transfer.setDestinationLocation(destination);
        transfer.setStatus(TransferStatus.DRAFT);
        transfer.setNote(request.note());
        if (request.requestId() != null) {
            StockRequest linkedRequest = stockRequestRepository.findById(request.requestId())
                    .orElseThrow(() -> new EntityNotFoundException("Stock request not found"));
            transfer.setRequest(linkedRequest);
        }

        for (ApiDtos.TransferItemRequest itemRequest : request.items()) {
            Product product = catalogService.getProduct(itemRequest.productId());
            StockTransferItem item = new StockTransferItem();
            item.setTransfer(transfer);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setQuantityReceived(0);
            item.setUnitCost(itemRequest.unitCost() == null ? product.getCostPrice() : itemRequest.unitCost());
            transfer.getItems().add(item);
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        auditService.record("CREATE", "StockTransfer", String.valueOf(saved.getId()), null, saved.getTransferNumber(), true);
        return toStockTransferResponse(saved);
    }

    @Transactional
    public ApiDtos.StockTransferResponse approveTransfer(Long transferId) {
        currentUserService.requirePermission("transfers:manage");
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found"));
        currentUserService.requireLocationAccess(transfer.getSourceLocation());
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(currentUserService.currentUser());
        StockTransfer saved = stockTransferRepository.save(transfer);
        auditService.record("APPROVE", "StockTransfer", String.valueOf(saved.getId()), null, saved.getStatus().name(), true);
        return toStockTransferResponse(saved);
    }

    @Transactional
    public ApiDtos.StockTransferResponse dispatchTransfer(Long transferId) {
        currentUserService.requirePermission("transfers:manage");
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found"));
        currentUserService.requireLocationAccess(transfer.getSourceLocation());
        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw new IllegalStateException("Transfer cannot be dispatched from status " + transfer.getStatus());
        }

        Location transitLocation = getTransitLocation();
        AppUser user = currentUserService.currentUser();
        for (StockTransferItem item : transfer.getItems()) {
            inventoryService.adjustBalance(item.getProduct(), transfer.getSourceLocation(), -item.getQuantity());
            inventoryService.adjustBalance(item.getProduct(), transitLocation, item.getQuantity());
            inventoryService.recordLedgerEntry(
                    item.getProduct(),
                    transfer.getSourceLocation(),
                    transitLocation,
                    StockMovementType.TRANSFER_IN_TRANSIT,
                    item.getQuantity(),
                    "TRANSFER",
                    transfer.getTransferNumber(),
                    transfer.getNote()
            );
        }
        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setDispatchedBy(user);
        transfer.setSentAt(Instant.now());
        StockTransfer saved = stockTransferRepository.save(transfer);
        notificationService.notifyRole("SHOP_MANAGER", NotificationType.INFO,
                "Transfer dispatched",
                "Transfer " + saved.getTransferNumber() + " is now in transit.",
                saved.getTransferNumber());
        auditService.record("DISPATCH", "StockTransfer", String.valueOf(saved.getId()), null, saved.getStatus().name(), true);
        return toStockTransferResponse(saved);
    }

    @Transactional
    public ApiDtos.StockTransferResponse receiveTransfer(Long transferId, ApiDtos.TransferReceiveRequest request) {
        currentUserService.requirePermission("transfers:manage");
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found"));
        currentUserService.requireLocationAccess(transfer.getDestinationLocation());
        if (transfer.getStatus() != TransferStatus.IN_TRANSIT && transfer.getStatus() != TransferStatus.DISPATCHED) {
            throw new IllegalStateException("Transfer cannot be received from status " + transfer.getStatus());
        }

        Location transitLocation = getTransitLocation();
        Map<Long, Integer> receivedQuantities = new LinkedHashMap<>();
        for (ApiDtos.TransferReceiveItemRequest itemRequest : request.items()) {
            receivedQuantities.put(itemRequest.productId(), itemRequest.quantityReceived());
        }

        boolean fullyReceived = true;
        for (StockTransferItem item : transfer.getItems()) {
            int receivedQuantity = receivedQuantities.getOrDefault(item.getProduct().getId(), item.getQuantity());
            if (receivedQuantity < 0 || receivedQuantity > item.getQuantity()) {
                throw new IllegalArgumentException("Invalid received quantity for " + item.getProduct().getName());
            }
            if (receivedQuantity != item.getQuantity()) {
                fullyReceived = false;
            }
            if (receivedQuantity > 0) {
                inventoryService.adjustBalance(item.getProduct(), transitLocation, -receivedQuantity);
                inventoryService.adjustBalance(item.getProduct(), transfer.getDestinationLocation(), receivedQuantity);
                inventoryService.recordLedgerEntry(
                        item.getProduct(),
                        transitLocation,
                        transfer.getDestinationLocation(),
                        StockMovementType.TRANSFER_RECEIVED,
                        receivedQuantity,
                        "TRANSFER",
                        transfer.getTransferNumber(),
                        request.note()
                );
            }
            item.setQuantityReceived(receivedQuantity);
        }

        transfer.setReceivedBy(currentUserService.currentUser());
        transfer.setReceivedAt(Instant.now());
        transfer.setStatus(fullyReceived ? TransferStatus.RECEIVED : TransferStatus.PARTIALLY_RECEIVED);
        transfer.setNote(request.note() == null || request.note().isBlank() ? transfer.getNote() : request.note());
        StockTransfer saved = stockTransferRepository.save(transfer);
        notificationService.notifyRole("ADMIN", NotificationType.INFO,
                "Transfer received",
                "Transfer " + saved.getTransferNumber() + " was received with status " + saved.getStatus() + ".",
                saved.getTransferNumber());
        auditService.record("RECEIVE", "StockTransfer", String.valueOf(saved.getId()), null, saved.getStatus().name(), true);
        return toStockTransferResponse(saved);
    }

    @Transactional
    public ApiDtos.SaleResponse createSale(ApiDtos.SaleCreateRequest request) {
        currentUserService.requirePermission("sales:manage");
        Location location = catalogService.getLocation(request.locationId());
        currentUserService.requireLocationAccess(location);

        Sale sale = new Sale();
        sale.setReceiptNumber(code("RCT"));
        sale.setLocation(location);
        sale.setCashier(currentUserService.currentUser());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setNote(request.note());
        sale.setStatus(SaleStatus.COMPLETED);

        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;
        for (ApiDtos.SaleItemRequest itemRequest : request.items()) {
            Product product = catalogService.getProduct(itemRequest.productId());
            InventoryBalance balance = inventoryService.getBalance(product, location);
            if (balance.getQuantityOnHand() < itemRequest.quantity()) {
                throw new IllegalStateException("Insufficient stock for " + product.getName() + " at " + location.getName());
            }
            BigDecimal unitPrice = itemRequest.unitPrice() == null ? product.getSellingPrice() : itemRequest.unitPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity())).setScale(2, RoundingMode.HALF_UP);

            inventoryService.adjustBalance(product, location, -itemRequest.quantity());
            inventoryService.recordLedgerEntry(
                    product,
                    location,
                    null,
                    StockMovementType.SHOP_SALE,
                    itemRequest.quantity(),
                    "SALE",
                    sale.getReceiptNumber(),
                    request.note()
            );

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(itemRequest.quantity());
            saleItem.setUnitPrice(unitPrice);
            saleItem.setLineTotal(lineTotal);
            sale.getItems().add(saleItem);

            total = total.add(lineTotal);
            itemCount += itemRequest.quantity();
        }

        sale.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        sale.setTotalItems(itemCount);
        Sale saved = saleRepository.save(sale);
        auditService.record("SALE", "Sale", String.valueOf(saved.getId()), null, saved.getReceiptNumber(), true);
        notificationService.notifyRole("ADMIN", NotificationType.SUCCESS,
                "Sale completed",
                "Receipt " + saved.getReceiptNumber() + " was completed for " + location.getName() + ".",
                saved.getReceiptNumber());
        return toSaleResponse(saved);
    }

    private Location getTransitLocation() {
        return catalogService.listLocations().stream()
                .filter(location -> location.getType() == com.keen.erp.entity.LocationType.TRANSIT_AREA)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Transit location not configured"));
    }

    private String code(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

    private ApiDtos.SaleItemResponse toSaleItemResponse(SaleItem item) {
        return new ApiDtos.SaleItemResponse(
                item.getId(),
                toProductSummary(item.getProduct()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }

    private ApiDtos.SaleResponse toSaleResponse(Sale sale) {
        return new ApiDtos.SaleResponse(
                sale.getId(),
                sale.getReceiptNumber(),
                toLocationResponse(sale.getLocation()),
                toUserSummary(sale.getCashier()),
                sale.getStatus(),
                sale.getPaymentMethod(),
                sale.getTotalAmount(),
                sale.getTotalItems(),
                sale.getNote(),
                sale.getCreatedAt(),
                sale.getUpdatedAt(),
                sale.getItems().stream().map(this::toSaleItemResponse).toList()
        );
    }

    private ApiDtos.StockRequestItemResponse toStockRequestItemResponse(StockRequestItem item) {
        return new ApiDtos.StockRequestItemResponse(
                item.getId(),
                toProductSummary(item.getProduct()),
                item.getQuantityRequested()
        );
    }

    private ApiDtos.StockRequestResponse toStockRequestResponse(StockRequest request) {
        return new ApiDtos.StockRequestResponse(
                request.getId(),
                request.getRequestNumber(),
                toLocationResponse(request.getSourceLocation()),
                toLocationResponse(request.getDestinationLocation()),
                request.getStatus(),
                request.getNote(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getItems().stream().map(this::toStockRequestItemResponse).toList()
        );
    }

    private ApiDtos.StockTransferItemResponse toStockTransferItemResponse(StockTransferItem item) {
        return new ApiDtos.StockTransferItemResponse(
                item.getId(),
                toProductSummary(item.getProduct()),
                item.getQuantity(),
                item.getQuantityReceived(),
                item.getUnitCost()
        );
    }

    private ApiDtos.StockTransferResponse toStockTransferResponse(StockTransfer transfer) {
        return new ApiDtos.StockTransferResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                toLocationResponse(transfer.getSourceLocation()),
                toLocationResponse(transfer.getDestinationLocation()),
                transfer.getStatus(),
                transfer.getNote(),
                transfer.getSentAt(),
                transfer.getReceivedAt(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                transfer.getItems().stream().map(this::toStockTransferItemResponse).toList()
        );
    }
}
