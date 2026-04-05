package com.crm;

import com.crm.entity.Agent;
import com.crm.repository.AgentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void createPropertySupportsRichIndianMarketFieldsAndFilters() throws Exception {
        Agent broker = agentRepository.findAll().stream().findFirst().orElseThrow();
        String title = "Noida Signature Greens";

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "location": "Sector 150, Noida",
                                  "city": "Noida",
                                  "locality": "Sector 150",
                                  "propertyType": "FLAT",
                                  "configuration": "3 BHK",
                                  "areaSqFt": 1640,
                                  "price": 14500000,
                                  "status": "AVAILABLE",
                                  "featured": true,
                                  "agentId": %d
                                }
                                """.formatted(title, broker.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.city").value("Noida"))
                .andExpect(jsonPath("$.locality").value("Sector 150"))
                .andExpect(jsonPath("$.configuration").value("3 BHK"))
                .andExpect(jsonPath("$.areaSqFt").value(1640))
                .andExpect(jsonPath("$.featured").value(true));

        String filteredBody = mockMvc.perform(get("/api/properties?city=Noida&featured=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(filteredBody).contains(title);
    }
}
