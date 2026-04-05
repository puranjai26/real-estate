package com.crm.service;

import com.crm.dto.AgentResponse;
import com.crm.dto.AgentSummaryResponse;
import com.crm.dto.BookingResponse;
import com.crm.dto.BookingSummaryResponse;
import com.crm.dto.ClientResponse;
import com.crm.dto.ClientSummaryResponse;
import com.crm.dto.LeadResponse;
import com.crm.dto.PropertyResponse;
import com.crm.dto.PropertySummaryResponse;
import com.crm.entity.Agent;
import com.crm.entity.Booking;
import com.crm.entity.Client;
import com.crm.entity.Lead;
import com.crm.entity.Property;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DtoMapper {

    public AgentSummaryResponse toAgentSummary(Agent agent) {
        if (agent == null) {
            return null;
        }
        return new AgentSummaryResponse(agent.getId(), agent.getName(), agent.getEmail());
    }

    public ClientSummaryResponse toClientSummary(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientSummaryResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getPreferredLocation()
        );
    }

    public PropertySummaryResponse toPropertySummary(Property property) {
        if (property == null) {
            return null;
        }
        return new PropertySummaryResponse(
                property.getId(),
                property.getTitle(),
                property.getLocation(),
                property.getCity(),
                property.getLocality(),
                property.getPropertyType() == null ? null : property.getPropertyType().name(),
                property.getConfiguration(),
                property.getAreaSqFt(),
                property.getPrice(),
                property.getStatus().name(),
                property.isFeatured()
        );
    }

    public BookingSummaryResponse toBookingSummary(Booking booking) {
        return new BookingSummaryResponse(
                booking.getId(),
                booking.getBookingDate(),
                booking.getStatus().name(),
                valueOrZero(booking.getBookingAmount()),
                valueOrZero(booking.getAmountPaid()),
                valueOrZero(booking.getBookingAmount()).subtract(valueOrZero(booking.getAmountPaid())).max(BigDecimal.ZERO),
                booking.getPaymentStatus() == null ? "NOT_STARTED" : booking.getPaymentStatus().name(),
                toPropertySummary(booking.getProperty()),
                toClientSummary(booking.getClient()),
                toAgentSummary(booking.getAgent())
        );
    }

    public PropertyResponse toPropertyResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getTitle(),
                property.getLocation(),
                property.getCity(),
                property.getLocality(),
                property.getPropertyType() == null ? null : property.getPropertyType().name(),
                property.getConfiguration(),
                property.getAreaSqFt(),
                property.getPrice(),
                property.getStatus().name(),
                property.isFeatured(),
                property.getCreatedAt(),
                toAgentSummary(property.getAgent()),
                property.getBookings().size(),
                property.getBookings().stream().map(this::toBookingSummary).toList()
        );
    }

    public ClientResponse toClientResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getPreferredLocation(),
                client.getCreatedAt(),
                client.getBookings().size(),
                client.getBookings().stream().map(this::toBookingSummary).toList()
        );
    }

    public AgentResponse toAgentResponse(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getName(),
                agent.getEmail(),
                agent.getProperties().size(),
                agent.getBookings().size(),
                agent.getLeads().size(),
                agent.getProperties().stream().map(this::toPropertySummary).toList(),
                agent.getBookings().stream().map(this::toBookingSummary).toList()
        );
    }

    public BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingDate(),
                booking.getStatus().name(),
                valueOrZero(booking.getBookingAmount()),
                valueOrZero(booking.getAmountPaid()),
                valueOrZero(booking.getBookingAmount()).subtract(valueOrZero(booking.getAmountPaid())).max(BigDecimal.ZERO),
                booking.getPaymentStatus() == null ? "NOT_STARTED" : booking.getPaymentStatus().name(),
                booking.getPaymentDate(),
                booking.getPaymentReference(),
                booking.getCreatedAt(),
                toPropertySummary(booking.getProperty()),
                toClientSummary(booking.getClient()),
                toAgentSummary(booking.getAgent())
        );
    }

    public LeadResponse toLeadResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getPreferredLocation(),
                lead.getBudgetMin(),
                lead.getBudgetMax(),
                lead.getInterestType().name(),
                lead.getSource().name(),
                lead.getFollowUpDate(),
                lead.getStage().name(),
                lead.getNotes(),
                lead.getCreatedAt(),
                toAgentSummary(lead.getAgent())
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
