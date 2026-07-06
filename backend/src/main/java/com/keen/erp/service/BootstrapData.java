package com.keen.erp.service;

import com.keen.erp.entity.AppRole;
import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.InventoryBalance;
import com.keen.erp.entity.Location;
import com.keen.erp.entity.LocationType;
import com.keen.erp.entity.NotificationType;
import com.keen.erp.entity.Product;
import com.keen.erp.entity.RequestStatus;
import com.keen.erp.entity.Sale;
import com.keen.erp.entity.SaleItem;
import com.keen.erp.entity.SaleStatus;
import com.keen.erp.entity.StockLedgerEntry;
import com.keen.erp.entity.StockMovementType;
import com.keen.erp.entity.StockRequest;
import com.keen.erp.entity.StockRequestItem;
import com.keen.erp.entity.StockTransfer;
import com.keen.erp.entity.StockTransferItem;
import com.keen.erp.entity.Supplier;
import com.keen.erp.entity.TransferStatus;
import com.keen.erp.repo.AppRoleRepository;
import com.keen.erp.repo.AppUserRepository;
import com.keen.erp.repo.InventoryBalanceRepository;
import com.keen.erp.repo.LocationRepository;
import com.keen.erp.repo.ProductRepository;
import com.keen.erp.repo.SaleRepository;
import com.keen.erp.repo.StockLedgerRepository;
import com.keen.erp.repo.StockRequestRepository;
import com.keen.erp.repo.StockTransferRepository;
import com.keen.erp.repo.SupplierRepository;
import com.keen.erp.service.NotificationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class BootstrapData {

    @Bean
    CommandLineRunner seedData(AppRoleRepository roleRepository,
                               AppUserRepository userRepository,
                               LocationRepository locationRepository,
                               SupplierRepository supplierRepository,
                               ProductRepository productRepository,
                               InventoryBalanceRepository balanceRepository,
                               StockLedgerRepository ledgerRepository,
                               StockRequestRepository requestRepository,
                               StockTransferRepository transferRepository,
                               SaleRepository saleRepository,
                               NotificationService notificationService,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.bootstrap-initial-admin:false}") boolean bootstrapInitialAdmin,
                               @Value("${app.bootstrap-demo-data:false}") boolean bootstrapDemoData,
                               @Value("${ADMIN_USERNAME:}") String adminUsername,
                               @Value("${ADMIN_PASSWORD:}") String adminPassword,
                               @Value("${ADMIN_FULL_NAME:System Administrator}") String adminFullName,
                               @Value("${ADMIN_EMAIL:}") String adminEmail,
                               @Value("${BOOTSTRAP_SAMPLE_PASSWORD:}") String samplePassword) {
        return args -> {
            seedCoreReferenceData(roleRepository, locationRepository);

            if (bootstrapInitialAdmin) {
                seedInitialAdmin(roleRepository, userRepository, passwordEncoder, adminUsername, adminPassword, adminFullName, adminEmail);
                return;
            }

            if (!bootstrapDemoData) {
                if (hasText(adminUsername) && hasText(adminPassword)) {
                    seedInitialAdmin(roleRepository, userRepository, passwordEncoder, adminUsername, adminPassword, adminFullName, adminEmail);
                }
                return;
            }

            if (samplePassword == null || samplePassword.isBlank()) {
                throw new IllegalStateException("BOOTSTRAP_SAMPLE_PASSWORD must be set when BOOTSTRAP_DEMO_DATA=true");
            }

            if (userRepository.count() > 0) {
                return;
            }

            String resolvedAdminUsername = requireConfigured(adminUsername, "ADMIN_USERNAME");
            String resolvedAdminPassword = requireConfigured(adminPassword, "ADMIN_PASSWORD");
            String resolvedAdminFullName = defaultText(adminFullName, "Keen Admin");
            String resolvedAdminEmail = optionalText(adminEmail);

            AppRole adminRole = requireRole(roleRepository, "ADMIN");
            Location warehouse = seedLocation(locationRepository, "WH-MAIN", "Main Warehouse", LocationType.MAIN_WAREHOUSE);
            Location shop = seedLocation(locationRepository, "SHOP-A", "Shop A", LocationType.SHOP);
            Location transit = seedLocation(locationRepository, "TRANSIT", "Transit Area", LocationType.TRANSIT_AREA);
            Location damaged = seedLocation(locationRepository, "DAMAGED", "Damaged Goods Area", LocationType.DAMAGED_GOODS_AREA);

            Supplier supplier = new Supplier();
            supplier.setName("Keen Wholesale Ltd");
            supplier.setContactName("Supply Desk");
            supplier.setPhone("+254700000000");
            supplier.setEmail("supply@keen.local");
            supplier.setActive(true);
            supplierRepository.save(supplier);

            Product sugar = product("1kg Sugar", "SKU-SUGAR-1KG", "BAR-SUGAR-1KG", "Groceries", "Keen", "bag", new BigDecimal("82.00"), new BigDecimal("95.00"), new BigDecimal("90.00"), 15, supplier);
            Product oil = product("2L Cooking Oil", "SKU-OIL-2L", "BAR-OIL-2L", "Groceries", "Keen", "bottle", new BigDecimal("260.00"), new BigDecimal("310.00"), new BigDecimal("295.00"), 10, supplier);
            Product rice = product("5kg Rice", "SKU-RICE-5KG", "BAR-RICE-5KG", "Groceries", "Keen", "bag", new BigDecimal("500.00"), new BigDecimal("620.00"), new BigDecimal("580.00"), 8, supplier);
            productRepository.saveAll(List.of(sugar, oil, rice));

            String encodedSamplePassword = passwordEncoder.encode(samplePassword);
            String encodedAdminPassword = passwordEncoder.encode(resolvedAdminPassword);

            AppUser admin = user(resolvedAdminUsername, resolvedAdminFullName, resolvedAdminEmail, adminRole, encodedAdminPassword, Set.of(warehouse, shop, transit, damaged));
            AppUser manager = user("manager", "Warehouse Manager", "manager@keen.local", requireRole(roleRepository, "STORE_MANAGER"), encodedSamplePassword, Set.of(warehouse, transit));
            AppUser shopManager = user("shopmgr", "Shop Manager", "shop@keen.local", requireRole(roleRepository, "SHOP_MANAGER"), encodedSamplePassword, Set.of(shop));
            AppUser cashier = user("cashier", "Cashier One", "cashier@keen.local", requireRole(roleRepository, "CASHIER"), encodedSamplePassword, Set.of(shop));
            AppUser auditor = user("auditor", "Auditor One", "auditor@keen.local", requireRole(roleRepository, "AUDITOR"), encodedSamplePassword, Set.of(warehouse, shop));
            userRepository.saveAll(List.of(admin, manager, shopManager, cashier, auditor));

            seedBalance(balanceRepository, ledgerRepository, sugar, warehouse, 120, admin);
            seedBalance(balanceRepository, ledgerRepository, oil, warehouse, 90, admin);
            seedBalance(balanceRepository, ledgerRepository, rice, warehouse, 70, admin);
            seedBalance(balanceRepository, ledgerRepository, sugar, shop, 18, admin);
            seedBalance(balanceRepository, ledgerRepository, oil, shop, 12, admin);
            seedBalance(balanceRepository, ledgerRepository, rice, shop, 6, admin);

            StockRequest request = new StockRequest();
            request.setRequestNumber("REQ-SEED-1001");
            request.setSourceLocation(warehouse);
            request.setDestinationLocation(shop);
            request.setRequestedBy(shopManager);
            request.setStatus(RequestStatus.SUBMITTED);
            request.setNote("Initial seeded request");
            StockRequestItem requestItem = new StockRequestItem();
            requestItem.setRequest(request);
            requestItem.setProduct(sugar);
            requestItem.setQuantityRequested(10);
            request.getItems().add(requestItem);
            requestRepository.save(request);

            StockTransfer transfer = new StockTransfer();
            transfer.setTransferNumber("TRF-SEED-1001");
            transfer.setSourceLocation(warehouse);
            transfer.setDestinationLocation(shop);
            transfer.setApprovedBy(manager);
            transfer.setDispatchedBy(manager);
            transfer.setStatus(TransferStatus.IN_TRANSIT);
            transfer.setNote("Seeded transfer in transit");
            StockTransferItem transferItem = new StockTransferItem();
            transferItem.setTransfer(transfer);
            transferItem.setProduct(rice);
            transferItem.setQuantity(8);
            transferItem.setQuantityReceived(0);
            transferItem.setUnitCost(rice.getCostPrice());
            transfer.getItems().add(transferItem);
            transferRepository.save(transfer);

            Sale sale = new Sale();
            sale.setReceiptNumber("RCT-SEED-1001");
            sale.setLocation(shop);
            sale.setCashier(cashier);
            sale.setPaymentMethod("CASH");
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setNote("Seeded sale");

            SaleItem saleItem1 = new SaleItem();
            saleItem1.setSale(sale);
            saleItem1.setProduct(sugar);
            saleItem1.setQuantity(2);
            saleItem1.setUnitPrice(sugar.getSellingPrice());
            saleItem1.setLineTotal(sugar.getSellingPrice().multiply(BigDecimal.valueOf(2)));
            sale.getItems().add(saleItem1);

            SaleItem saleItem2 = new SaleItem();
            saleItem2.setSale(sale);
            saleItem2.setProduct(oil);
            saleItem2.setQuantity(1);
            saleItem2.setUnitPrice(oil.getSellingPrice());
            saleItem2.setLineTotal(oil.getSellingPrice());
            sale.getItems().add(saleItem2);

            sale.setTotalItems(3);
            sale.setTotalAmount(saleItem1.getLineTotal().add(saleItem2.getLineTotal()));
            saleRepository.save(sale);

            balanceRepository.findByProduct_IdAndLocation_Id(sugar.getId(), shop.getId()).ifPresent(balance -> {
                balance.setQuantityOnHand(balance.getQuantityOnHand() - 2);
                balanceRepository.save(balance);
            });
            balanceRepository.findByProduct_IdAndLocation_Id(oil.getId(), shop.getId()).ifPresent(balance -> {
                balance.setQuantityOnHand(balance.getQuantityOnHand() - 1);
                balanceRepository.save(balance);
            });
            ledgerRepository.save(ledger(sugar, shop, null, StockMovementType.SHOP_SALE, 2, "SALE", sale.getReceiptNumber(), "Seeded sale", cashier));
            ledgerRepository.save(ledger(oil, shop, null, StockMovementType.SHOP_SALE, 1, "SALE", sale.getReceiptNumber(), "Seeded sale", cashier));

            notificationService.create(null, "ADMIN", NotificationType.INFO, "EMAIL",
                    "System ready",
                    "Keen ERP demo data has been loaded and the system is ready.",
                    "BOOTSTRAP");
            notificationService.create(null, "SHOP_MANAGER", NotificationType.INFO, "IN_APP",
                    "Seeded transfer",
                    "There is a seeded transfer waiting in transit for Shop A.",
                    "TRF-SEED-1001");
        };
    }

    private void seedCoreReferenceData(AppRoleRepository roleRepository, LocationRepository locationRepository) {
        seedRole(roleRepository, "ADMIN", "Store Owner / Admin", Set.of(
                "admin:all",
                "catalog:view",
                "catalog:manage",
                "inventory:view",
                "inventory:receive",
                "requests:view",
                "requests:manage",
                "transfers:view",
                "transfers:manage",
                "sales:view",
                "sales:manage",
                "reports:view",
                "audit:view",
                "notifications:view"
        ));
        seedRole(roleRepository, "STORE_MANAGER", "Store Manager", Set.of(
                "catalog:view",
                "inventory:view",
                "inventory:receive",
                "requests:view",
                "requests:manage",
                "transfers:view",
                "transfers:manage",
                "reports:view",
                "audit:view",
                "notifications:view"
        ));
        seedRole(roleRepository, "SHOP_MANAGER", "Shop Manager", Set.of(
                "catalog:view",
                "inventory:view",
                "requests:view",
                "requests:manage",
                "transfers:view",
                "transfers:manage",
                "sales:view",
                "sales:manage",
                "reports:view",
                "notifications:view"
        ));
        seedRole(roleRepository, "CASHIER", "Cashier", Set.of(
                "catalog:view",
                "inventory:view",
                "sales:view",
                "sales:manage",
                "reports:view",
                "notifications:view"
        ));
        seedRole(roleRepository, "AUDITOR", "Auditor / Accountant", Set.of(
                "inventory:view",
                "requests:view",
                "transfers:view",
                "sales:view",
                "reports:view",
                "audit:view",
                "notifications:view"
        ));
    }

    private AppRole seedRole(AppRoleRepository roleRepository, String code, String name, Set<String> permissions) {
        AppRole role = roleRepository.findByCode(code).orElseGet(AppRole::new);
        role.setCode(code);
        role.setName(name);
        role.setPermissions(new LinkedHashSet<>(permissions));
        return roleRepository.save(role);
    }

    private Location seedLocation(LocationRepository locationRepository, String code, String name, LocationType type) {
        Location location = locationRepository.findByCode(code).orElseGet(Location::new);
        location.setCode(code);
        location.setName(name);
        location.setType(type);
        location.setActive(true);
        return locationRepository.save(location);
    }

    private AppRole requireRole(AppRoleRepository roleRepository, String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Missing required role: " + code));
    }

    private Location requireLocation(LocationRepository locationRepository, String code) {
        return locationRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Missing required location: " + code));
    }

    private void seedInitialAdmin(AppRoleRepository roleRepository,
                                  AppUserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  String adminUsername,
                                  String adminPassword,
                                  String adminFullName,
                                  String adminEmail) {
        if (userRepository.count() > 0) {
            return;
        }

        String username = requireConfigured(adminUsername, "ADMIN_USERNAME");
        String password = requireConfigured(adminPassword, "ADMIN_PASSWORD");

        AppRole adminRole = requireRole(roleRepository, "ADMIN");

        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setFullName(defaultText(adminFullName, "System Administrator"));
        admin.setEmail(optionalText(adminEmail));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setActive(true);
        admin.setRoles(new LinkedHashSet<>(Set.of(adminRole)));
        admin.setLocations(new LinkedHashSet<>());
        userRepository.save(admin);
    }

    private AppRole role(String code, String name, Set<String> permissions) {
        AppRole role = new AppRole();
        role.setCode(code);
        role.setName(name);
        role.setPermissions(new LinkedHashSet<>(permissions));
        return role;
    }

    private Location location(String code, String name, LocationType type) {
        Location location = new Location();
        location.setCode(code);
        location.setName(name);
        location.setType(type);
        location.setActive(true);
        return location;
    }

    private Product product(String name,
                            String sku,
                            String barcode,
                            String category,
                            String brand,
                            String unit,
                            BigDecimal costPrice,
                            BigDecimal sellingPrice,
                            BigDecimal wholesalePrice,
                            int reorderLevel,
                            Supplier supplier) {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setCategory(category);
        product.setBrand(brand);
        product.setUnitOfMeasure(unit);
        product.setCostPrice(costPrice);
        product.setSellingPrice(sellingPrice);
        product.setWholesalePrice(wholesalePrice);
        product.setReorderLevel(reorderLevel);
        product.setSupplier(supplier);
        product.setActive(true);
        return product;
    }

    private AppUser user(String username,
                         String fullName,
                         String email,
                         AppRole role,
                         String passwordHash,
                         Set<Location> locations) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setActive(true);
        user.setRoles(new LinkedHashSet<>(Set.of(role)));
        user.setLocations(new LinkedHashSet<>(locations));
        return user;
    }

    private String requireConfigured(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set when bootstrapping admin accounts");
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void seedBalance(InventoryBalanceRepository balanceRepository,
                             StockLedgerRepository ledgerRepository,
                             Product product,
                             Location location,
                             int quantity,
                             AppUser actor) {
        InventoryBalance balance = new InventoryBalance();
        balance.setProduct(product);
        balance.setLocation(location);
        balance.setQuantityOnHand(quantity);
        balanceRepository.save(balance);
        ledgerRepository.save(ledger(product, null, location, StockMovementType.INITIAL_ADJUSTMENT, quantity, "SEED", "BOOTSTRAP", "Initial seed balance", actor));
    }

    private StockLedgerEntry ledger(Product product,
                                    Location sourceLocation,
                                    Location destinationLocation,
                                    StockMovementType movementType,
                                    int quantity,
                                    String referenceType,
                                    String referenceNumber,
                                    String note,
                                    AppUser actor) {
        StockLedgerEntry entry = new StockLedgerEntry();
        entry.setProduct(product);
        entry.setSourceLocation(sourceLocation);
        entry.setDestinationLocation(destinationLocation);
        entry.setMovementType(movementType);
        entry.setQuantity(quantity);
        entry.setReferenceType(referenceType);
        entry.setReferenceNumber(referenceNumber);
        entry.setNote(note);
        entry.setActor(actor);
        return entry;
    }
}
