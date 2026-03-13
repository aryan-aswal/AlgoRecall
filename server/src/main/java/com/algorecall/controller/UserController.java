package com.algorecall.controller;

import com.algorecall.dto.UserResponse;
import com.algorecall.dto.UserSettingsRequest;
import com.algorecall.dto.UserSettingsResponse;
import com.algorecall.model.User;
import com.algorecall.model.UserPreference;
import com.algorecall.repository.UserPreferenceRepository;
import com.algorecall.repository.UserRepository;
import com.algorecall.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<UserSettingsResponse> getSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPreference newPref = UserPreference.builder().user(user).build();
                    return userPreferenceRepository.save(newPref);
                });

        return ResponseEntity.ok(buildSettingsResponse(user, pref));
    }

    @PutMapping("/settings")
    public ResponseEntity<UserSettingsResponse> updateSettings(
            @RequestBody UserSettingsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPreference newPref = UserPreference.builder().user(user).build();
                    return userPreferenceRepository.save(newPref);
                });

        // Update toggles
        if (request.getEmailNotifications() != null) pref.setEmailNotifications(request.getEmailNotifications());
        if (request.getPushNotifications() != null) pref.setPushNotifications(request.getPushNotifications());
        if (request.getSmsNotifications() != null) pref.setSmsNotifications(request.getSmsNotifications());
        if (request.getCalendarSync() != null) pref.setCalendarSync(request.getCalendarSync());
        if (request.getDarkMode() != null) pref.setDarkMode(request.getDarkMode());

        // Update durations
        if (request.getNewProblemDuration() != null) pref.setNewProblemDuration(request.getNewProblemDuration());
        if (request.getRevisionDuration() != null) pref.setRevisionDuration(request.getRevisionDuration());

        // Update FCM token
        if (request.getFcmDeviceToken() != null) pref.setFcmDeviceToken(request.getFcmDeviceToken());

        // Update phone on User entity
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
            userRepository.save(user);
        }

        userPreferenceRepository.save(pref);

        return ResponseEntity.ok(buildSettingsResponse(user, pref));
    }

    private UserSettingsResponse buildSettingsResponse(User user, UserPreference pref) {
        return UserSettingsResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .emailNotifications(pref.getEmailNotifications())
                .pushNotifications(pref.getPushNotifications())
                .smsNotifications(pref.getSmsNotifications())
                .calendarSync(pref.getCalendarSync())
                .darkMode(pref.getDarkMode())
                .newProblemDuration(pref.getNewProblemDuration())
                .revisionDuration(pref.getRevisionDuration())
                .googleCalendarConnected(pref.getGoogleRefreshToken() != null && !pref.getGoogleRefreshToken().isBlank())
                .build();
    }
}
