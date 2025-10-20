package com.badhtaxfileserv.controller;

import com.badhtaxfileserv.dto.CreateTaxFileRequest;
import com.badhtaxfileserv.dto.TaxFileResponse;
import com.badhtaxfileserv.dto.TaxUserResponse;
import com.badhtaxfileserv.service.TaxFileService;
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
@RequestMapping("/taxFile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tax File", description = "Tax file management operations")
public class TaxFileController {
    
    private final TaxFileService taxFileService;
    
    @PostMapping
    @Operation(summary = "Create a new tax file", description = "Creates a new tax file for the specified user and year")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tax file created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Tax file already exists for this user and year")
    })
    public ResponseEntity<TaxFileResponse> createTaxFile(@Valid @RequestBody CreateTaxFileRequest request) {
        log.info("Received request to create tax file for user: {} and year: {}", request.getUserId(), request.getYear());
        
        TaxFileResponse response = taxFileService.createTaxFile(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get tax file", description = "Retrieves a tax file by user ID and year")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tax file retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Tax file not found")
    })
    public ResponseEntity<TaxFileResponse> getTaxFile(
            @Parameter(description = "User ID", required = true)
            @RequestParam String userId,
            @Parameter(description = "Tax year", required = true)
            @RequestParam Integer year) {
        
        log.info("Received request to get tax file for user: {} and year: {}", userId, year);
        
        TaxFileResponse response = taxFileService.getTaxFile(userId, year);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/taxUser")
    @Operation(summary = "Get all tax files for a user", description = "Retrieves all tax files for the specified user ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tax files retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "404", description = "No tax files found for user")
    })
    public ResponseEntity<TaxUserResponse> getTaxFilesByUserId(
            @Parameter(description = "User ID", required = true)
            @RequestParam String userId) {
        
        log.info("Received request to get all tax files for user: {}", userId);
        
        TaxUserResponse response = taxFileService.getTaxFilesByUserId(userId);
        
        return ResponseEntity.ok(response);
    }
}

