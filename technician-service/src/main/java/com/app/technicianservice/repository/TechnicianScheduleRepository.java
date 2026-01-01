package com.app.technicianservice.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.technicianservice.entity.TechnicianSchedule;

public interface TechnicianScheduleRepository extends JpaRepository<TechnicianSchedule, String> {
    Optional<TechnicianSchedule> findByTechnicianIdAndScheduleDate(String technicianId, LocalDate scheduleDate);
}

