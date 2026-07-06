package com.keen.erp.controller;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/balances")
    public List<ApiDtos.InventoryBalanceResponse> balances(@RequestParam(required = false) Long locationId,
                            @RequestParam(required = false) Long productId) {
        return inventoryService.listBalances(locationId, productId);
    }

    @GetMapping("/ledger")
    public List<ApiDtos.StockLedgerEntryResponse> ledger() {
        return inventoryService.listLedger();
    }

    @PostMapping("/receive")
    public Object receive(@Valid @RequestBody ApiDtos.ReceiveStockRequest request) {
        return inventoryService.receiveStock(request);
    }
}
