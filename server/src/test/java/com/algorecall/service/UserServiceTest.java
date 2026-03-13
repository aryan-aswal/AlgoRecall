package com.algorecall.service;

import com.algorecall.dto.*;
import com.algorecall.mapper.UserMapper;
import com.algorecall.model.User;
import com.algorecall.repository.UserRepository;
import com.algorecall.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, passwordEncoder, jwtUtil, authenticationManager);
    }

    // ─── loadUserByUsername ─────────────────────────────────────────

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        User user = User.builder().username("alice").password("hashed").role(User.Role.USER).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_unknownUser_throwsException() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ─── register ───────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequest req = RegisterRequest.builder()
                .username("bob").email("bob@example.com").password("secret123").build();

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$encoded$");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        // register calls loadUserByUsername which calls findByUsername
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(
                User.builder().id(1L).username("bob").email("bob@example.com")
                        .password("$encoded$").role(User.Role.USER).build()));
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = userService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("bob");
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest req = RegisterRequest.builder()
                .username("existing").email("e@mail.com").password("pass").build();
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = RegisterRequest.builder()
                .username("new").email("taken@mail.com").password("pass").build();
        when(userRepository.existsByUsername("new")).thenReturn(false);
        when(userRepository.existsByEmail("taken@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    // ─── login ──────────────────────────────────────────────────────

    @Test
    void login_success() {
        LoginRequest req = LoginRequest.builder().username("bob").password("pass").build();

        User user = User.builder().username("bob").email("bob@mail.com")
                .password("$encoded$").role(User.Role.USER).build();

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("login-token");

        AuthResponse response = userService.login(req);

        assertThat(response.getToken()).isEqualTo("login-token");
        assertThat(response.getUsername()).isEqualTo("bob");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_badCredentials_throwsException() {
        LoginRequest req = LoginRequest.builder().username("bob").password("wrong").build();
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ─── getCurrentUser ─────────────────────────────────────────────

    @Test
    void getCurrentUser_success() {
        User user = User.builder().username("bob").email("bob@mail.com").build();
        UserResponse expected = UserResponse.builder().username("bob").email("bob@mail.com").build();

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        UserResponse result = userService.getCurrentUser("bob");

        assertThat(result.getUsername()).isEqualTo("bob");
    }

    @Test
    void getCurrentUser_notFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
