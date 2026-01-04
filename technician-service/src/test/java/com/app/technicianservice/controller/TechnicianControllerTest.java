package com.app.technicianservice.controller;

import com.app.technicianservice.dto.*;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.security.RequestUser;
import com.app.technicianservice.security.RequestUserResolver;
import com.app.technicianservice.service.TechnicianApplicationService;
import com.app.technicianservice.service.TechnicianService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TechnicianController.class, excludeAutoConfiguration = {
                EurekaClientAutoConfiguration.class
})
@TestPropertySource(properties = {
                "logging.level.root=INFO",
                "logging.level.com.app.technicianservice=INFO",
                "spring.application.name=technician-service-test",
                "server.port=0",
                "spring.cloud.config.enabled=false"
})
class TechnicianControllerTest {

        @TestConfiguration
        static class TestConfig implements WebMvcConfigurer {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                        resolvers.add(new RequestUserResolver());
                }
        }

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private TechnicianService technicianService;

        @MockBean
        private TechnicianApplicationService applicationService;

        @Autowired
        private ObjectMapper objectMapper;

        private RequestUser testUser;
        private TechnicianProfileResponse profileResponse;
        private CreateProfileRequest createProfileRequest;

        @BeforeEach
        void setUp() {
                testUser = new RequestUser("user-1", "TECHNICIAN");

                profileResponse = new TechnicianProfileResponse();
                profileResponse.setId("profile-1");
                profileResponse.setUserId("user-1");
                profileResponse.setEmail("tech@example.com");
                profileResponse.setName("John Doe");
                profileResponse.setPhone("1234567890");
                profileResponse.setAvailable(true);
                profileResponse.setCurrentWorkload(2);
                profileResponse.setMaxWorkload(5);

                createProfileRequest = new CreateProfileRequest();
                createProfileRequest.setEmail("tech@example.com");
                createProfileRequest.setName("John Doe");
                createProfileRequest.setPhone("1234567890");
                createProfileRequest.setSkills(Arrays.asList("Plumbing", "Electrical"));
                createProfileRequest.setLocation("New York");
                createProfileRequest.setMaxWorkload(5);
        }

        @Test
        void createProfile_ShouldReturnCreated() throws Exception {
                when(technicianService.createProfile(any(RequestUser.class), any(CreateProfileRequest.class)))
                                .thenReturn(profileResponse);

                mockMvc.perform(post("/api/technicians/profile")
                                .header("X-User-Id", "user-1")
                                .header("X-User-Role", "TECHNICIAN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createProfileRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("profile-1"))
                                .andExpect(jsonPath("$.message").value("Technician profile created successfully"));
        }

        @Test
        void createProfile_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
                createProfileRequest.setEmail(""); // Invalid email

                mockMvc.perform(post("/api/technicians/profile")
                                .header("X-User-Id", "user-1")
                                .header("X-User-Role", "TECHNICIAN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createProfileRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getAvailable_ShouldReturnOk() throws Exception {
                TechnicianSummaryResponse summary = new TechnicianSummaryResponse(
                                "profile-1", "John Doe", "Plumbing", true, 2, 5);
                List<TechnicianSummaryResponse> summaries = Arrays.asList(summary);
                when(technicianService.getAvailable()).thenReturn(summaries);

                mockMvc.perform(get("/api/technicians/available"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("profile-1"))
                                .andExpect(jsonPath("$[0].name").value("John Doe"));
        }

        @Test
        void getStats_ShouldReturnOk() throws Exception {
                StatsResponse stats = new StatsResponse();
                stats.setTotalTechnicians(10);
                stats.setAvailableTechnicians(5);
                stats.setAverageWorkloadRatio(60.0);

                when(technicianService.getStats()).thenReturn(stats);

                mockMvc.perform(get("/api/technicians/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalTechnicians").value(10))
                                .andExpect(jsonPath("$.availableTechnicians").value(5));
        }

        @Test
        void getMyProfile_ShouldReturnOk() throws Exception {
                when(technicianService.getByUserId("user-1")).thenReturn(profileResponse);

                mockMvc.perform(get("/api/technicians/me")
                                .header("X-User-Id", "user-1")
                                .header("X-User-Role", "TECHNICIAN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("profile-1"))
                                .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        void getByUserId_ShouldReturnOk() throws Exception {
                when(technicianService.getByUserId("user-1")).thenReturn(profileResponse);

                mockMvc.perform(get("/api/technicians/by-user/user-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("profile-1"));
        }

        @Test
        void getMyWorkload_ShouldReturnOk() throws Exception {
                WorkloadResponse workload = new WorkloadResponse("profile-1", true, 2, 5);
                when(technicianService.getMyWorkload(any(RequestUser.class))).thenReturn(workload);

                mockMvc.perform(get("/api/technicians/my/workload")
                                .header("X-User-Id", "user-1")
                                .header("X-User-Role", "TECHNICIAN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.technicianId").value("profile-1"))
                                .andExpect(jsonPath("$.currentWorkload").value(2));
        }

        @Test
        void updateMyAvailability_ShouldReturnNoContent() throws Exception {
                AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
                request.setAvailable(false);

                when(technicianService.updateMyAvailability(any(RequestUser.class),
                                any(AvailabilityUpdateRequest.class)))
                                .thenReturn(profileResponse);

                mockMvc.perform(put("/api/technicians/my/availability")
                                .header("X-User-Id", "user-1")
                                .header("X-User-Role", "TECHNICIAN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());
        }

        @Test
        void apply_ShouldReturnCreated() throws Exception {
                TechnicianApplicationRequest request = new TechnicianApplicationRequest();
                request.setEmail("newtech@example.com");
                request.setFullName("Jane Doe");
                request.setPhone("9876543210");
                request.setAddress("123 Main St");
                request.setCity("New York");
                request.setState("NY");
                request.setZipCode("10001");
                request.setExperience(5);
                request.setSpecialization("Plumbing");
                request.setSkills(Arrays.asList("Pipe Installation", "Leak Repair"));
                request.setMaxWorkload(10);

                ApplicationSubmissionResponse response = new ApplicationSubmissionResponse();
                response.setId("app-1");
                response.setEmail("newtech@example.com");
                response.setStatus(com.app.technicianservice.entity.TechnicianApplication.ApplicationStatus.PENDING);

                when(applicationService.applyForTechnician(any(TechnicianApplicationRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/technicians/apply")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("app-1"));
        }

        @Test
        void getPendingApplications_ShouldReturnOk() throws Exception {
                ApplicationReviewResponse review = new ApplicationReviewResponse();
                review.setId("app-1");
                review.setEmail("newtech@example.com");
                List<ApplicationReviewResponse> reviews = Arrays.asList(review);

                when(applicationService.getPendingApplications(any(RequestUser.class))).thenReturn(reviews);

                mockMvc.perform(get("/api/technicians/applications/pending")
                                .header("X-User-Id", "admin-1")
                                .header("X-User-Role", "ADMIN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("app-1"));
        }

        @Test
        void approveApplication_ShouldReturnOk() throws Exception {
                ApplicationReviewResponse response = new ApplicationReviewResponse();
                response.setId("app-1");

                when(applicationService.approveApplication(any(RequestUser.class), eq("app-1")))
                                .thenReturn(response);

                mockMvc.perform(post("/api/technicians/applications/app-1/approve")
                                .header("X-User-Id", "admin-1")
                                .header("X-User-Role", "ADMIN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("app-1"))
                                .andExpect(jsonPath("$.message").value("Application approved successfully"));
        }

        @Test
        void rejectApplication_ShouldReturnOk() throws Exception {
                ApplicationReviewResponse response = new ApplicationReviewResponse();
                response.setId("app-1");

                ApplicationRejectionRequest request = new ApplicationRejectionRequest();
                request.setReason("Insufficient experience");

                when(applicationService.rejectApplication(any(RequestUser.class), eq("app-1"), anyString()))
                                .thenReturn(response);

                mockMvc.perform(post("/api/technicians/applications/app-1/reject")
                                .header("X-User-Id", "admin-1")
                                .header("X-User-Role", "ADMIN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("app-1"))
                                .andExpect(jsonPath("$.message").value("Application rejected successfully"));
        }

        @Test
        void getWorkload_ShouldReturnOk() throws Exception {
                WorkloadResponse workload = new WorkloadResponse("profile-1", true, 2, 5);
                when(technicianService.getWorkload("profile-1")).thenReturn(workload);

                mockMvc.perform(get("/api/technicians/profile-1/workload"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.technicianId").value("profile-1"))
                                .andExpect(jsonPath("$.currentWorkload").value(2));
        }

        @Test
        void updateWorkload_ShouldReturnNoContent() throws Exception {
                WorkloadResponse workload = new WorkloadResponse("profile-1", true, 3, 5);
                when(technicianService.updateWorkload("profile-1", 3)).thenReturn(workload);

                mockMvc.perform(put("/api/technicians/profile-1/workload")
                                .param("current", "3"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void updateAvailability_ShouldReturnNoContent() throws Exception {
                AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
                request.setAvailable(false);

                when(technicianService.updateAvailability(
                                any(RequestUser.class), eq("profile-1"), any(AvailabilityUpdateRequest.class)))
                                .thenReturn(profileResponse);

                mockMvc.perform(put("/api/technicians/profile-1/availability")
                                .header("X-User-Id", "user-1")
                                .header("X-User-Role", "TECHNICIAN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getById_ShouldReturnOk() throws Exception {
                when(technicianService.getById("profile-1")).thenReturn(profileResponse);

                mockMvc.perform(get("/api/technicians/profile-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("profile-1"))
                                .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        void getById_ShouldReturnNotFound_WhenNotFound() throws Exception {
                when(technicianService.getById("invalid-id"))
                                .thenThrow(new NotFoundException("Technician not found"));

                mockMvc.perform(get("/api/technicians/invalid-id"))
                                .andExpect(status().isNotFound());
        }
}
