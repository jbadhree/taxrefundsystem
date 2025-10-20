package com.badhtaxfileserv.dto;

import com.badhtaxfileserv.entity.Refund;
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
public class RefundResponse {
    
    private String fileId;
    private String userId;
    private Integer year;
    private String refundStatus;
    private List<ErrorDetail> errors;
    private LocalDateTime eta;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
    }
    
    public static RefundResponse fromEntity(Refund refund) {
        RefundResponseBuilder builder = RefundResponse.builder()
                .fileId(refund.getTaxFile().getId().toString())
                .userId(refund.getTaxFile().getUserId())
                .year(refund.getTaxFile().getYear())
                .refundStatus(refund.getRefundStatus().name())
                .eta(refund.getRefundEta());
        
        // Parse refund errors if present
        if (refund.getRefundErrors() != null && !refund.getRefundErrors().isEmpty()) {
            // For now, we'll handle this in the service layer
            builder.errors(List.of());
        } else {
            builder.errors(List.of());
        }
        
        return builder.build();
    }
}

