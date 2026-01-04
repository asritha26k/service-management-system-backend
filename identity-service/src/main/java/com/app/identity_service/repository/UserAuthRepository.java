package com.app.identity_service.repository;

import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// UserAuth Repository
// Data access layer for UserAuth entity
@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, String> {
	Optional<UserAuth> findByEmail(String email);
	List<UserAuth> findByRole(UserRole role);
	Page<UserAuth> findByRole(UserRole role, Pageable pageable);
	List<UserAuth> findByIsActive(Boolean isActive);
	boolean existsByEmail(String email);
}

