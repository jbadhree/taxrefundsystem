package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.CreateTaxFileRequest;
import com.badhtaxfileserv.dto.TaxFileResponse;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.TaxFile;
import com.badhtaxfileserv.repository.RefundRepository;
import com.badhtaxfileserv.repository.TaxFileRepository;
import com.badhtaxfileserv.util.ETAPredictor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxFileServiceTest {
    
    @Mock
    private TaxFileRepository taxFileRepository;
    
    @Mock
    private RefundRepository refundRepository;
    
    @Mock
    private ETAPredictor etaPredictor;
    
    @InjectMocks
    private TaxFileService taxFileService;
    
    private CreateTaxFileRequest validRequest;
    private TaxFile savedTaxFile;
    private Refund savedRefund;
    
    @BeforeEach
    void setUp() {
        validRequest = CreateTaxFileRequest.builder()
                .userId("user-123")
                .year(2024)
                .income(new BigDecimal("120000.00"))
                .expense(new BigDecimal("20000.00"))
                .taxRate(new BigDecimal("30.0"))
                .deducted(new BigDecimal("25000.00"))
                .refund(new BigDecimal("500.00"))
                .build();
        
        savedTaxFile = TaxFile.builder()
                .id(UUID.randomUUID())
                .userId("user-123")
                .year(2024)
                .income(new BigDecimal("120000.00"))
                .expense(new BigDecimal("20000.00"))
                .taxRate(new BigDecimal("30.0"))
                .deducted(new BigDecimal("25000.00"))
                .refundAmount(new BigDecimal("500.00"))
                .taxStatus(TaxFile.TaxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        savedRefund = Refund.builder()
                .id(UUID.randomUUID())
                .taxFile(savedTaxFile)
                .refundStatus(Refund.RefundStatus.PENDING)
                .refundEta(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        savedTaxFile.setRefund(savedRefund);
    }
    
    @Test
    void createTaxFile_WithRefund_ShouldCreateTaxFileAndRefund() {
        // Given
        when(taxFileRepository.existsByUserIdAndYear(anyString(), any())).thenReturn(false);
        when(taxFileRepository.save(any(TaxFile.class))).thenReturn(savedTaxFile);
        when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);
        when(etaPredictor.predictETA()).thenReturn(LocalDateTime.now().plusDays(30));
        when(taxFileRepository.findByUserIdAndYearWithRefund(anyString(), any()))
                .thenReturn(Optional.of(savedTaxFile));
        
        // When
        TaxFileResponse response = taxFileService.createTaxFile(validRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        assertEquals(2024, response.getYear());
        assertEquals("PENDING", response.getTaxStatus());
        assertEquals("PENDING", response.getRefundStatus());
        assertNotNull(response.getRefundEta());
        
        verify(taxFileRepository).existsByUserIdAndYear("user-123", 2024);
        verify(taxFileRepository).save(any(TaxFile.class));
        verify(refundRepository).save(any(Refund.class));
        verify(etaPredictor).predictETA();
    }
    
    @Test
    void createTaxFile_WithZeroRefund_ShouldCreateTaxFileWithCompletedStatus() {
        // Given
        CreateTaxFileRequest zeroRefundRequest = CreateTaxFileRequest.builder()
                .userId("user-123")
                .year(2024)
                .income(new BigDecimal("120000.00"))
                .expense(new BigDecimal("20000.00"))
                .taxRate(new BigDecimal("30.0"))
                .deducted(new BigDecimal("25000.00"))
                .refund(BigDecimal.ZERO)
                .build();
        
        TaxFile completedTaxFile = TaxFile.builder()
                .id(savedTaxFile.getId())
                .userId("user-123")
                .year(2024)
                .income(new BigDecimal("120000.00"))
                .expense(new BigDecimal("20000.00"))
                .taxRate(new BigDecimal("30.0"))
                .deducted(new BigDecimal("25000.00"))
                .refundAmount(BigDecimal.ZERO)
                .taxStatus(TaxFile.TaxStatus.COMPLETED)
                .refund(null)
                .createdAt(savedTaxFile.getCreatedAt())
                .updatedAt(savedTaxFile.getUpdatedAt())
                .build();
        
        when(taxFileRepository.existsByUserIdAndYear(anyString(), any())).thenReturn(false);
        when(taxFileRepository.save(any(TaxFile.class))).thenReturn(completedTaxFile);
        when(taxFileRepository.findByUserIdAndYearWithRefund(anyString(), any()))
                .thenReturn(Optional.of(completedTaxFile));
        
        // When
        TaxFileResponse response = taxFileService.createTaxFile(zeroRefundRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("COMPLETED", response.getTaxStatus());
        assertNull(response.getRefundStatus());
        assertNull(response.getRefundEta());
        
        verify(taxFileRepository).save(any(TaxFile.class));
        verify(refundRepository, never()).save(any(Refund.class));
        verify(etaPredictor, never()).predictETA();
    }
    
    @Test
    void createTaxFile_WhenTaxFileExists_ShouldThrowException() {
        // Given
        when(taxFileRepository.existsByUserIdAndYear(anyString(), any())).thenReturn(true);
        
        // When & Then
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> taxFileService.createTaxFile(validRequest)
        );
        
        assertTrue(exception.getMessage().contains("Tax file already exists"));
        verify(taxFileRepository, never()).save(any(TaxFile.class));
    }
    
    @Test
    void getTaxFile_WhenExists_ShouldReturnTaxFile() {
        // Given
        when(taxFileRepository.findByUserIdAndYearWithRefund(anyString(), any()))
                .thenReturn(Optional.of(savedTaxFile));
        
        // When
        TaxFileResponse response = taxFileService.getTaxFile("user-123", 2024);
        
        // Then
        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        assertEquals(2024, response.getYear());
        assertEquals("PENDING", response.getTaxStatus());
    }
    
    @Test
    void getTaxFile_WhenNotExists_ShouldThrowException() {
        // Given
        when(taxFileRepository.findByUserIdAndYearWithRefund(anyString(), any()))
                .thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> taxFileService.getTaxFile("user-123", 2024)
        );
        
        assertTrue(exception.getMessage().contains("Tax file not found"));
    }
}
