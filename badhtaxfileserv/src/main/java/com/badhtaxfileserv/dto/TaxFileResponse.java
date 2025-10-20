package com.badhtaxfileserv.dto;

import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.TaxFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxFileResponse {
    
    private String fileId;
    private String userId;
    private Integer year;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal taxRate;
    private BigDecimal deducted;
    private BigDecimal refund;
    private String taxStatus;
    private String refundStatus;
    private List<ErrorDetail> refundErrors;
    private LocalDateTime refundEta;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
    }
    
    public static TaxFileResponse fromEntity(TaxFile taxFile) {
        TaxFileResponseBuilder builder = TaxFileResponse.builder()
                .fileId(taxFile.getId().toString())
                .userId(taxFile.getUserId())
                .year(taxFile.getYear())
                .income(taxFile.getIncome())
                .expense(taxFile.getExpense())
                .taxRate(taxFile.getTaxRate())
                .deducted(taxFile.getDeducted())
                .refund(taxFile.getRefundAmount())
                .taxStatus(taxFile.getTaxStatus().name())
                .createdAt(taxFile.getCreatedAt())
                .updatedAt(taxFile.getUpdatedAt());
        
        if (taxFile.getRefund() != null) {
            Refund refund = taxFile.getRefund();
            builder.refundStatus(refund.getRefundStatus().name())
                   .refundEta(refund.getRefundEta());
            
            // Parse refund errors if present
            if (refund.getRefundErrors() != null && !refund.getRefundErrors().isEmpty()) {
                // For now, we'll handle this in the service layer
                builder.refundErrors(List.of());
            } else {
                builder.refundErrors(List.of());
            }
        } else {
            builder.refundStatus(null)
                   .refundEta(null)
                   .refundErrors(List.of());
        }
        
        return builder.build();
    }
}

