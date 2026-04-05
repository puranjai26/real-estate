package com.crm.controller;

import com.crm.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/properties.csv")
    public ResponseEntity<byte[]> exportPropertiesCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=properties.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(exportService.exportPropertiesToCsv());
    }

    @GetMapping("/clients.csv")
    public ResponseEntity<byte[]> exportClientsCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clients.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(exportService.exportClientsToCsv());
    }

    @GetMapping("/leads.csv")
    public ResponseEntity<byte[]> exportLeadsCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(exportService.exportLeadsToCsv());
    }

    @GetMapping("/bookings.xlsx")
    public ResponseEntity<byte[]> exportBookingsExcel() throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bookings.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(exportService.exportBookingsToExcel());
    }
}
