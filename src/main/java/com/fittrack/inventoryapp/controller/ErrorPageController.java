package com.fittrack.inventoryapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {

    @GetMapping("/error-page")
    public String showErrorPage() {
        return "error-page";
    }
}
