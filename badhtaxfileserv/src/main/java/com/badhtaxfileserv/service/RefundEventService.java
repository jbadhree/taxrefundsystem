package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.ProcessRefundEventRequest;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.RefundEvent;
import com.badhtaxfileserv.repository.RefundEventRepository;
import com.badhtaxfileserv.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundEventService {
    
    private final RefundRepository refundRepository;
    private final RefundEventRepository refundEventRepository;
    private final TaxFileCacheServiceInterface cacheService;
    
    @Transactional
    public void processRefundEvent(ProcessRefundEventRequest request) {
        log.info("Processing refund event: {} for file ID: {}", request.getType(), request.getFileId());
        
        UUID fileId = UUID.fromString(request.getFileId());
        Refund refund = refundRepository.findByTaxFileId(fileId)
                .orElseThrow(() -> new RuntimeException("Refund not found for file ID: " + request.getFileId()));
        
        RefundEvent.EventType eventType = RefundEvent.EventType.fromValue(request.getType());
        Refund.RefundStatus currentStatus = refund.getRefundStatus();
        
        // Process event based on type
        boolean statusChanged = false;
        switch (eventType) {
            case REFUND_INPROGRESS:
                if (currentStatus == Refund.RefundStatus.PENDING) {
                    refund.setRefundStatus(Refund.RefundStatus.IN_PROGRESS);
                    refundRepository.save(refund);
                    statusChanged = true;
                    log.info("Updated refund status to IN_PROGRESS");
                } else {
                    log.info("Refund already in progress, skipping event");
                }
                break;
                
            case REFUND_APPROVED:
                refund.setRefundStatus(Refund.RefundStatus.APPROVED);
                refundRepository.save(refund);
                statusChanged = true;
                log.info("Updated refund status to APPROVED");
                break;
                
            case REFUND_REJECTED:
                refund.setRefundStatus(Refund.RefundStatus.REJECTED);
                refundRepository.save(refund);
                statusChanged = true;
                log.info("Updated refund status to REJECTED");
                break;
                
            case REFUND_ERROR:
                refund.setRefundStatus(Refund.RefundStatus.ERROR);
                if (request.getData().getErrorReasons() != null) {
                    // Convert error reasons to JSON string
                    refund.setRefundErrors(convertErrorReasonsToJson(request.getData().getErrorReasons()));
                }
                refundRepository.save(refund);
                statusChanged = true;
                log.info("Updated refund status to ERROR");
                break;
        }
        
        // Invalidate cache if status changed
        if (statusChanged) {
            String userId = refund.getTaxFile().getUserId();
            Integer year = refund.getTaxFile().getYear();
            cacheService.evictFromCache(userId, year);
            log.info("Invalidated cache for tax file: userId={}, year={}", userId, year);
        }
        
        // Create event record
        RefundEvent event = RefundEvent.builder()
                .refund(refund)
                .eventType(eventType)
                .eventDate(request.getData().getEventDate() != null ? request.getData().getEventDate() : LocalDateTime.now())
                .errorReasons(request.getData().getErrorReasons() != null ? 
                    convertErrorReasonsToJson(request.getData().getErrorReasons()) : null)
                .build();
        
        refundEventRepository.save(event);
        log.info("Created refund event record");
    }
    
    private String convertErrorReasonsToJson(java.util.List<ProcessRefundEventRequest.EventData.ErrorDetail> errorReasons) {
        // Simple JSON conversion - in production, use a proper JSON library
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < errorReasons.size(); i++) {
            if (i > 0) json.append(",");
            ProcessRefundEventRequest.EventData.ErrorDetail error = errorReasons.get(i);
            json.append("{\"code\":\"").append(error.getCode()).append("\",\"message\":\"").append(error.getMessage()).append("\"}");
        }
        json.append("]");
        return json.toString();
    }
}

