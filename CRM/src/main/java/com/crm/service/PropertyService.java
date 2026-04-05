package com.crm.service;

import com.crm.dto.PageResponse;
import com.crm.dto.PropertyRequest;
import com.crm.dto.PropertyResponse;
import com.crm.entity.Agent;
import com.crm.entity.Property;
import com.crm.entity.enums.BookingStatus;
import com.crm.entity.enums.PropertyStatus;
import com.crm.entity.enums.PropertyType;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.BookingRepository;
import com.crm.repository.PropertyRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);

    private final PropertyRepository propertyRepository;
    private final AgentService agentService;
    private final BookingRepository bookingRepository;
    private final DtoMapper dtoMapper;

    public PropertyService(
            PropertyRepository propertyRepository,
            AgentService agentService,
            BookingRepository bookingRepository,
            DtoMapper dtoMapper
    ) {
        this.propertyRepository = propertyRepository;
        this.agentService = agentService;
        this.bookingRepository = bookingRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public PropertyResponse createProperty(PropertyRequest request) {
        Property property = new Property();
        applyRequest(property, request);
        Property savedProperty = propertyRepository.save(property);
        log.info("Created property {}", savedProperty.getTitle());
        return dtoMapper.toPropertyResponse(savedProperty);
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> getProperties(
            String location,
            String city,
            String locality,
            PropertyType propertyType,
            String configuration,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            PropertyStatus status,
            Boolean featured,
            int page,
            int size
    ) {
        Specification<Property> specification = buildSpecification(
                location,
                city,
                locality,
                propertyType,
                configuration,
                minPrice,
                maxPrice,
                status,
                featured
        );
        Page<PropertyResponse> responsePage = propertyRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(dtoMapper::toPropertyResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(Long id) {
        return dtoMapper.toPropertyResponse(findPropertyEntity(id));
    }

    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyRequest request) {
        Property property = findPropertyEntity(id);
        if (request.status() == PropertyStatus.AVAILABLE
                && bookingRepository.existsByPropertyAndStatus(property, BookingStatus.CONFIRMED)) {
            throw new BadRequestException("A property with a confirmed booking cannot be marked available");
        }
        applyRequest(property, request);
        log.info("Updated property {}", property.getTitle());
        return dtoMapper.toPropertyResponse(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse assignAgent(Long propertyId, Long agentId) {
        Property property = findPropertyEntity(propertyId);
        Agent agent = agentService.findAgentEntity(agentId);
        property.setAgent(agent);
        log.info("Assigned agent {} to property {}", agent.getEmail(), property.getTitle());
        return dtoMapper.toPropertyResponse(propertyRepository.save(property));
    }

    @Transactional
    public void deleteProperty(Long id) {
        Property property = findPropertyEntity(id);
        if (!property.getBookings().isEmpty()) {
            throw new BadRequestException("Property cannot be deleted while bookings exist");
        }
        propertyRepository.delete(property);
        log.info("Deleted property {}", property.getTitle());
    }

    @Transactional(readOnly = true)
    public Property findPropertyEntity(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id " + id));
    }

    private void applyRequest(Property property, PropertyRequest request) {
        property.setTitle(request.title());
        property.setLocation(request.location());
        property.setCity(request.city());
        property.setLocality(request.locality());
        property.setPropertyType(request.propertyType());
        property.setConfiguration(request.configuration());
        property.setAreaSqFt(request.areaSqFt());
        property.setPrice(request.price());
        property.setStatus(request.status());
        property.setFeatured(request.featured());
        property.setAgent(request.agentId() == null ? null : agentService.findAgentEntity(request.agentId()));
    }

    private Specification<Property> buildSpecification(
            String location,
            String city,
            String locality,
            PropertyType propertyType,
            String configuration,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            PropertyStatus status,
            Boolean featured
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (location != null && !location.isBlank()) {
                String search = "%" + location.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), search),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("locality")), search)
                ));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("city")),
                        "%" + city.toLowerCase() + "%"));
            }
            if (locality != null && !locality.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("locality")),
                        "%" + locality.toLowerCase() + "%"));
            }
            if (propertyType != null) {
                predicates.add(criteriaBuilder.equal(root.get("propertyType"), propertyType));
            }
            if (configuration != null && !configuration.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("configuration")),
                        "%" + configuration.toLowerCase() + "%"));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (featured != null) {
                predicates.add(criteriaBuilder.equal(root.get("featured"), featured));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
