package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.ChangePasswordRequest;
import com.ecotrack.dto.UserProfileResponse;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.FileUploadService;
import com.ecotrack.service.UserService;
import com.ecotrack.dto.UserProfileUpdateRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles user profile management operations.
 *
 * <p>Phase 3: Duplicate /register and /login endpoints have been removed. Authentication
 * is handled exclusively by {@link AuthController} under /api/auth/**.</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    public UserController(UserService userService,
                          UserRepository userRepository,
                          FileUploadService fileUploadService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse profile = userService.getUserProfile(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile fetched successfully", profile));
    }

    @PostMapping("/profile-picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            String imageUrl = fileUploadService.uploadFile(file);
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setProfilePicture(imageUrl);
            userRepository.save(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "Profile picture updated successfully", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Image upload failed: " + e.getMessage(), null));
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setFullName(request.getFullName());
            user.setRole(request.getRole());
            user.setLocation(request.getLocation());
            user.setCommuteMode(request.getCommuteMode());
            user.setDietPreference(request.getDietPreference());
            user.setCountry(request.getCountry());
            user.setInterests(request.getInterests());
            userRepository.save(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to update profile: " + e.getMessage(), null));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            userService.changePassword(email, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userService.deactivateUser(user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deactivated successfully", null));
    }
}