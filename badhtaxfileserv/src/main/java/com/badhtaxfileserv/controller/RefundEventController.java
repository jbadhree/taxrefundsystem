package com.badhtaxfileserv.controller;

import com.badhtaxfileserv.dto.ProcessRefundEventRequest;
import com.badhtaxfileserv.service.RefundEventService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/processRefundEvent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Refund Event", description = "Process refund events for status updates")
public class RefundEventController {
    
    private final RefundEventService refundEventService;
    
    @PostMapping
    @Operation(summary = "Process refund event", description = "Processes a refund event to update refund status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Refund event processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid event data"),
        @ApiResponse(responseCode = "404", description = "Refund not found")
    })
    public ResponseEntity<Void> processRefundEvent(@Valid @RequestBody ProcessRefundEventRequest request) {
        log.info("Received refund event: {} for file ID: {}", request.getType(), request.getFileId());
        
        refundEventService.processRefundEvent(request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}

