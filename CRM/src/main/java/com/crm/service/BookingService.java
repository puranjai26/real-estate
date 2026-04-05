package com.crm.service;

import com.crm.dto.BookingRequest;
import com.crm.dto.BookingResponse;
import com.crm.dto.PageResponse;
import com.crm.entity.Agent;
import com.crm.entity.Booking;
import com.crm.entity.Client;
import com.crm.entity.Property;
import com.crm.entity.enums.BookingStatus;
import com.crm.entity.enums.PaymentStatus;
import com.crm.entity.enums.PropertyStatus;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;
    private final ClientService clientService;
    private final AgentService agentService;
    private final DtoMapper dtoMapper;

    public BookingService(
            BookingRepository bookingRepository,
            PropertyRepository propertyRepository,
            PropertyService propertyService,
            ClientService clientService,
            AgentService agentService,
            DtoMapper dtoMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.propertyService = propertyService;
        this.clientService = clientService;
        this.agentService = agentService;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Property property = propertyService.findPropertyEntity(request.propertyId());
        Client client = clientService.findClientEntity(request.clientId());
        Agent agent = agentService.findAgentEntity(request.agentId());

        validatePropertyAgent(property, agent);
        if (property.getStatus() == PropertyStatus.SOLD) {
            throw new BadRequestException("Sold properties cannot be booked");
        }
        validateConfirmedBooking(property, request.status(), null);

        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setClient(client);
        booking.setAgent(agent);
        applyRequest(booking, request);

        if (property.getAgent() == null) {
            property.setAgent(agent);
        }

        Booking savedBooking = bookingRepository.save(booking);
        synchronizePropertyStatus(property);
        log.info("Created booking {} for property {}", savedBooking.getId(), property.getTitle());
        return dtoMapper.toBookingResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getBookings(
            BookingStatus status,
            Long agentId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Specification<Booking> specification = buildSpecification(status, agentId, fromDate, toDate);
        Page<BookingResponse> responsePage = bookingRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(dtoMapper::toBookingResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        return dtoMapper.toBookingResponse(findBookingEntity(id));
    }

    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = findBookingEntity(id);
        Property previousProperty = booking.getProperty();
        Property property = propertyService.findPropertyEntity(request.propertyId());
        Client client = clientService.findClientEntity(request.clientId());
        Agent agent = agentService.findAgentEntity(request.agentId());

        if (!booking.getProperty().getId().equals(property.getId()) && property.getStatus() == PropertyStatus.SOLD) {
            throw new BadRequestException("Sold properties cannot be reassigned to a booking");
        }

        validatePropertyAgent(property, agent);
        validateConfirmedBooking(property, request.status(), booking.getId());

        booking.setProperty(property);
        booking.setClient(client);
        booking.setAgent(agent);
        applyRequest(booking, request);

        Booking updatedBooking = bookingRepository.save(booking);
        synchronizePropertyStatus(property);
        if (!previousProperty.getId().equals(property.getId())) {
            synchronizePropertyStatus(previousProperty);
        }
        log.info("Updated booking {}", updatedBooking.getId());
        return dtoMapper.toBookingResponse(updatedBooking);
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = findBookingEntity(id);
        Property property = booking.getProperty();
        bookingRepository.delete(booking);
        synchronizePropertyStatus(property);
        log.info("Deleted booking {}", id);
    }

    @Transactional(readOnly = true)
    public Booking findBookingEntity(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
    }

    private void validatePropertyAgent(Property property, Agent agent) {
        if (property.getAgent() != null && !property.getAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("Booking agent must match the property's assigned agent");
        }
    }

    private void validateConfirmedBooking(Property property, BookingStatus status, Long currentBookingId) {
        if (status != BookingStatus.CONFIRMED) {
            return;
        }
        boolean anotherConfirmedBookingExists = bookingRepository.findByProperty(property).stream()
                .filter(existing -> existing.getStatus() == BookingStatus.CONFIRMED)
                .anyMatch(existing -> !existing.getId().equals(currentBookingId));
        if (anotherConfirmedBookingExists) {
            throw new BadRequestException("Only one confirmed booking is allowed for a property");
        }
    }

    private void synchronizePropertyStatus(Property property) {
        boolean confirmedBookingExists = bookingRepository.existsByPropertyAndStatus(property, BookingStatus.CONFIRMED);
        property.setStatus(confirmedBookingExists ? PropertyStatus.SOLD : PropertyStatus.AVAILABLE);
        propertyRepository.save(property);
    }

    private void applyRequest(Booking booking, BookingRequest request) {
        BigDecimal bookingAmount = request.bookingAmount() == null ? BigDecimal.ZERO : request.bookingAmount();
        BigDecimal amountPaid = request.amountPaid() == null ? BigDecimal.ZERO : request.amountPaid();
        if (amountPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Amount paid cannot be negative");
        }
        if (bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Booking amount must be greater than zero");
        }
        if (amountPaid.compareTo(bookingAmount) > 0) {
            throw new BadRequestException("Amount paid cannot exceed booking amount");
        }

        booking.setBookingDate(request.bookingDate());
        booking.setStatus(request.status());
        booking.setBookingAmount(bookingAmount);
        booking.setAmountPaid(amountPaid);
        booking.setPaymentStatus(resolvePaymentStatus(request.paymentStatus(), bookingAmount, amountPaid));
        booking.setPaymentDate(request.paymentDate());
        booking.setPaymentReference(request.paymentReference());
    }

    private PaymentStatus resolvePaymentStatus(PaymentStatus requestedStatus, BigDecimal bookingAmount, BigDecimal amountPaid) {
        PaymentStatus derivedStatus;
        if (amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            derivedStatus = PaymentStatus.NOT_STARTED;
        } else if (amountPaid.compareTo(bookingAmount) >= 0) {
            derivedStatus = PaymentStatus.PAID;
        } else {
            derivedStatus = PaymentStatus.PARTIAL;
        }

        if (requestedStatus == null) {
            return derivedStatus;
        }
        if (requestedStatus == PaymentStatus.PAID && derivedStatus != PaymentStatus.PAID) {
            throw new BadRequestException("Payment status cannot be marked paid until the full booking amount is collected");
        }
        if (requestedStatus == PaymentStatus.NOT_STARTED && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Payment status cannot be not started when an amount has already been collected");
        }
        return requestedStatus == PaymentStatus.PAID ? PaymentStatus.PAID : derivedStatus;
    }

    private Specification<Booking> buildSpecification(
            BookingStatus status,
            Long agentId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (agentId != null) {
                predicates.add(criteriaBuilder.equal(root.join("agent").get("id"), agentId));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bookingDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("bookingDate"), toDate));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
