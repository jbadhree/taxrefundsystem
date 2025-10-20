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
}

