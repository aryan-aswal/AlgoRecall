package com.algorecall.controller;

import com.algorecall.dto.AuthResponse;
import com.algorecall.dto.LoginRequest;
import com.algorecall.dto.RegisterRequest;
import com.algorecall.dto.UserResponse;
import com.algorecall.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_returns201() {
        RegisterRequest req = RegisterRequest.builder()
                .username("alice").email("alice@mail.com").password("pass123").build();
        AuthResponse mockResp = AuthResponse.builder()
                .token("jwt").username("alice").email("alice@mail.com").role("USER").build();
        when(userService.register(req)).thenReturn(mockResp);

        ResponseEntity<AuthResponse> response = authController.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getToken()).isEqualTo("jwt");
    }

    @Test
    void login_returns200() {
        LoginRequest req = LoginRequest.builder().username("alice").password("pass").build();
        AuthResponse mockResp = AuthResponse.builder()
                .token("jwt").username("alice").role("USER").build();
        when(userService.login(req)).thenReturn(mockResp);

        ResponseEntity<AuthResponse> response = authController.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("alice");
    }

    @Test
    void getProfile_returnsUser() {
        when(userDetails.getUsername()).thenReturn("alice");
        UserResponse mockResp = UserResponse.builder().username("alice").email("a@b.com").build();
        when(userService.getCurrentUser("alice")).thenReturn(mockResp);

        ResponseEntity<UserResponse> response = authController.getProfile(userDetails);

        assertThat(response.getBody().getUsername()).isEqualTo("alice");
    }
}
