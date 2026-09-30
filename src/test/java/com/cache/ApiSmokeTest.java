package com.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPutAndGet() throws Exception {
        mockMvc.perform(put("/api/cache/entries/testKey")
                .contentType(MediaType.TEXT_PLAIN)
                .content("testValue")
                .param("ttl", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STORED"));

        mockMvc.perform(get("/api/cache/entries/testKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIT"))
                .andExpect(jsonPath("$.value").value("testValue"));
    }

    @Test
    public void testInvalidPolicy() throws Exception {
        mockMvc.perform(post("/api/cache/policy").param("policy", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
