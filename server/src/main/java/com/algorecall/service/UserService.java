package com.algorecall.service;

import com.algorecall.dto.*;
import com.algorecall.mapper.UserMapper;
import com.algorecall.model.User;
import com.algorecall.repository.UserRepository;
import com.algorecall.security.JwtUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Cacheable(cacheNames = "userDetailsByUsername", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return toUserDetails(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder().username(request.getUsername()).email(request.getEmail()).phoneNumber(request.getPhoneNumber()).password(passwordEncoder.encode(request.getPassword())).role(User.Role.USER).build();

        user = userRepository.save(user);

        UserDetails userDetails = toUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder().token(token).username(user.getUsername()).email(user.getEmail()).role(user.getRole().name()).build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserDetails userDetails = toUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder().token(token).username(user.getUsername()).email(user.getEmail()).role(user.getRole().name()).build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
