package com.crm.controller;

import com.crm.dto.ClientResponse;
import com.crm.dto.LeadRequest;
import com.crm.dto.LeadResponse;
import com.crm.dto.PageResponse;
import com.crm.entity.enums.LeadSource;
import com.crm.entity.enums.LeadStage;
import com.crm.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public LeadResponse createLead(@Valid @RequestBody LeadRequest request) {
        return leadService.createLead(request);
    }

    @GetMapping
    public PageResponse<LeadResponse> getLeads(
            @RequestParam(required = false) LeadStage stage,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) LocalDate followUpDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return leadService.getLeads(stage, source, agentId, followUpDate, page, size);
    }

    @GetMapping("/{id}")
    public LeadResponse getLeadById(@PathVariable Long id) {
        return leadService.getLeadById(id);
    }

    @PutMapping("/{id}")
    public LeadResponse updateLead(@PathVariable Long id, @Valid @RequestBody LeadRequest request) {
        return leadService.updateLead(id, request);
    }

    @PostMapping("/{id}/convert")
    public ClientResponse convertLead(@PathVariable Long id) {
        return leadService.convertLead(id);
    }

    @DeleteMapping("/{id}")
    public void deleteLead(@PathVariable Long id) {
        leadService.deleteLead(id);
    }
}
