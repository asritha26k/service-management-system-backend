package com.app.identity_service.repository;

import com.app.identity_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// RefreshToken Repository
// Data access layer for RefreshToken entity
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
	Optional<RefreshToken> findByToken(String token);
	Optional<RefreshToken> findByUserId(String userId);
	void deleteByUserId(String userId);
}

