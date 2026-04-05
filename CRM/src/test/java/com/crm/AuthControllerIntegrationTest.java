package com.crm;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void loginReturnsJwtAndBrokerAliasField() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "aarav@realestatecrm.com",
                                  "password": "agent123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.brokerId").isNumber())
                .andExpect(jsonPath("$.agentId").isNumber());
    }

    @Test
    void currentUserProfileReturnsAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@realestatecrm.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
