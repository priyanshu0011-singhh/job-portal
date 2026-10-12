package com.ncst.job.portal.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HomeControllerTest {

    @Test
    void shouldReturnApiHomeMessage() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HomeController()).build();

        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Job Portal API is running"));
    }
}
