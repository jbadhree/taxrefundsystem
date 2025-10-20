package com.badhtaxfileserv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRefundEventRequest {
    
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotBlank(message = "File ID is required")
    private String fileId;
    
    @NotBlank(message = "Event type is required")
    private String type;
    
    @NotNull(message = "Event data is required")
    private EventData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventData {
        private LocalDateTime eventDate;
        private List<ErrorDetail> errorReasons;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ErrorDetail {
            private String code;
            private String message;
        }
    }
}

