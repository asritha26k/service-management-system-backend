package com.app.identity_service.repository;

import com.app.identity_service.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// UserProfile Repository
// Data access layer for UserProfile entity
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
	Optional<UserProfile> findByUserId(String userId);
	boolean existsByUserId(String userId);
}

