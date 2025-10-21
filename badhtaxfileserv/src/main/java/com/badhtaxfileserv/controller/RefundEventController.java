package com.badhtaxfileserv.controller;

import com.badhtaxfileserv.dto.ProcessRefundEventRequest;
import com.badhtaxfileserv.service.RefundEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;

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
    public ResponseEntity<Void> processRefundEvent(@RequestBody Map<String, Object> requestBody) {
        log.info("Received request: {}", requestBody);
        
        try {
            ProcessRefundEventRequest request;
            
            // Check if this is a Pub/Sub message (has 'message' field)
            if (requestBody.containsKey("message")) {
                log.info("Processing Pub/Sub message");
                request = parsePubSubMessage(requestBody);
            } else {
                // This is a direct request
                log.info("Processing direct request");
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                request = objectMapper.convertValue(requestBody, ProcessRefundEventRequest.class);
            }
            
            log.info("Processed refund event: {} for file ID: {}", request.getType(), request.getFileId());
            
            refundEventService.processRefundEvent(request);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
            
        } catch (Exception e) {
            log.error("Failed to process refund event", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private ProcessRefundEventRequest parsePubSubMessage(Map<String, Object> pubsubMessage) throws Exception {
        // Extract the message data from Pub/Sub format
        String messageData = null;
        if (pubsubMessage.containsKey("message")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) pubsubMessage.get("message");
            if (message.containsKey("data")) {
                String base64Data = (String) message.get("data");
                // Decode base64 data
                messageData = new String(java.util.Base64.getDecoder().decode(base64Data));
            }
        }
        
        if (messageData == null) {
            throw new IllegalArgumentException("No message data found in Pub/Sub message");
        }
        
        log.info("Decoded message data: {}", messageData);
        
        // Parse the JSON message data
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonNode messageJson = objectMapper.readTree(messageData);
        
        // Create ProcessRefundEventRequest from the message
        String eventDateStr = messageJson.get("data").get("eventDate").asText();
        // Parse the date string, handling both with and without timezone
        java.time.LocalDateTime eventDate;
        if (eventDateStr.endsWith("Z")) {
            // Remove 'Z' and parse as LocalDateTime
            eventDate = java.time.LocalDateTime.parse(eventDateStr.substring(0, eventDateStr.length() - 1));
        } else {
            eventDate = java.time.LocalDateTime.parse(eventDateStr);
        }
        
        return ProcessRefundEventRequest.builder()
            .eventId(messageJson.get("eventId").asText())
            .fileId(messageJson.get("fileId").asText())
            .type(messageJson.get("type").asText())
            .data(ProcessRefundEventRequest.EventData.builder()
                .eventDate(eventDate)
                .build())
            .build();
    }
}

