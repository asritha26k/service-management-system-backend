package com.app.technicianservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.technicianservice.entity.TechnicianProfile;

public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, String> {
    Optional<TechnicianProfile> findByUserId(String userId);
    List<TechnicianProfile> findByIsAvailableTrue();
    List<TechnicianProfile> findByIsAvailableTrueAndCurrentWorkloadLessThan(Integer maxWorkload);
}
