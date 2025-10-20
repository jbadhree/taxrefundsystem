package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.RefundResponse;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.TaxFile;
import com.badhtaxfileserv.repository.RefundRepository;
import com.badhtaxfileserv.repository.TaxFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    
    @Mock
    private RefundRepository refundRepository;
    
    @Mock
    private TaxFileRepository taxFileRepository;
    
    @InjectMocks
    private RefundService refundService;
    
    private Refund refund;
    private TaxFile taxFile;
    
    @BeforeEach
    void setUp() {
        taxFile = TaxFile.builder()
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
        
        refund = Refund.builder()
                .id(UUID.randomUUID())
                .taxFile(taxFile)
                .refundStatus(Refund.RefundStatus.PENDING)
                .refundEta(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void getRefund_ByUserIdAndYear_WhenExists_ShouldReturnRefund() {
        // Given
        when(refundRepository.findByUserIdAndYear(anyString(), any()))
                .thenReturn(Optional.of(refund));
        
        // When
        RefundResponse response = refundService.getRefund("user-123", 2024);
        
        // Then
        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        assertEquals(2024, response.getYear());
        assertEquals("PENDING", response.getRefundStatus());
        assertNotNull(response.getEta());
        
        verify(refundRepository).findByUserIdAndYear("user-123", 2024);
    }
    
    @Test
    void getRefund_ByUserIdAndYear_WhenNotExists_ShouldThrowException() {
        // Given
        when(refundRepository.findByUserIdAndYear(anyString(), any()))
                .thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refundService.getRefund("user-123", 2024)
        );
        
        assertTrue(exception.getMessage().contains("Refund not found"));
        verify(refundRepository).findByUserIdAndYear("user-123", 2024);
    }
    
    @Test
    void getRefundByFileId_WhenExists_ShouldReturnRefund() {
        // Given
        String fileId = taxFile.getId().toString();
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.of(refund));
        
        // When
        RefundResponse response = refundService.getRefundByFileId(fileId);
        
        // Then
        assertNotNull(response);
        assertEquals(fileId, response.getFileId());
        assertEquals("user-123", response.getUserId());
        assertEquals(2024, response.getYear());
        assertEquals("PENDING", response.getRefundStatus());
        
        verify(refundRepository).findByTaxFileId(UUID.fromString(fileId));
    }
    
    @Test
    void getRefundByFileId_WhenNotExists_ShouldThrowException() {
        // Given
        String fileId = UUID.randomUUID().toString();
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refundService.getRefundByFileId(fileId)
        );
        
        assertTrue(exception.getMessage().contains("Refund not found"));
        verify(refundRepository).findByTaxFileId(UUID.fromString(fileId));
    }
}

