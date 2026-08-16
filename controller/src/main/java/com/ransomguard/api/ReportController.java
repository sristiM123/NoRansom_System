package com.ransomguard.api;

import com.ransomguard.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public Map<String, Object> report() {
        return reportService.buildQuickReport();
    }
}
