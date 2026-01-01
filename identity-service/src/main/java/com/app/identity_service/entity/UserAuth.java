package com.app.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// UserAuth Entity
// Stores authentication credentials and basic user information
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_auth")
public class UserAuth {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Email(message = "Email must be valid")
	@NotBlank(message = "Email is required")
	@Column(unique = true, nullable = false)
	private String email;

	@NotBlank(message = "Password is required")
	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Column(nullable = false)
	private Boolean isActive = true;

	@Column(nullable = false)
	private Boolean isEmailVerified = false;

	@Column(name = "force_password_change")
	private Boolean forcePasswordChange = false;

	@Column(name = "created_by")
	private String createdBy;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public UserAuth(String email, String password, UserRole role) {
		this.email = email;
		this.password = password;
		this.role = role;
		this.isActive = true;
		this.isEmailVerified = false;
		this.forcePasswordChange = !role.equals(UserRole.CUSTOMER);
	}

	// ============ JPA Lifecycle Callbacks ============
	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
