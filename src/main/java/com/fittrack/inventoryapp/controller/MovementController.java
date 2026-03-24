package com.fittrack.inventoryapp.controller;

import com.fittrack.inventoryapp.model.entity.AssetMovement;
import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.model.enums.AssetCondition;
import com.fittrack.inventoryapp.model.enums.Role;
import com.fittrack.inventoryapp.service.AssetService;
import com.fittrack.inventoryapp.service.MovementService;
import com.fittrack.inventoryapp.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/movements")
public class MovementController {

    private final MovementService movementService;
    private final AssetService assetService;
    private final UserService userService;

    public MovementController(MovementService movementService,
            AssetService assetService,
            UserService userService) {
        this.movementService = movementService;
        this.assetService = assetService;
        this.userService = userService;
    }

    @GetMapping
    public String listAllMovements(Model model,
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "borrowedDate") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        User loggedIn = null;
        if (currentUser != null) {
            loggedIn = userService.findByUsername(currentUser.getUsername()).orElseThrow();
            model.addAttribute("currentUserRole", loggedIn.getRole().name());
            model.addAttribute("currentUsername", loggedIn.getUsername());
        }

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();

        List<AssetMovement> movements;
        if ((keyword == null || keyword.trim().isEmpty())
                && (status == null || status.trim().isEmpty() || "all".equalsIgnoreCase(status))) {
            movements = movementService.getAllMovements(sort);
        } else {
            movements = movementService.searchMovements(keyword, status, sort);
        }

        if (loggedIn != null && loggedIn.getRole() == Role.STUDENT) {
            final String studentUsername = loggedIn.getUsername();
            movements = movements.stream()
                    .filter(m -> m.getUser().getUsername().equals(studentUsername))
                    .toList();
        }

        model.addAttribute("movements", movements);
        model.addAttribute("conditions", AssetCondition.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "movements/movement-list";
    }

    @GetMapping("/borrow")
    public String showBorrowForm(Model model,
            @RequestParam(value = "assetId", required = false) Long assetId,
            @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("availableAssets", assetService.findBorrowableAssets());
        if (assetId != null) {
            model.addAttribute("selectedAssetId", assetId);
        }
        model.addAttribute("users", getAllowedBorrowTargets(currentUser));

        User loggedIn = userService.findByUsername(currentUser.getUsername()).orElseThrow();
        model.addAttribute("isStudent", loggedIn.getRole() == Role.STUDENT);

        return "movements/borrow-form";
    }

    @PostMapping("/borrow")
    public String borrowAsset(@RequestParam("assetId") Long assetId,
            @RequestParam("username") String username,
            @RequestParam("borrowedDate") String borrowedDateStr,
            @RequestParam("expectedReturnDate") String expectedReturnDateStr,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        // Server-side: verify the current user is allowed to borrow for the target user
        if (!isAllowedToBorrowFor(currentUser, username)) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.borrow.permission_denied");
            return "redirect:/movements/borrow";
        }

        // Validate borrowed date
        LocalDate borrowedDate;
        LocalDate expectedReturnDate;
        try {
            borrowedDate = LocalDate.parse(borrowedDateStr);
            expectedReturnDate = LocalDate.parse(expectedReturnDateStr);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.date.invalid");
            return "redirect:/movements/borrow";
        }

        // Nobody can borrow for past dates
        if (borrowedDate.isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.borrow.past_date");
            return "redirect:/movements/borrow";
        }

        if (expectedReturnDate.isBefore(borrowedDate)) {
            redirectAttributes.addFlashAttribute("errorMessage", "error.borrow.end_before_start");
            return "redirect:/movements/borrow";
        }

        try {
            movementService.borrowAsset(assetId, username, borrowedDate, expectedReturnDate);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.asset_borrowed");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/movements";
    }

    @PostMapping("/return/{assetId}")
    public String returnAsset(@PathVariable Long assetId,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "newCondition", required = false) AssetCondition newCondition,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            validateMovementAuthority(assetId, currentUser);
            movementService.returnAsset(assetId, notes, newCondition);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.asset_returned");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/movements";
    }

    @PostMapping("/cancel/{assetId}")
    public String cancelBorrow(@PathVariable Long assetId,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            validateMovementAuthority(assetId, currentUser);
            movementService.cancelBorrow(assetId);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.borrow_cancelled");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/movements";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/pending")
    public String showPendingRequests(Model model) {
        model.addAttribute("pendingMovements", movementService.getPendingMovements());
        return "movements/pending-list";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{id}/approve")
    public String approveBorrowRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            movementService.approveBorrow(id);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.request_approved");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/movements/pending";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{id}/reject")
    public String rejectBorrowRequest(@PathVariable Long id,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            movementService.rejectBorrow(id, notes);
            redirectAttributes.addFlashAttribute("successMessage", "alert.success.request_rejected");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/movements/pending";
    }

    @GetMapping("/my-items")
    public String showMyItems(Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("movements", movementService.getMyActiveItems(currentUser.getUsername()));
        model.addAttribute("conditions", AssetCondition.values());
        return "movements/my-items";
    }

    // --- helpers ---

    private List<User> getAllowedBorrowTargets(UserDetails currentUser) {
        User loggedIn = userService.findByUsername(currentUser.getUsername())
                .orElseThrow();

        return switch (loggedIn.getRole()) {
            case ADMIN, MANAGER -> userService.findAllUsers();
            case TEACHER -> userService.findAllUsers().stream()
                    .filter(u -> u.getRole() == Role.STUDENT
                            || u.getUsername().equals(loggedIn.getUsername()))
                    .toList();
            case STUDENT -> List.of(loggedIn);
        };
    }

    private boolean isAllowedToBorrowFor(UserDetails currentUser, String targetUsername) {
        User loggedIn = userService.findByUsername(currentUser.getUsername())
                .orElseThrow();

        if (loggedIn.getRole() == Role.ADMIN || loggedIn.getRole() == Role.MANAGER) {
            return true;
        }

        if (loggedIn.getRole() == Role.TEACHER) {
            if (targetUsername.equals(loggedIn.getUsername())) {
                return true;
            }
            return userService.findByUsername(targetUsername)
                    .map(target -> target.getRole() == Role.STUDENT)
                    .orElse(false);
        }

        // STUDENT — self only
        return targetUsername.equals(loggedIn.getUsername());
    }

    private void validateMovementAuthority(Long assetId, UserDetails currentUser) {
        User loggedIn = userService.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (loggedIn.getRole() == Role.ADMIN || loggedIn.getRole() == Role.MANAGER) {
            return; // Authorized
        }

        // Must be the person who borrowed it
        List<AssetMovement> history = movementService.getHistoryByAsset(assetId);
        AssetMovement activeMovement = history.stream()
                .filter(m -> m.getReturnedDate() == null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active borrow record found for this asset."));

        if (!activeMovement.getUser().getUsername().equals(loggedIn.getUsername())) {
            throw new RuntimeException("You do not have permission to modify this movement record.");
        }
    }
}
