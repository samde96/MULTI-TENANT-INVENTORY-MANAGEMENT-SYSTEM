package com.keen.erp.dto;

import com.keen.erp.entity.LocationType;
import com.keen.erp.entity.RequestStatus;
import com.keen.erp.entity.SaleStatus;
import com.keen.erp.entity.StockMovementType;
import com.keen.erp.entity.TransferStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 80) String username,
            @NotBlank @Size(min = 3, max = 160) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    public record RegistrationResponse(Long userId, String username, boolean pendingApproval, String message) {
    }

    public record PendingUserResponse(
            Long id,
            String username,
            String fullName,
            String email,
            Instant createdAt
    ) {
    }

    public record ApprovalOptionsResponse(List<RoleResponse> roles, List<LocationResponse> locations) {
    }

    public record ApproveUserRequest(
            @NotEmpty List<Long> roleIds,
            List<Long> locationIds
    ) {
    }

    public record ApprovalResponse(
            Long userId,
            String username,
            boolean active,
            String message
    ) {
    }

    public record RoleResponse(Long id, String code, String name, Set<String> permissions) {
    }

    public record LocationResponse(Long id, String code, String name, LocationType type, boolean active) {
    }

    public record UserProfileResponse(
            Long id,
            String username,
            String fullName,
            String email,
            Set<String> permissions,
            List<RoleResponse> roles,
            List<LocationResponse> locations
    ) {
    }

    public record AuthResponse(String token, UserProfileResponse user) {
    }

    public record UserSummaryResponse(Long id, String username, String fullName) {
    }

    public record ProductSummaryResponse(
            Long id,
            String name,
            String sku,
            BigDecimal costPrice,
            BigDecimal sellingPrice,
            Integer reorderLevel
    ) {
    }

    public record SaleItemResponse(
            Long id,
            ProductSummaryResponse product,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record SaleResponse(
            Long id,
            String receiptNumber,
            LocationResponse location,
            UserSummaryResponse cashier,
            SaleStatus status,
            String paymentMethod,
            BigDecimal totalAmount,
            int totalItems,
            String note,
            Instant createdAt,
            Instant updatedAt,
            List<SaleItemResponse> items
    ) {
    }

    public record StockRequestItemResponse(
            Long id,
            ProductSummaryResponse product,
            int quantityRequested
    ) {
    }

    public record StockRequestResponse(
            Long id,
            String requestNumber,
            LocationResponse sourceLocation,
            LocationResponse destinationLocation,
            RequestStatus status,
            String note,
            Instant createdAt,
            Instant updatedAt,
            List<StockRequestItemResponse> items
    ) {
    }

    public record StockTransferItemResponse(
            Long id,
            ProductSummaryResponse product,
            int quantity,
            int quantityReceived,
            BigDecimal unitCost
    ) {
    }

    public record StockTransferResponse(
            Long id,
            String transferNumber,
            LocationResponse sourceLocation,
            LocationResponse destinationLocation,
            TransferStatus status,
            String note,
            Instant sentAt,
            Instant receivedAt,
            Instant createdAt,
            Instant updatedAt,
            List<StockTransferItemResponse> items
    ) {
    }

    public record StockLedgerEntryResponse(
            Long id,
            ProductSummaryResponse product,
            LocationResponse sourceLocation,
            LocationResponse destinationLocation,
            StockMovementType movementType,
            int quantity,
            String referenceType,
            String referenceNumber,
            String note,
            UserSummaryResponse actor,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record InventoryBalanceResponse(
            Long id,
            ProductSummaryResponse product,
            LocationResponse location,
            int quantityOnHand,
            Instant updatedAt
    ) {
    }

    public record SupplierRequest(
            @NotBlank String name,
            String contactName,
            String phone,
            String email,
            boolean active
    ) {
    }

    public record UploadResponse(String url, String fileName) {
    }

    public record LocationRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull LocationType type,
            boolean active
    ) {
    }

    public record ProductRequest(
            @NotBlank String name,
            @NotBlank String sku,
            String barcode,
            String category,
            String brand,
            String unitOfMeasure,
            @NotNull BigDecimal costPrice,
            @NotNull BigDecimal sellingPrice,
            BigDecimal wholesalePrice,
            Integer reorderLevel,
            Long supplierId,
            boolean active,
            String imageUrl
    ) {
    }

    public record ReceiveStockRequest(
            @NotNull Long productId,
            @NotNull Long locationId,
            @Positive int quantity,
            @NotNull BigDecimal costPrice,
            String note
    ) {
    }

    public record StockRequestItemRequest(
            @NotNull Long productId,
            @Positive int quantityRequested
    ) {
    }

    public record StockRequestCreateRequest(
            @NotNull Long sourceLocationId,
            @NotNull Long destinationLocationId,
            @NotEmpty List<@Valid StockRequestItemRequest> items,
            String note
    ) {
    }

    public record StockRequestActionRequest(String note) {
    }

    public record TransferItemRequest(
            @NotNull Long productId,
            @Positive int quantity,
            BigDecimal unitCost
    ) {
    }

    public record TransferCreateRequest(
            @NotNull Long sourceLocationId,
            @NotNull Long destinationLocationId,
            Long requestId,
            @NotEmpty List<@Valid TransferItemRequest> items,
            String note
    ) {
    }

    public record TransferReceiveItemRequest(
            @NotNull Long productId,
            @Positive int quantityReceived
    ) {
    }

    public record TransferReceiveRequest(
            @NotEmpty List<@Valid TransferReceiveItemRequest> items,
            String note
    ) {
    }

    public record SaleItemRequest(
            @NotNull Long productId,
            @Positive int quantity,
            BigDecimal unitPrice
    ) {
    }

    public record SaleCreateRequest(
            @NotNull Long locationId,
            @NotBlank String paymentMethod,
            @NotEmpty List<@Valid SaleItemRequest> items,
            String note
    ) {
    }

    public record DashboardSummaryResponse(
            BigDecimal totalStockValue,
            BigDecimal totalSalesToday,
            long activeProducts,
            long totalLocations,
            long pendingRequests,
            long inTransitTransfers,
            long lowStockBalances,
            long completedSalesToday,
            long unreadNotifications
    ) {
    }
}
