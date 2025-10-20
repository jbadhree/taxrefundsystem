package com.badhtaxfileserv.dto;

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
public class TaxUserResponse {
    
    private String userId;
    private List<TaxFileSummary> taxFiles;
    private int totalFiles;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxFileSummary {
        private String fileId;
        private Integer year;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal taxRate;
        private BigDecimal deducted;
        private BigDecimal refundAmount;
        private String taxStatus;
        private String refundStatus;
        private LocalDateTime refundEta;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
