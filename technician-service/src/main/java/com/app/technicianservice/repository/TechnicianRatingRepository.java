package com.app.technicianservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.technicianservice.entity.TechnicianRating;

@Repository
public interface TechnicianRatingRepository extends JpaRepository<TechnicianRating, String> {
    List<TechnicianRating> findByTechnicianId(String technicianId);
    Optional<TechnicianRating> findByTechnicianIdAndCustomerId(String technicianId, String customerId);
}
