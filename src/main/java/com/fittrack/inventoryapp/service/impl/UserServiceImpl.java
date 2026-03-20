package com.fittrack.inventoryapp.service.impl;

import com.fittrack.inventoryapp.exception.DuplicateResourceException;
import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.model.enums.Role;
import com.fittrack.inventoryapp.repository.UserRepository;
import com.fittrack.inventoryapp.repository.ManagerRegistrationRequestRepository;
import com.fittrack.inventoryapp.model.entity.ManagerRegistrationRequest;
import com.fittrack.inventoryapp.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManagerRegistrationRequestRepository managerRequestRepository;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            ManagerRegistrationRequestRepository managerRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.managerRequestRepository = managerRequestRepository;
    }

    @Override
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())
                || managerRequestRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("error.user.username_taken");
        }

        if (user.getId() == null && (userRepository.existsByEmail(user.getEmail())
                || managerRequestRepository.existsByEmail(user.getEmail()))) {
            throw new DuplicateResourceException("error.user.email_taken");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public void changeUserRole(Long userId, Role newRole) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("error.user.not_found"));
        existingUser.setRole(newRole);
        userRepository.save(existingUser);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("error.user.not_found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("error.user.current_password_incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void changeEmail(String username, String currentPassword, String newEmail) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("error.user.not_found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("error.user.current_password_incorrect");
        }

        if (userRepository.existsByEmail(newEmail) && !user.getEmail().equals(newEmail)) {
            throw new IllegalArgumentException("error.user.email_taken");
        }

        user.setEmail(newEmail);
        userRepository.save(user);
    }

    @Override
    public void createManagerRequest(User user) {
        if (userRepository.existsByUsername(user.getUsername())
                || managerRequestRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("error.user.username_taken");
        }

        if (userRepository.existsByEmail(user.getEmail()) || managerRequestRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("error.user.email_taken");
        }

        ManagerRegistrationRequest request = ManagerRegistrationRequest.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword()))
                .fullName(user.getFullName())
                .build();

        managerRequestRepository.save(request);
    }

    @Override
    public List<ManagerRegistrationRequest> getPendingManagerRequests() {
        return managerRequestRepository.findAll();
    }

    @Override
    public void approveManagerRequest(Long requestId) {
        ManagerRegistrationRequest request = managerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (userRepository.existsByUsername(request.getUsername())) {
            managerRequestRepository.delete(request);
            throw new DuplicateResourceException("error.user.username_taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            managerRequestRepository.delete(request);
            throw new DuplicateResourceException("error.user.email_taken");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .role(Role.MANAGER)
                .build();

        userRepository.save(newUser);
        managerRequestRepository.delete(request);
    }

    @Override
    public void rejectManagerRequest(Long requestId) {
        managerRequestRepository.deleteById(requestId);
    }
}
