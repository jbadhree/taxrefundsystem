package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.CreateTaxFileRequest;
import com.badhtaxfileserv.dto.TaxFileResponse;
import com.badhtaxfileserv.dto.TaxUserResponse;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.TaxFile;
import com.badhtaxfileserv.entity.User;
import com.badhtaxfileserv.repository.RefundRepository;
import com.badhtaxfileserv.repository.TaxFileRepository;
import com.badhtaxfileserv.repository.UserRepository;
import com.badhtaxfileserv.util.ETAPredictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxFileService {
    
    private final TaxFileRepository taxFileRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final ETAPredictor etaPredictor;
    private final PubSubServiceInterface pubSubService;
    private final TaxFileCacheServiceInterface cacheService;
    
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
            
            refund = refundRepository.save(refund);
            log.info("Created refund record with ETA: {}", refund.getRefundEta());
            
            // Send Pub/Sub message for refund creation
            // Message format matches batch job database structure: file_id, status, error_message
            try {
                String message = String.format(
                    "{\"file_id\":\"%s\",\"status\":\"%s\",\"error_message\":null,\"refund_amount\":\"%s\",\"user_id\":\"%s\",\"year\":%d,\"eta\":\"%s\",\"timestamp\":\"%s\"}",
                    taxFile.getId(),  // file_id - matches batch job database field
                    "pending",        // status - matches batch job database field  
                    request.getRefund().toString(),  // Convert to string
                    taxFile.getUserId(),
                    taxFile.getYear(),
                    refund.getRefundEta(),
                    java.time.Instant.now().toString()
                );
                
                log.info("About to call pubSubService.publishSendRefund with message: {}", message);
                pubSubService.publishSendRefund(message);
                log.info("Published refund creation event to Pub/Sub for file ID: {}", taxFile.getId());
            } catch (Exception e) {
                log.error("Failed to publish refund creation event for file ID: {}", taxFile.getId(), e);
                // Don't fail the transaction if Pub/Sub fails
            }
        }
        
        // Fetch the complete tax file with refund
        TaxFile completeTaxFile = taxFileRepository.findByUserIdAndYearWithRefund(request.getUserId(), request.getYear())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created tax file"));
        
        TaxFileResponse response = TaxFileResponse.fromEntity(completeTaxFile);
        
        // Cache the response for future reads
        cacheService.putInCache(request.getUserId(), request.getYear(), response);
        
        return response;
    }
    
    public TaxFileResponse getTaxFile(String userId, Integer year) {
        log.info("Retrieving tax file for user: {} and year: {}", userId, year);
        
        // Try to get from cache first
        TaxFileResponse cachedResponse = cacheService.getFromCache(userId, year);
        if (cachedResponse != null) {
            log.debug("Returning cached tax file for user: {} and year: {}", userId, year);
            return cachedResponse;
        }
        
        // Cache miss - fetch from database
        TaxFile taxFile = taxFileRepository.findByUserIdAndYearWithRefund(userId, year)
                .orElseThrow(() -> new RuntimeException("Tax file not found for user: " + userId + " and year: " + year));
        
        TaxFileResponse response = TaxFileResponse.fromEntity(taxFile);
        
        // Cache the response for future reads
        cacheService.putInCache(userId, year, response);
        
        return response;
    }
    
    public TaxUserResponse getTaxFilesByUserId(String userId) {
        log.info("Retrieving all tax files for user: {}", userId);
        
        // Fetch user information
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        List<TaxFile> taxFiles = taxFileRepository.findByUserIdWithRefund(userId);
        
        List<TaxUserResponse.TaxFileSummary> taxFileSummaries = taxFiles.stream()
                .map(this::convertToTaxFileSummary)
                .collect(Collectors.toList());
        
        return TaxUserResponse.builder()
                .userId(userId)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .taxFiles(taxFileSummaries)
                .totalFiles(taxFiles.size())
                .build();
    }
    
    private TaxUserResponse.TaxFileSummary convertToTaxFileSummary(TaxFile taxFile) {
        TaxUserResponse.TaxFileSummary.TaxFileSummaryBuilder builder = TaxUserResponse.TaxFileSummary.builder()
                .fileId(taxFile.getId().toString())
                .year(taxFile.getYear())
                .income(taxFile.getIncome())
                .expense(taxFile.getExpense())
                .taxRate(taxFile.getTaxRate())
                .deducted(taxFile.getDeducted())
                .refundAmount(taxFile.getRefundAmount())
                .taxStatus(taxFile.getTaxStatus().name())
                .createdAt(taxFile.getCreatedAt())
                .updatedAt(taxFile.getUpdatedAt());
        
        if (taxFile.getRefund() != null) {
            Refund refund = taxFile.getRefund();
            builder.refundStatus(refund.getRefundStatus().name())
                   .refundEta(refund.getRefundEta());
        } else {
            builder.refundStatus(null)
                   .refundEta(null);
        }
        
        return builder.build();
    }
}

