package com.app.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// RefreshToken Entity
// Stores refresh tokens for long-lived authentication
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@NotBlank(message = "Token is required")
	@Column(nullable = false, unique = true, columnDefinition = "LONGTEXT")
	private String token;

	@Column(nullable = false)
	private String userId;

	@Column(nullable = false)
	private LocalDateTime expiryDate;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public RefreshToken(String token, String userId, LocalDateTime expiryDate) {
		this.token = token;
		this.userId = userId;
		this.expiryDate = expiryDate;
	}

	// ============ JPA Lifecycle Callbacks ============
	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	// ============ Helper Methods ============
	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiryDate);
	}
}

