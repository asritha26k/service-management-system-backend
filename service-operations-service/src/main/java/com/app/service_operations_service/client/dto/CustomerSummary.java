package com.app.service_operations_service.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerSummary {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;

    @JsonProperty("active")
    @JsonAlias({"isActive", "Active"})
    private boolean active;
}