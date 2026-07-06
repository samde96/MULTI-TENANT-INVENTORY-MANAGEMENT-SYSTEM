package com.keen.erp.controller;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/stock-requests")
    public List<ApiDtos.StockRequestResponse> listRequests() {
        return workflowService.listRequests();
    }

    @PostMapping("/stock-requests")
    public ApiDtos.StockRequestResponse createRequest(@Valid @RequestBody ApiDtos.StockRequestCreateRequest request) {
        return workflowService.createRequest(request);
    }

    @PostMapping("/stock-requests/{id}/submit")
    public ApiDtos.StockRequestResponse submitRequest(@PathVariable Long id, @RequestBody(required = false) ApiDtos.StockRequestActionRequest request) {
        return workflowService.submitRequest(id, request == null ? null : request.note());
    }

    @PostMapping("/stock-requests/{id}/approve")
    public ApiDtos.StockTransferResponse approveRequest(@PathVariable Long id) {
        return workflowService.approveRequest(id);
    }

    @PostMapping("/stock-requests/{id}/reject")
    public ApiDtos.StockRequestResponse rejectRequest(@PathVariable Long id, @RequestBody(required = false) ApiDtos.StockRequestActionRequest request) {
        return workflowService.rejectRequest(id, request == null ? null : request.note());
    }

    @GetMapping("/stock-transfers")
    public List<ApiDtos.StockTransferResponse> listTransfers() {
        return workflowService.listTransfers();
    }

    @PostMapping("/stock-transfers")
    public ApiDtos.StockTransferResponse createTransfer(@Valid @RequestBody ApiDtos.TransferCreateRequest request) {
        return workflowService.createTransfer(request);
    }

    @PostMapping("/stock-transfers/{id}/approve")
    public ApiDtos.StockTransferResponse approveTransfer(@PathVariable Long id) {
        return workflowService.approveTransfer(id);
    }

    @PostMapping("/stock-transfers/{id}/dispatch")
    public ApiDtos.StockTransferResponse dispatchTransfer(@PathVariable Long id) {
        return workflowService.dispatchTransfer(id);
    }

    @PostMapping("/stock-transfers/{id}/receive")
    public ApiDtos.StockTransferResponse receiveTransfer(@PathVariable Long id, @Valid @RequestBody ApiDtos.TransferReceiveRequest request) {
        return workflowService.receiveTransfer(id, request);
    }

    @GetMapping("/sales")
    public List<ApiDtos.SaleResponse> listSales() {
        return workflowService.listSales();
    }

    @PostMapping("/sales")
    public ApiDtos.SaleResponse createSale(@Valid @RequestBody ApiDtos.SaleCreateRequest request) {
        return workflowService.createSale(request);
    }
}
