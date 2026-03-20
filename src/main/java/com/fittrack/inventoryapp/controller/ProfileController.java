package com.fittrack.inventoryapp.controller;

import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String showProfile(Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = userService.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("profileUser", user);
        return "profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.profile.update_failed");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.password.mismatch");
            return "redirect:/profile";
        }

        try {
            userService.changePassword(currentUser.getUsername(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.password_updated");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.password.update_failed");
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/change-email")
    public String changeEmail(@RequestParam String currentPassword,
            @RequestParam String newEmail,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            userService.changeEmail(currentUser.getUsername(), currentPassword, newEmail);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.email_updated");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.email.update_failed");
        }

        return "redirect:/profile";
    }
}
