package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.AllUsersResponse;
import com.badhtaxfileserv.dto.CreateUserRequest;
import com.badhtaxfileserv.dto.UserResponse;
import com.badhtaxfileserv.entity.User;
import com.badhtaxfileserv.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with ID: {}", request.getUserId());
        
        // Check if user already exists
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("User with ID " + request.getUserId() + " already exists");
        }
        
        User user = User.builder()
                .userId(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getUserId());
        
        return mapToUserResponse(savedUser);
    }
    
    @Transactional(readOnly = true)
    public AllUsersResponse getAllUsers() {
        log.info("Retrieving all users");
        
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        log.info("Found {} users", users.size());
        
        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
        
        return AllUsersResponse.builder()
                .users(userResponses)
                .totalUsers(userResponses.size())
                .build();
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        log.info("Retrieving user with ID: {}", userId);
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
        
        return mapToUserResponse(user);
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
