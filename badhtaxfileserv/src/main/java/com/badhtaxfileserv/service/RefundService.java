package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.RefundResponse;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.repository.RefundRepository;
import com.badhtaxfileserv.repository.TaxFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final RefundRepository refundRepository;
    private final TaxFileRepository taxFileRepository;
    private final PubSubServiceInterface pubSubService;
    
    public RefundResponse getRefund(String userId, Integer year) {
        log.info("Retrieving refund for user: {} and year: {}", userId, year);
        
        Refund refund = refundRepository.findByUserIdAndYear(userId, year)
                .orElseThrow(() -> new RuntimeException("Refund not found for user: " + userId + " and year: " + year));
        
        return RefundResponse.fromEntity(refund);
    }
    
    public RefundResponse getRefundByFileId(String fileId) {
        log.info("Retrieving refund for file ID: {}", fileId);
        
        UUID uuid = UUID.fromString(fileId);
        Refund refund = refundRepository.findByTaxFileId(uuid)
                .orElseThrow(() -> new RuntimeException("Refund not found for file ID: " + fileId));
        
        return RefundResponse.fromEntity(refund);
    }
    
    public void publishRefundUpdateEvent(Refund refund) {
        try {
            String message = String.format(
                "{\"refundId\":\"%s\",\"taxFileId\":\"%s\",\"status\":\"%s\",\"eta\":\"%s\",\"timestamp\":\"%s\"}",
                refund.getId(),
                refund.getTaxFile() != null ? refund.getTaxFile().getId() : null,
                refund.getRefundStatus(),
                refund.getRefundEta(),
                java.time.Instant.now().toString()
            );
            
            pubSubService.publishRefundUpdate(message);
            log.info("Published refund update event for refund ID: {}", refund.getId());
        } catch (Exception e) {
            log.error("Failed to publish refund update event for refund ID: {}", refund.getId(), e);
        }
    }
    
    public void publishSendRefundEvent(Refund refund) {
        try {
            String message = String.format(
                "{\"refundId\":\"%s\",\"taxFileId\":\"%s\",\"status\":\"%s\",\"eta\":\"%s\",\"timestamp\":\"%s\"}",
                refund.getId(),
                refund.getTaxFile() != null ? refund.getTaxFile().getId() : null,
                refund.getRefundStatus(),
                refund.getRefundEta(),
                java.time.Instant.now().toString()
            );
            
            pubSubService.publishSendRefund(message);
            log.info("Published send refund event for refund ID: {}", refund.getId());
        } catch (Exception e) {
            log.error("Failed to publish send refund event for refund ID: {}", refund.getId(), e);
        }
    }
}

