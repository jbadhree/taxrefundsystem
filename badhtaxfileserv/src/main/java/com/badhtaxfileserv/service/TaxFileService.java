package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.CreateTaxFileRequest;
import com.badhtaxfileserv.dto.TaxFileResponse;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.TaxFile;
import com.badhtaxfileserv.repository.RefundRepository;
import com.badhtaxfileserv.repository.TaxFileRepository;
import com.badhtaxfileserv.util.ETAPredictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxFileService {
    
    private final TaxFileRepository taxFileRepository;
    private final RefundRepository refundRepository;
    private final ETAPredictor etaPredictor;
    
    @Transactional
    public TaxFileResponse createTaxFile(CreateTaxFileRequest request) {
        log.info("Creating tax file for user: {} and year: {}", request.getUserId(), request.getYear());
        
        // Check if tax file already exists
        if (taxFileRepository.existsByUserIdAndYear(request.getUserId(), request.getYear())) {
            throw new DataIntegrityViolationException("Tax file already exists for user: " + request.getUserId() + " and year: " + request.getYear());
        }
        
        // Create tax file
        TaxFile.TaxStatus taxStatus = request.getRefund().compareTo(BigDecimal.ZERO) > 0 
            ? TaxFile.TaxStatus.PENDING 
            : TaxFile.TaxStatus.COMPLETED;
            
        TaxFile taxFile = TaxFile.builder()
                .userId(request.getUserId())
                .year(request.getYear())
                .income(request.getIncome())
                .expense(request.getExpense())
                .taxRate(request.getTaxRate())
                .deducted(request.getDeducted())
                .refundAmount(request.getRefund())
                .taxStatus(taxStatus)
                .build();
        
        taxFile = taxFileRepository.save(taxFile);
        log.info("Created tax file with ID: {}", taxFile.getId());
        
        // Create refund record if refund amount > 0
        if (request.getRefund().compareTo(BigDecimal.ZERO) > 0) {
            Refund refund = Refund.builder()
                    .taxFile(taxFile)
                    .refundStatus(Refund.RefundStatus.PENDING)
                    .refundEta(etaPredictor.predictETA())
                    .build();
            
            refundRepository.save(refund);
            log.info("Created refund record with ETA: {}", refund.getRefundEta());
        }
        
        // Fetch the complete tax file with refund
        TaxFile completeTaxFile = taxFileRepository.findByUserIdAndYearWithRefund(request.getUserId(), request.getYear())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created tax file"));
        
        return TaxFileResponse.fromEntity(completeTaxFile);
    }
    
    public TaxFileResponse getTaxFile(String userId, Integer year) {
        log.info("Retrieving tax file for user: {} and year: {}", userId, year);
        
        TaxFile taxFile = taxFileRepository.findByUserIdAndYearWithRefund(userId, year)
                .orElseThrow(() -> new RuntimeException("Tax file not found for user: " + userId + " and year: " + year));
        
        return TaxFileResponse.fromEntity(taxFile);
    }
}

