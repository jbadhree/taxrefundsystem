package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.ProcessRefundEventRequest;
import com.badhtaxfileserv.entity.Refund;
import com.badhtaxfileserv.entity.RefundEvent;
import com.badhtaxfileserv.entity.TaxFile;
import com.badhtaxfileserv.repository.RefundEventRepository;
import com.badhtaxfileserv.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundEventServiceTest {
    
    @Mock
    private RefundRepository refundRepository;
    
    @Mock
    private RefundEventRepository refundEventRepository;
    
    @InjectMocks
    private RefundEventService refundEventService;
    
    private Refund refund;
    private TaxFile taxFile;
    private ProcessRefundEventRequest inProgressRequest;
    private ProcessRefundEventRequest approvedRequest;
    private ProcessRefundEventRequest errorRequest;
    
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
        
        inProgressRequest = ProcessRefundEventRequest.builder()
                .eventId("evt-1")
                .fileId(taxFile.getId().toString())
                .type("refund.inprogress")
                .data(ProcessRefundEventRequest.EventData.builder()
                        .eventDate(LocalDateTime.now())
                        .build())
                .build();
        
        approvedRequest = ProcessRefundEventRequest.builder()
                .eventId("evt-2")
                .fileId(taxFile.getId().toString())
                .type("refund.approved")
                .data(ProcessRefundEventRequest.EventData.builder()
                        .eventDate(LocalDateTime.now())
                        .build())
                .build();
        
        errorRequest = ProcessRefundEventRequest.builder()
                .eventId("evt-3")
                .fileId(taxFile.getId().toString())
                .type("refund.error")
                .data(ProcessRefundEventRequest.EventData.builder()
                        .eventDate(LocalDateTime.now())
                        .errorReasons(List.of(
                                ProcessRefundEventRequest.EventData.ErrorDetail.builder()
                                        .code("ERR001")
                                        .message("Invalid bank account")
                                        .build()
                        ))
                        .build())
                .build();
    }
    
    @Test
    void processRefundEvent_InProgress_WhenPending_ShouldUpdateStatus() {
        // Given
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);
        when(refundEventRepository.save(any(RefundEvent.class))).thenReturn(new RefundEvent());
        
        // When
        refundEventService.processRefundEvent(inProgressRequest);
        
        // Then
        verify(refundRepository).save(argThat(r -> 
                r.getRefundStatus() == Refund.RefundStatus.IN_PROGRESS));
        verify(refundEventRepository).save(any(RefundEvent.class));
    }
    
    @Test
    void processRefundEvent_InProgress_WhenAlreadyInProgress_ShouldNotUpdateStatus() {
        // Given
        refund.setRefundStatus(Refund.RefundStatus.IN_PROGRESS);
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.of(refund));
        when(refundEventRepository.save(any(RefundEvent.class))).thenReturn(new RefundEvent());
        
        // When
        refundEventService.processRefundEvent(inProgressRequest);
        
        // Then
        verify(refundRepository, never()).save(any(Refund.class));
        verify(refundEventRepository).save(any(RefundEvent.class));
    }
    
    @Test
    void processRefundEvent_Approved_ShouldUpdateStatus() {
        // Given
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);
        when(refundEventRepository.save(any(RefundEvent.class))).thenReturn(new RefundEvent());
        
        // When
        refundEventService.processRefundEvent(approvedRequest);
        
        // Then
        verify(refundRepository).save(argThat(r -> 
                r.getRefundStatus() == Refund.RefundStatus.APPROVED));
        verify(refundEventRepository).save(any(RefundEvent.class));
    }
    
    @Test
    void processRefundEvent_Error_ShouldUpdateStatusAndSetErrorReasons() {
        // Given
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);
        when(refundEventRepository.save(any(RefundEvent.class))).thenReturn(new RefundEvent());
        
        // When
        refundEventService.processRefundEvent(errorRequest);
        
        // Then
        verify(refundRepository).save(argThat(r -> 
                r.getRefundStatus() == Refund.RefundStatus.ERROR &&
                r.getRefundErrors() != null &&
                r.getRefundErrors().contains("ERR001")));
        verify(refundEventRepository).save(any(RefundEvent.class));
    }
    
    @Test
    void processRefundEvent_WhenRefundNotFound_ShouldThrowException() {
        // Given
        when(refundRepository.findByTaxFileId(any(UUID.class)))
                .thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refundEventService.processRefundEvent(inProgressRequest)
        );
        
        assertTrue(exception.getMessage().contains("Refund not found"));
        verify(refundRepository, never()).save(any(Refund.class));
    }
}

