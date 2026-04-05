package com.crm;

import com.crm.entity.Agent;
import com.crm.repository.AgentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void leadCanBeCreatedFilteredAndConverted() throws Exception {
        Agent broker = agentRepository.findAll().stream().findFirst().orElseThrow();

        String createResponse = mockMvc.perform(post("/api/leads")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Lead",
                                  "email": "test.lead@example.com",
                                  "phone": "+91-9000011111",
                                  "preferredLocation": "Sector 150, Noida",
                                  "budgetMin": 9000000,
                                  "budgetMax": 13000000,
                                  "interestType": "BUY",
                                  "source": "PORTAL",
                                  "followUpDate": "2026-04-21",
                                  "stage": "NEW",
                                  "notes": "Looking for a 3 BHK near expressway access",
                                  "agentId": %d
                                }
                                """.formatted(broker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Lead"))
                .andExpect(jsonPath("$.source").value("PORTAL"))
                .andExpect(jsonPath("$.agent.name").value(broker.getName()))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        long leadId = objectMapper.readTree(createResponse).get("id").asLong();

        String filteredBody = mockMvc.perform(get("/api/leads?stage=NEW&source=PORTAL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(filteredBody).contains("test.lead@example.com");

        mockMvc.perform(post("/api/leads/{id}/convert", leadId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test.lead@example.com"))
                .andExpect(jsonPath("$.preferredLocation").value("Sector 150, Noida"));

        mockMvc.perform(get("/api/leads/{id}", leadId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("CONVERTED"));
    }
}
