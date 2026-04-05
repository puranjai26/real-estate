package com.crm.controller;

import com.crm.dto.PageResponse;
import com.crm.dto.PropertyRequest;
import com.crm.dto.PropertyResponse;
import com.crm.entity.enums.PropertyStatus;
import com.crm.entity.enums.PropertyType;
import com.crm.service.PropertyService;
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

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    public PropertyResponse createProperty(@Valid @RequestBody PropertyRequest request) {
        return propertyService.createProperty(request);
    }

    @GetMapping
    public PageResponse<PropertyResponse> getProperties(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) String configuration,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return propertyService.getProperties(
                location,
                city,
                locality,
                propertyType,
                configuration,
                minPrice,
                maxPrice,
                status,
                featured,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    public PropertyResponse getPropertyById(@PathVariable Long id) {
        return propertyService.getPropertyById(id);
    }

    @PutMapping("/{id}")
    public PropertyResponse updateProperty(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
        return propertyService.updateProperty(id, request);
    }

    @PostMapping("/{propertyId}/assign-agent/{agentId}")
    public PropertyResponse assignAgent(@PathVariable Long propertyId, @PathVariable Long agentId) {
        return propertyService.assignAgent(propertyId, agentId);
    }

    @PostMapping("/{propertyId}/assign-broker/{brokerId}")
    public PropertyResponse assignBroker(@PathVariable Long propertyId, @PathVariable Long brokerId) {
        return propertyService.assignAgent(propertyId, brokerId);
    }

    @DeleteMapping("/{id}")
    public void deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
    }
}
