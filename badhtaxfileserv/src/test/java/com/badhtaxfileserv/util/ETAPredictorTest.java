package com.badhtaxfileserv.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ETAPredictorTest {
    
    @InjectMocks
    private ETAPredictor etaPredictor;
    
    @Test
    void testPredictETA_ShouldReturnDateWithinRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // When
        LocalDateTime eta = etaPredictor.predictETA();
        
        // Then
        assertNotNull(eta);
        assertTrue(eta.isAfter(now));
        
        long daysDifference = ChronoUnit.DAYS.between(now, eta);
        assertTrue(daysDifference >= 10, "ETA should be at least 10 days from now");
        assertTrue(daysDifference <= 60, "ETA should be at most 60 days from now");
    }
    
    @Test
    void testPredictETA_MultipleCalls_ShouldReturnDifferentValues() {
        // When
        LocalDateTime eta1 = etaPredictor.predictETA();
        LocalDateTime eta2 = etaPredictor.predictETA();
        
        // Then
        // While it's possible they could be the same due to randomness,
        // it's very unlikely with 50-day range
        assertNotEquals(eta1, eta2, "Multiple calls should return different ETAs");
    }
}

