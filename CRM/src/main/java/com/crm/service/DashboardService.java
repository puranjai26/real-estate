package com.crm.service;

import com.crm.dto.DashboardBrokerSnapshot;
import com.crm.dto.DashboardDistributionItem;
import com.crm.dto.DashboardLeadFollowUp;
import com.crm.dto.DashboardResponse;
import com.crm.dto.DashboardUpcomingBooking;
import com.crm.entity.Booking;
import com.crm.entity.Lead;
import com.crm.entity.Property;
import com.crm.entity.enums.BookingStatus;
import com.crm.entity.enums.LeadStage;
import com.crm.entity.enums.PropertyStatus;
import com.crm.repository.AgentRepository;
import com.crm.repository.BookingRepository;
import com.crm.repository.ClientRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final ClientRepository clientRepository;
    private final BookingRepository bookingRepository;
    private final AgentRepository agentRepository;
    private final LeadRepository leadRepository;

    public DashboardService(
            PropertyRepository propertyRepository,
            ClientRepository clientRepository,
            BookingRepository bookingRepository,
            AgentRepository agentRepository,
            LeadRepository leadRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.clientRepository = clientRepository;
        this.bookingRepository = bookingRepository;
        this.agentRepository = agentRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardSummary() {
        List<Property> properties = propertyRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Lead> leads = leadRepository.findAll();
        long totalLeads = leads.size();
        long totalProperties = propertyRepository.count();
        long totalClients = clientRepository.count();
        long totalBookings = bookings.size();
        long availableProperties = properties.stream()
                .filter(property -> property.getStatus() == PropertyStatus.AVAILABLE)
                .count();
        long soldProperties = properties.stream()
                .filter(property -> property.getStatus() == PropertyStatus.SOLD)
                .count();
        BigDecimal totalPortfolioValue = properties.stream()
                .map(property -> property.getPrice() == null ? BigDecimal.ZERO : property.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableInventoryValue = properties.stream()
                .filter(property -> property.getStatus() == PropertyStatus.AVAILABLE)
                .map(property -> property.getPrice() == null ? BigDecimal.ZERO : property.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldInventoryValue = properties.stream()
                .filter(property -> property.getStatus() == PropertyStatus.SOLD)
                .map(property -> property.getPrice() == null ? BigDecimal.ZERO : property.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePropertyPrice = totalProperties == 0
                ? BigDecimal.ZERO
                : totalPortfolioValue.divide(BigDecimal.valueOf(totalProperties), 2, RoundingMode.HALF_UP);
        BigDecimal totalBookingValue = bookings.stream()
                .map(booking -> booking.getBookingAmount() == null ? BigDecimal.ZERO : booking.getBookingAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collectedPayments = bookings.stream()
                .map(booking -> booking.getAmountPaid() == null ? BigDecimal.ZERO : booking.getAmountPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstandingPayments = totalBookingValue.subtract(collectedPayments).max(BigDecimal.ZERO);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        double bookingConversionRate = totalBookings == 0
                ? 0.0
                : BigDecimal.valueOf(confirmedBookings * 100.0 / totalBookings)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new DashboardResponse(
                totalLeads,
                totalProperties,
                totalClients,
                totalBookings,
                availableProperties,
                soldProperties,
                pendingBookings,
                confirmedBookings,
                cancelledBookings,
                totalPortfolioValue,
                availableInventoryValue,
                soldInventoryValue,
                averagePropertyPrice,
                totalBookingValue,
                collectedPayments,
                outstandingPayments,
                bookingConversionRate,
                buildDistribution(properties.stream()
                        .collect(Collectors.groupingBy(property -> property.getPropertyType() == null ? "Flat" : property.getPropertyType().name(), Collectors.counting()))),
                buildDistribution(properties.stream()
                        .collect(Collectors.groupingBy(property -> property.getCity() == null ? "Unknown" : property.getCity(), Collectors.counting()))),
                buildDistribution(bookings.stream()
                        .collect(Collectors.groupingBy(booking -> booking.getStatus().name(), Collectors.counting()))),
                buildDistribution(leads.stream()
                        .collect(Collectors.groupingBy(lead -> lead.getStage().name(), Collectors.counting()))),
                buildDistribution(leads.stream()
                        .collect(Collectors.groupingBy(lead -> lead.getSource().name(), Collectors.counting()))),
                agentRepository.findAll().stream()
                        .map(agent -> new DashboardBrokerSnapshot(
                                agent.getName(),
                                agent.getProperties().size(),
                                agent.getBookings().size(),
                                agent.getBookings().stream()
                                        .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                                        .count()
                        ))
                        .sorted(Comparator.comparingLong(DashboardBrokerSnapshot::confirmedDeals)
                                .reversed()
                                .thenComparing(Comparator.comparingLong(DashboardBrokerSnapshot::bookingCount).reversed())
                                .thenComparing(DashboardBrokerSnapshot::name))
                        .limit(4)
                        .toList(),
                bookings.stream()
                        .filter(booking -> booking.getBookingDate() != null)
                        .filter(booking -> !booking.getBookingDate().isBefore(LocalDate.now()))
                        .sorted(Comparator.comparing(Booking::getBookingDate))
                        .limit(5)
                        .map(booking -> new DashboardUpcomingBooking(
                                booking.getProperty().getTitle(),
                                booking.getClient().getName(),
                                booking.getProperty().getLocation(),
                                booking.getBookingDate(),
                                booking.getStatus().name()
                        ))
                        .toList(),
                leads.stream()
                        .filter(lead -> lead.getFollowUpDate() != null)
                        .filter(lead -> lead.getStage() != LeadStage.CONVERTED && lead.getStage() != LeadStage.LOST)
                        .filter(lead -> !lead.getFollowUpDate().isBefore(LocalDate.now()))
                        .sorted(Comparator.comparing(Lead::getFollowUpDate))
                        .limit(6)
                        .map(lead -> new DashboardLeadFollowUp(
                                lead.getName(),
                                lead.getPreferredLocation(),
                                lead.getStage().name(),
                                lead.getSource().name(),
                                lead.getFollowUpDate(),
                                lead.getAgent() == null ? "Unassigned" : lead.getAgent().getName()
                        ))
                        .toList()
        );
    }

    private List<DashboardDistributionItem> buildDistribution(Map<String, Long> groupedData) {
        return groupedData.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new DashboardDistributionItem(formatLabel(entry.getKey()), entry.getValue()))
                .toList();
    }

    private String formatLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        return List.of(value.split("[_\\s]+")).stream()
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
