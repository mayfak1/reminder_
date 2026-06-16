package com.example.reminder.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldReturn401ForProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/reminder"))
                .andExpect(status().isUnauthorized());
    }
}
