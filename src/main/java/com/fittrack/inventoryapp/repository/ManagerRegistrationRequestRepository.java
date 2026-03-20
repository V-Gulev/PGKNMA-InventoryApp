package com.fittrack.inventoryapp.repository;

import com.fittrack.inventoryapp.model.entity.ManagerRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRegistrationRequestRepository extends JpaRepository<ManagerRegistrationRequest, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
