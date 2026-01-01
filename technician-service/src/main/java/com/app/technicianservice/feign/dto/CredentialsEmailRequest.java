package com.app.technicianservice.feign.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CredentialsEmailRequest {

    private String email;
    private String password; // Temporary password for new technician
    private String role;

    // Constructor that accepts all fields
    public CredentialsEmailRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}


