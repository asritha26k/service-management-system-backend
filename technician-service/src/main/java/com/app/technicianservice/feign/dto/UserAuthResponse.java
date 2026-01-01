package com.app.technicianservice.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {

    private String id;
    private String email;
    private String role;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean forcePasswordChange;
}

