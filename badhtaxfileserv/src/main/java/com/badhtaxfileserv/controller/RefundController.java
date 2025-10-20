package com.badhtaxfileserv.controller;

import com.badhtaxfileserv.dto.RefundResponse;
import com.badhtaxfileserv.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Refund", description = "Refund status and information operations")
public class RefundController {
    
    private final RefundService refundService;
    
    @GetMapping
    @Operation(summary = "Get refund information", description = "Retrieves refund information by user ID and year, or by file ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refund information retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<RefundResponse> getRefund(
            @Parameter(description = "User ID (optional if fileId is provided)")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Tax year (optional if fileId is provided)")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "File ID (optional if userId and year are provided)")
            @RequestParam(required = false) String fileId) {
        
        log.info("Received request to get refund - userId: {}, year: {}, fileId: {}", userId, year, fileId);
        
        RefundResponse response;
        
        if (fileId != null) {
            response = refundService.getRefundByFileId(fileId);
        } else if (userId != null && year != null) {
            response = refundService.getRefund(userId, year);
        } else {
            throw new IllegalArgumentException("Either fileId or both userId and year must be provided");
        }
        
        return ResponseEntity.ok(response);
    }
}

