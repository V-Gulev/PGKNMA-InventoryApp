package com.fittrack.inventoryapp.service;

import com.fittrack.inventoryapp.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void changeUserRole(Long userId, com.fittrack.inventoryapp.model.enums.Role newRole);

    List<User> findAllUsers();

    void changePassword(String username, String currentPassword, String newPassword);

    void changeEmail(String username, String currentPassword, String newEmail);

    void createManagerRequest(User user);

    List<com.fittrack.inventoryapp.model.entity.ManagerRegistrationRequest> getPendingManagerRequests();

    void approveManagerRequest(Long requestId);

    void rejectManagerRequest(Long requestId);
}
