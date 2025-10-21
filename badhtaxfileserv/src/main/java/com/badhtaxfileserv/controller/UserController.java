package com.badhtaxfileserv.controller;

import com.badhtaxfileserv.dto.AllUsersResponse;
import com.badhtaxfileserv.dto.CreateUserRequest;
import com.badhtaxfileserv.dto.UserResponse;
import com.badhtaxfileserv.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User management operations")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with first name, last name, and user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or user already exists")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Received request to create user with ID: {}", request.getUserId());
        
        UserResponse response = userService.createUser(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves all users in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<AllUsersResponse> getAllUsers() {
        log.info("Received request to get all users");
        
        AllUsersResponse response = userService.getAllUsers();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId) {
        
        log.info("Received request to get user with ID: {}", userId);
        
        UserResponse response = userService.getUserById(userId);
        
        return ResponseEntity.ok(response);
    }
}
