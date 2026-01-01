package com.app.identity_service.entity;

// UserRole Enum
// Defines all available roles in the system
public enum UserRole {
	ADMIN("Administrator"),
	MANAGER("Manager"),
	TECHNICIAN("Technician"),
	CUSTOMER("Customer");

	private final String displayName;

	UserRole(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

