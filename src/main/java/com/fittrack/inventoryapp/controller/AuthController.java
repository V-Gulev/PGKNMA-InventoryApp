package com.fittrack.inventoryapp.controller;

import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.model.enums.Role;
import com.fittrack.inventoryapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;
import java.util.List;

@Controller
public class AuthController {

    private final UserService userService;

    /** Roles available for self-registration (ADMIN excluded). */
    private static final List<Role> REGISTRATION_ROLES = Arrays.stream(Role.values())
            .filter(r -> r != Role.ADMIN)
            .toList();

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", REGISTRATION_ROLES);
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            if (bindingResult.hasFieldErrors("username")) {
                model.addAttribute("registrationError", "error.register.invalid_username");
            } else if (bindingResult.hasFieldErrors("fullName")) {
                if (bindingResult.getFieldError("fullName").getCode() != null && bindingResult.getFieldError("fullName").getCode().equals("Pattern")) {
                     model.addAttribute("registrationError", "error.register.invalid_name_format");
                } else {
                     model.addAttribute("registrationError", "error.register.invalid_name");
                }
            } else if (bindingResult.hasFieldErrors("password")) {
                model.addAttribute("registrationError", "error.register.invalid_password");
            } else {
                model.addAttribute("registrationError", "error.register.validation_failed");
            }
            model.addAttribute("roles", REGISTRATION_ROLES);
            return "auth/register";
        }

        // Server-side guard: prevent ADMIN self-registration
        if (user.getRole() == Role.ADMIN) {
            user.setRole(Role.STUDENT);
        }

        try {
            if (user.getRole() == Role.MANAGER) {
                userService.createManagerRequest(user);
                return "redirect:/login?request_sent";
            } else {
                userService.registerUser(user);
                return "redirect:/login?registered";
            }
        } catch (RuntimeException e) {
            model.addAttribute("registrationError", e.getMessage());
            model.addAttribute("roles", REGISTRATION_ROLES);
            return "auth/register";
        }
    }
}
