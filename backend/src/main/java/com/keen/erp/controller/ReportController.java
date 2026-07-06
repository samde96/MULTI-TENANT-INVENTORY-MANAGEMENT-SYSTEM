package com.keen.erp.controller;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public ApiDtos.DashboardSummaryResponse dashboard() {
        return reportService.dashboard();
    }
}
