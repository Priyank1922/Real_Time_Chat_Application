package com.example.chatapp.controller;

import com.example.chatapp.enums.RoomType;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden // Hide UI template endpoints from OpenAPI schema
public class ViewController {

    @GetMapping({"/", "/chat"})
    public String index(Model model) {
        model.addAttribute("roomTypes", RoomType.values());
        return "index";
    }
}
