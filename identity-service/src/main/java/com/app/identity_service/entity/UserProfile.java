package com.app.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// UserProfile Entity
// Stores detailed user profile information
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_profile",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
public class UserProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

    @Column(name = "user_id", nullable = false, unique = true)
	private String userId;
	@NotBlank(message = "Name is required")
	@Column(nullable = false)
	private String name;

	@Column(length = 20)
	private String phone;

	@Column(length = 255)
	private String address;

	@Column(length = 100)
	private String city;

	@Column(length = 100)
	private String state;

	@Column(length = 10)
	private String pincode;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public UserProfile(String userId, String name) {
		this.userId = userId;
		this.name = name;
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
