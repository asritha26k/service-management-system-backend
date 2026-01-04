package com.app.identity_service.service;

import com.app.identity_service.dto.PagedResponse;
import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.dto.UserDetailResponse;
import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserProfile;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.repository.UserAuthRepository;
import com.app.identity_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserService userService;

    private UserAuth userAuth;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userAuth = new UserAuth();
        userAuth.setId("user-1");
        userAuth.setEmail("user@example.com");
        userAuth.setPassword("encodedPassword");
        userAuth.setRole(UserRole.CUSTOMER);
        userAuth.setIsActive(true);
        userAuth.setIsEmailVerified(true);
        userAuth.setForcePasswordChange(false);

        userProfile = new UserProfile();
        userProfile.setUserId("user-1");
        userProfile.setName("Test User");
        userProfile.setPhone("1234567890");
        userProfile.setAddress("123 Test St");
        userProfile.setCity("Test City");
        userProfile.setState("TS");
        userProfile.setPincode("12345");
    }

    @Test
    void getUsersByRole_ShouldReturnUsers() {
        List<UserAuth> users = Arrays.asList(userAuth);
        when(userAuthRepository.findByRole(UserRole.CUSTOMER)).thenReturn(users);

        List<UserAuthResponse> responses = userService.getUsersByRole("CUSTOMER");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("user-1", responses.get(0).getId());
        assertEquals("CUSTOMER", responses.get(0).getRole());
        verify(userAuthRepository, times(1)).findByRole(UserRole.CUSTOMER);
    }

    @Test
    void getUsersByRole_ShouldThrowIllegalArgumentException_WhenInvalidRole() {
        assertThrows(IllegalArgumentException.class, () -> 
            userService.getUsersByRole("INVALID_ROLE"));
    }

    @Test
    void searchUsersByEmail_ShouldReturnMatchingUsers() {
        UserAuth user1 = new UserAuth();
        user1.setId("user-1");
        user1.setEmail("user1@example.com");
        user1.setRole(UserRole.CUSTOMER);
        user1.setIsActive(true);
        user1.setIsEmailVerified(true);
        user1.setForcePasswordChange(false);
        
        UserAuth user2 = new UserAuth();
        user2.setId("user-2");
        user2.setEmail("user2@test.com");
        user2.setRole(UserRole.CUSTOMER);
        user2.setIsActive(true);
        user2.setIsEmailVerified(true);
        user2.setForcePasswordChange(false);

        when(userAuthRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<UserAuthResponse> responses = userService.searchUsersByEmail("example");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("user1@example.com", responses.get(0).getEmail());
        verify(userAuthRepository, times(1)).findAll();
    }

    @Test
    void searchUsersByEmail_ShouldReturnEmptyList_WhenNoMatches() {
        when(userAuthRepository.findAll()).thenReturn(Arrays.asList(userAuth));

        List<UserAuthResponse> responses = userService.searchUsersByEmail("nonexistent");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void searchUsersByEmail_ShouldBeCaseInsensitive() {
        UserAuth user1 = new UserAuth();
        user1.setId("user-1");
        user1.setEmail("User@Example.com");
        user1.setRole(UserRole.CUSTOMER);
        user1.setIsActive(true);
        user1.setIsEmailVerified(true);
        user1.setForcePasswordChange(false);
        
        when(userAuthRepository.findAll()).thenReturn(Arrays.asList(user1));

        List<UserAuthResponse> responses = userService.searchUsersByEmail("user");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));

        UserAuthResponse response = userService.getUserById("user-1");

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("user@example.com", response.getEmail());
        verify(userAuthRepository, times(1)).findById("user-1");
    }

    @Test
    void getUserById_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userAuthRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userService.getUserById("invalid-id"));
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        when(userAuthRepository.findByEmail("user@example.com")).thenReturn(Optional.of(userAuth));

        UserAuthResponse response = userService.getUserByEmail("user@example.com");

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("user@example.com", response.getEmail());
        verify(userAuthRepository, times(1)).findByEmail("user@example.com");
    }

    @Test
    void getUserByEmail_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userAuthRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userService.getUserByEmail("invalid@example.com"));
    }

    @Test
    void deactivateUser_ShouldDeactivateUser() {
        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userAuth);

        userService.deactivateUser("user-1");

        assertFalse(userAuth.getIsActive());
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
    }

    @Test
    void deactivateUser_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userAuthRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userService.deactivateUser("invalid-id"));
    }

    @Test
    void activateUser_ShouldActivateUser() {
        userAuth.setIsActive(false);
        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userAuth);

        userService.activateUser("user-1");

        assertTrue(userAuth.getIsActive());
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
    }

    @Test
    void activateUser_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userAuthRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userService.activateUser("invalid-id"));
    }

    @Test
    void getUsersWithDetailsByRole_ShouldReturnPagedResponse() {
        List<UserAuth> users = Arrays.asList(userAuth);
        Page<UserAuth> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        
        when(userAuthRepository.findByRole(eq(UserRole.CUSTOMER), any(Pageable.class))).thenReturn(userPage);
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(userProfile));

        PagedResponse<UserDetailResponse> response = userService.getUsersWithDetailsByRole("CUSTOMER", PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPageNumber());
        assertEquals(10, response.getPageSize());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isLast());
        
        UserDetailResponse userDetail = response.getContent().get(0);
        assertEquals("user-1", userDetail.getId());
        assertEquals("user@example.com", userDetail.getEmail());
        assertEquals("CUSTOMER", userDetail.getRole());
        assertEquals("Test User", userDetail.getName());
        assertEquals("1234567890", userDetail.getPhone());
        
        verify(userAuthRepository, times(1)).findByRole(eq(UserRole.CUSTOMER), any(Pageable.class));
        verify(userProfileRepository, times(1)).findByUserId("user-1");
    }

    @Test
    void getUsersWithDetailsByRole_ShouldThrowIllegalArgumentException_WhenInvalidRole() {
        assertThrows(IllegalArgumentException.class, () -> 
            userService.getUsersWithDetailsByRole("INVALID_ROLE", PageRequest.of(0, 10)));
    }

    @Test
    void getUsersWithDetailsByRole_ShouldHandleUserWithoutProfile() {
        List<UserAuth> users = Arrays.asList(userAuth);
        Page<UserAuth> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        
        when(userAuthRepository.findByRole(eq(UserRole.CUSTOMER), any(Pageable.class))).thenReturn(userPage);
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PagedResponse<UserDetailResponse> response = userService.getUsersWithDetailsByRole("CUSTOMER", PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        
        UserDetailResponse userDetail = response.getContent().get(0);
        assertEquals("user-1", userDetail.getId());
        assertEquals("user@example.com", userDetail.getEmail());
        assertNull(userDetail.getName());
        assertNull(userDetail.getPhone());
        assertNull(userDetail.getAddress());
    }
}

