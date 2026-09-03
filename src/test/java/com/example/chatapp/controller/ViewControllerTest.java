package com.example.chatapp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ViewController.class)
class ViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / - returns index view with roomTypes model attribute")
    void testIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("roomTypes"));
    }

    @Test
    @DisplayName("GET /chat - returns index view")
    void testChatView() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}
