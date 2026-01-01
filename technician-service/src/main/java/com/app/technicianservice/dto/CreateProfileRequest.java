package com.app.technicianservice.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateProfileRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    @NotNull
    @Size(min = 1)
    private List<String> skills;

    @Size(max = 200)
    private String specialization;

    @PositiveOrZero
    private Integer experience;

    @NotBlank
    private String location;

    @NotNull
    @PositiveOrZero
    private Integer maxWorkload;
}
