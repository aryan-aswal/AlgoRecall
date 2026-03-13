package com.algorecall.controller;

import com.algorecall.dto.UserResponse;
import com.algorecall.dto.UserSettingsRequest;
import com.algorecall.dto.UserSettingsResponse;
import com.algorecall.model.User;
import com.algorecall.model.UserPreference;
import com.algorecall.repository.UserPreferenceRepository;
import com.algorecall.repository.UserRepository;
import com.algorecall.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private UserPreferenceRepository userPreferenceRepository;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    private User mockUser;
    private UserPreference mockPref;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("alice").email("alice@mail.com").build();
        mockPref = UserPreference.builder()
                .id(1L).user(mockUser)
                .emailNotifications(true).pushNotifications(false)
                .smsNotifications(false).calendarSync(true).darkMode(false)
                .newProblemDuration(45).revisionDuration(30)
                .build();
    }

    @Test
    void getCurrentUser_returnsUser() {
        when(userDetails.getUsername()).thenReturn("alice");
        UserResponse resp = UserResponse.builder().username("alice").email("alice@mail.com").build();
        when(userService.getCurrentUser("alice")).thenReturn(resp);

        ResponseEntity<UserResponse> response = userController.getCurrentUser(userDetails);

        assertThat(response.getBody().getUsername()).isEqualTo("alice");
    }

    @Test
    void getSettings_existingPreference() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(mockPref));

        ResponseEntity<UserSettingsResponse> response = userController.getSettings(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isEmailNotifications()).isTrue();
        assertThat(response.getBody().getNewProblemDuration()).isEqualTo(45);
    }

    @Test
    void getSettings_createsDefaultPreferenceWhenMissing() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenReturn(mockPref);

        ResponseEntity<UserSettingsResponse> response = userController.getSettings(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    void getSettings_userNotFound_throws() {
        when(userDetails.getUsername()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getSettings(userDetails))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void updateSettings_updatesAllFields() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(mockPref));
        when(userPreferenceRepository.save(any())).thenReturn(mockPref);

        UserSettingsRequest request = UserSettingsRequest.builder()
                .emailNotifications(false).pushNotifications(true)
                .darkMode(true).newProblemDuration(60).revisionDuration(15)
                .phoneNumber("+1234567890").fcmDeviceToken("token123")
                .build();

        ResponseEntity<UserSettingsResponse> response = userController.updateSettings(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userPreferenceRepository).save(any());
        verify(userRepository).save(mockUser); // phone updated on User
        assertThat(mockUser.getPhoneNumber()).isEqualTo("+1234567890");
    }

    @Test
    void updateSettings_partialUpdate_onlyChangesProvided() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(mockPref));
        when(userPreferenceRepository.save(any())).thenReturn(mockPref);

        UserSettingsRequest request = UserSettingsRequest.builder()
                .darkMode(true).build(); // only darkMode set

        ResponseEntity<UserSettingsResponse> response = userController.updateSettings(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRepository, never()).save(any()); // phone not provided, user not saved
    }

    @Test
    void getSettings_googleCalendarConnected_flag() {
        mockPref.setGoogleRefreshToken("refresh-token-xyz");
        when(userDetails.getUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(mockPref));

        ResponseEntity<UserSettingsResponse> response = userController.getSettings(userDetails);

        assertThat(response.getBody().isGoogleCalendarConnected()).isTrue();
    }
}
