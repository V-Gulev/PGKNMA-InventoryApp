package com.fittrack.inventoryapp.init;

import com.fittrack.inventoryapp.model.entity.User;
import com.fittrack.inventoryapp.model.enums.Role;
import com.fittrack.inventoryapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            System.out.println(">>> Default admin user created (username: admin / password: admin123)");
        }
    }
}
