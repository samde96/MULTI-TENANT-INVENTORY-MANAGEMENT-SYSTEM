package com.keen.erp.controller;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.service.CatalogService;
import com.keen.erp.service.ProductImageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;
    private final ProductImageService productImageService;

    public CatalogController(CatalogService catalogService, ProductImageService productImageService) {
        this.catalogService = catalogService;
        this.productImageService = productImageService;
    }

    @GetMapping("/products")
    public List<?> listProducts() {
        return catalogService.listProducts();
    }

    @GetMapping("/products/{id}")
    public Object getProduct(@PathVariable Long id) {
        return catalogService.getProduct(id);
    }

    @PostMapping("/products")
    public Object createProduct(@Valid @RequestBody ApiDtos.ProductRequest request) {
        return catalogService.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public Object updateProduct(@PathVariable Long id, @Valid @RequestBody ApiDtos.ProductRequest request) {
        return catalogService.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    public Object deleteProduct(@PathVariable Long id) {
        return catalogService.deleteProduct(id);
    }

    @GetMapping("/suppliers")
    public List<?> listSuppliers() {
        return catalogService.listSuppliers();
    }

    @PostMapping("/suppliers")
    public Object createSupplier(@Valid @RequestBody ApiDtos.SupplierRequest request) {
        return catalogService.createSupplier(request);
    }

    @PutMapping("/suppliers/{id}")
    public Object updateSupplier(@PathVariable Long id, @Valid @RequestBody ApiDtos.SupplierRequest request) {
        return catalogService.updateSupplier(id, request);
    }

    @DeleteMapping("/suppliers/{id}")
    public Object deleteSupplier(@PathVariable Long id) {
        return catalogService.deleteSupplier(id);
    }

    @GetMapping("/locations")
    public List<?> listLocations() {
        return catalogService.listLocations();
    }

    @PostMapping("/locations")
    public Object createLocation(@Valid @RequestBody ApiDtos.LocationRequest request) {
        return catalogService.createLocation(request);
    }

    @PutMapping("/locations/{id}")
    public Object updateLocation(@PathVariable Long id, @Valid @RequestBody ApiDtos.LocationRequest request) {
        return catalogService.updateLocation(id, request);
    }

    @DeleteMapping("/locations/{id}")
    public Object deleteLocation(@PathVariable Long id) {
        return catalogService.deleteLocation(id);
    }

    @PostMapping(value = "/products/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiDtos.UploadResponse uploadProductImage(@RequestPart("image") MultipartFile image) {
        return productImageService.storeProductImage(image);
    }
}
