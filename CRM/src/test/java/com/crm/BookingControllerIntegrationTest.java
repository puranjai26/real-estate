package com.crm;

import com.crm.entity.Agent;
import com.crm.entity.Client;
import com.crm.entity.Property;
import com.crm.entity.enums.PropertyStatus;
import com.crm.entity.enums.PropertyType;
import com.crm.repository.AgentRepository;
import com.crm.repository.ClientRepository;
import com.crm.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Test
    void confirmedBookingConflictIsRejected() throws Exception {
        Agent broker = agentRepository.findAll().stream().findFirst().orElseThrow();

        Client firstClient = new Client();
        firstClient.setName("Test Buyer One");
        firstClient.setEmail("buyer.one@test.com");
        firstClient.setPhone("+91-9000000001");
        firstClient.setPreferredLocation("Dwarka Expressway, Gurugram");
        clientRepository.save(firstClient);

        Client secondClient = new Client();
        secondClient.setName("Test Buyer Two");
        secondClient.setEmail("buyer.two@test.com");
        secondClient.setPhone("+91-9000000002");
        secondClient.setPreferredLocation("Dwarka Expressway, Gurugram");
        clientRepository.save(secondClient);

        Property property = new Property();
        property.setTitle("Dwarka Expressway Heights");
        property.setLocation("Sector 113, Gurugram");
        property.setCity("Gurugram");
        property.setLocality("Sector 113");
        property.setPropertyType(PropertyType.FLAT);
        property.setConfiguration("3 BHK");
        property.setAreaSqFt(1710);
        property.setPrice(new BigDecimal("15800000"));
        property.setStatus(PropertyStatus.AVAILABLE);
        property.setFeatured(true);
        property.setAgent(broker);
        propertyRepository.save(property);

        String firstBookingBody = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyId": %d,
                                  "clientId": %d,
                                  "agentId": %d,
                                  "bookingDate": "2026-04-18",
                                  "status": "PENDING",
                                  "bookingAmount": 1580000,
                                  "amountPaid": 250000,
                                  "paymentStatus": "PARTIAL",
                                  "paymentReference": "TEST-PAY-001"
                                }
                                """.formatted(property.getId(), firstClient.getId(), broker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String secondBookingBody = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyId": %d,
                                  "clientId": %d,
                                  "agentId": %d,
                                  "bookingDate": "2026-04-20",
                                  "status": "PENDING",
                                  "bookingAmount": 1580000,
                                  "amountPaid": 0,
                                  "paymentStatus": "NOT_STARTED",
                                  "paymentReference": "TEST-PAY-002"
                                }
                                """.formatted(property.getId(), secondClient.getId(), broker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        long firstBookingId = objectMapper.readTree(firstBookingBody).get("id").asLong();
        long secondBookingId = objectMapper.readTree(secondBookingBody).get("id").asLong();

        mockMvc.perform(put("/api/bookings/{id}", firstBookingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyId": %d,
                                  "clientId": %d,
                                  "agentId": %d,
                                  "bookingDate": "2026-04-18",
                                  "status": "CONFIRMED",
                                  "bookingAmount": 1580000,
                                  "amountPaid": 1580000,
                                  "paymentStatus": "PAID",
                                  "paymentReference": "TEST-PAY-001"
                                }
                                """.formatted(property.getId(), firstClient.getId(), broker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        String conflictResponse = mockMvc.perform(put("/api/bookings/{id}", secondBookingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "propertyId": %d,
                                  "clientId": %d,
                                  "agentId": %d,
                                  "bookingDate": "2026-04-20",
                                  "status": "CONFIRMED",
                                  "bookingAmount": 1580000,
                                  "amountPaid": 500000,
                                  "paymentStatus": "PARTIAL",
                                  "paymentReference": "TEST-PAY-002"
                                }
                                """.formatted(property.getId(), secondClient.getId(), broker.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only one confirmed booking is allowed for a property"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(conflictResponse).contains("Only one confirmed booking is allowed for a property");
    }
}
