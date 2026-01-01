package com.app.technicianservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.technicianservice.entity.TechnicianApplication;
import com.app.technicianservice.entity.TechnicianApplication.ApplicationStatus;

public interface TechnicianApplicationRepository extends JpaRepository<TechnicianApplication, String> {
    Optional<TechnicianApplication> findByEmail(String email);
    List<TechnicianApplication> findByStatus(ApplicationStatus status);
}

