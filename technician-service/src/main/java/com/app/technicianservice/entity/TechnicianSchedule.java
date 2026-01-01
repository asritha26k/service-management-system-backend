package com.app.technicianservice.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "technician_schedules")
public class TechnicianSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String technicianId;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String availableSlotsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bookedSlotsJson;
}
