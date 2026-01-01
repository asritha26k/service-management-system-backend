package com.app.technicianservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TechnicianApplicationRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 10, max = 15)
    private String phone;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 50)
    private String state;

    @NotBlank
    @Size(max = 10)
    private String zipCode;

    @NotNull
    @PositiveOrZero
    private Integer experience; // in years

    @NotBlank
    @Size(max = 100)
    private String specialization; // e.g., "Plumbing", "Electrical", "HVAC"

    @NotNull
    @Size(min = 1)
    private List<String> skills; // e.g., ["Pipe Installation", "Water Heater Repair"]

    @Size(max = 200)
    private String certifications; // e.g., "Licensed Plumber, EPA Certified"

    @Size(max = 100)
    private String previousEmployer;

    @Size(max = 1000)
    private String workExperienceDetails; // Brief description of work history

    @NotNull
    @Min(1)
    @Max(20)
    private Integer maxWorkload; // Maximum concurrent jobs they can handle

    @Size(max = 1000)
    private String motivation; // Why they want to join

    private Boolean hasVehicle; // Do they have their own vehicle

    private Boolean hasToolkit; // Do they have their own tools

    @Size(max = 500)
    private String availability; // e.g., "Monday-Friday 9AM-5PM"

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 15)
    private String emergencyContactPhone;
}

