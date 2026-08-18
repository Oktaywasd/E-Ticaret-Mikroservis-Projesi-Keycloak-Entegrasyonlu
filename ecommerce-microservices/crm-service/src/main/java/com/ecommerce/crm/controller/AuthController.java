package com.ecommerce.crm.controller;

import com.ecommerce.crm.dto.request.UserRegisterRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserProfileService userProfileService;

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserProfileResponse response = userProfileService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}