package com.crm;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerAliasIntegrationTest extends BaseIntegrationTest {

    @Test
    void brokerAliasEndpointReturnsBrokerDataForAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/brokers?page=0&size=5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").isString())
                .andExpect(jsonPath("$.content[0].propertyCount").isNumber());
    }
}
