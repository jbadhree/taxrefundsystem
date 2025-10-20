package com.badhtaxfileserv.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
public class ETAPredictor {
    
    private static final Random RANDOM = new Random();
    private static final int MIN_DAYS = 10;
    private static final int MAX_DAYS = 60;
    
    public LocalDateTime predictETA() {
        int daysToAdd = RANDOM.nextInt(MAX_DAYS - MIN_DAYS + 1) + MIN_DAYS;
        return LocalDateTime.now().plusDays(daysToAdd);
    }
}

