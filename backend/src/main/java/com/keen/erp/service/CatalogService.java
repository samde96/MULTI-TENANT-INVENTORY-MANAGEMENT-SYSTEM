package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.Location;
import com.keen.erp.entity.Product;
import com.keen.erp.entity.Supplier;
import com.keen.erp.repo.LocationRepository;
import com.keen.erp.repo.ProductRepository;
import com.keen.erp.repo.SupplierRepository;
import com.keen.erp.security.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final LocationRepository locationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public CatalogService(ProductRepository productRepository,
                          SupplierRepository supplierRepository,
                          LocationRepository locationRepository,
                          CurrentUserService currentUserService,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.locationRepository = locationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public List<Product> listProducts() {
        currentUserService.requireAnyPermission("catalog:view", "catalog:manage");
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    public Product createProduct(ApiDtos.ProductRequest request) {
        currentUserService.requirePermission("catalog:manage");

        Product product = new Product();
        applyProductRequest(product, request, true);
        Product saved = productRepository.save(product);
        auditService.record("CREATE", "Product", String.valueOf(saved.getId()), null, saved.getSku(), true);
        return saved;
    }

    @Transactional
    public Product updateProduct(Long id, ApiDtos.ProductRequest request) {
        currentUserService.requirePermission("catalog:manage");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        applyProductRequest(product, request, false);
        Product saved = productRepository.save(product);
        auditService.record("UPDATE", "Product", String.valueOf(saved.getId()), null, saved.getSku(), true);
        return saved;
    }

    @Transactional
    public Product deleteProduct(Long id) {
        currentUserService.requirePermission("catalog:manage");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        if (!product.isActive()) {
            return product;
        }
        product.setActive(false);
        Product saved = productRepository.save(product);
        auditService.record("DEACTIVATE", "Product", String.valueOf(saved.getId()), null, saved.getSku(), true);
        return saved;
    }

    public List<Supplier> listSuppliers() {
        currentUserService.requireAnyPermission("catalog:view", "catalog:manage");
        return supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    public Supplier createSupplier(ApiDtos.SupplierRequest request) {
        currentUserService.requirePermission("catalog:manage");
        String name = request.name().trim();
        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalStateException("Supplier name already exists");
        }
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setContactName(trimToNull(request.contactName()));
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setEmail(trimToNull(request.email()));
        supplier.setActive(request.active());
        Supplier saved = supplierRepository.save(supplier);
        auditService.record("CREATE", "Supplier", String.valueOf(saved.getId()), null, saved.getName(), true);
        return saved;
    }

    @Transactional
    public Supplier updateSupplier(Long id, ApiDtos.SupplierRequest request) {
        currentUserService.requirePermission("catalog:manage");
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        String name = request.name().trim();
        supplierRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(supplier.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Supplier name already exists");
                });
        supplier.setName(name);
        supplier.setContactName(trimToNull(request.contactName()));
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setEmail(trimToNull(request.email()));
        supplier.setActive(request.active());
        Supplier saved = supplierRepository.save(supplier);
        auditService.record("UPDATE", "Supplier", String.valueOf(saved.getId()), null, saved.getName(), true);
        return saved;
    }

    @Transactional
    public Supplier deleteSupplier(Long id) {
        currentUserService.requirePermission("catalog:manage");
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        if (!supplier.isActive()) {
            return supplier;
        }
        supplier.setActive(false);
        Supplier saved = supplierRepository.save(supplier);
        auditService.record("DEACTIVATE", "Supplier", String.valueOf(saved.getId()), null, saved.getName(), true);
        return saved;
    }

    public List<Location> listLocations() {
        currentUserService.requireAnyPermission("catalog:view", "catalog:manage");
        boolean admin = currentUserService.isAdmin();
        if (admin) {
            return locationRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        var allowedLocationIds = currentUserService.currentUserLocationIds();
        return locationRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(location -> allowedLocationIds.contains(location.getId()))
                .toList();
    }

    @Transactional
    public Location createLocation(ApiDtos.LocationRequest request) {
        currentUserService.requirePermission("catalog:manage");
        String code = request.code().trim();
        if (locationRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalStateException("Location code already exists");
        }
        Location location = new Location();
        location.setCode(code);
        location.setName(request.name().trim());
        location.setType(request.type());
        location.setActive(request.active());
        Location saved = locationRepository.save(location);
        auditService.record("CREATE", "Location", String.valueOf(saved.getId()), null, saved.getCode(), true);
        return saved;
    }

    @Transactional
    public Location updateLocation(Long id, ApiDtos.LocationRequest request) {
        currentUserService.requirePermission("catalog:manage");
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));
        String code = request.code().trim();
        locationRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(location.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Location code already exists");
                });
        location.setCode(code);
        location.setName(request.name().trim());
        location.setType(request.type());
        location.setActive(request.active());
        Location saved = locationRepository.save(location);
        auditService.record("UPDATE", "Location", String.valueOf(saved.getId()), null, saved.getCode(), true);
        return saved;
    }

    @Transactional
    public Location deleteLocation(Long id) {
        currentUserService.requirePermission("catalog:manage");
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));
        if (!location.isActive()) {
            return location;
        }
        location.setActive(false);
        Location saved = locationRepository.save(location);
        auditService.record("DEACTIVATE", "Location", String.valueOf(saved.getId()), null, saved.getCode(), true);
        return saved;
    }

    public Product getProduct(Long id) {
        currentUserService.requireAnyPermission("catalog:view", "catalog:manage");
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    public Supplier getSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
    }

    public Location getLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));
    }

    public Location getLocationByCode(String code) {
        return locationRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + code));
    }

    private void applyProductRequest(Product product, ApiDtos.ProductRequest request, boolean creating) {
        String name = request.name().trim();
        String sku = request.sku().trim();
        String existingBarcode = trimToNull(product.getBarcode());
        String barcode = trimToNull(request.barcode());

        productRepository.findBySkuIgnoreCase(sku)
                .filter(existing -> !existing.getId().equals(product.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("SKU already exists");
                });

        if (barcode != null) {
            productRepository.findByBarcodeIgnoreCase(barcode)
                    .filter(existing -> !existing.getId().equals(product.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Barcode already exists");
                    });
        } else if (creating || existingBarcode == null) {
            barcode = generateBarcode();
        } else {
            barcode = existingBarcode;
        }

        product.setName(name);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setCategory(trimToNull(request.category()));
        product.setBrand(trimToNull(request.brand()));
        product.setUnitOfMeasure(trimToNull(request.unitOfMeasure()));
        product.setCostPrice(request.costPrice());
        product.setSellingPrice(request.sellingPrice());
        product.setWholesalePrice(request.wholesalePrice() == null ? request.sellingPrice() : request.wholesalePrice());
        product.setReorderLevel(request.reorderLevel() == null ? 0 : request.reorderLevel());
        product.setActive(request.active());
        if (request.imageUrl() != null) {
            product.setImageUrl(trimToNull(request.imageUrl()));
        }
        if (request.supplierId() != null) {
            product.setSupplier(getSupplier(request.supplierId()));
        } else {
            product.setSupplier(null);
        }
    }

    private String generateBarcode() {
        String barcode;
        do {
            barcode = "BC-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (productRepository.existsByBarcodeIgnoreCase(barcode));
        return barcode;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
