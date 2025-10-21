package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.TaxFileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false", matchIfMissing = false)
public class NoOpTaxFileCacheService implements TaxFileCacheServiceInterface {
    
    @Override
    public TaxFileResponse getFromCache(String userId, Integer year) {
        log.debug("No-op cache: Cache miss for tax file: userId={}, year={}", userId, year);
        return null;
    }
    
    @Override
    public void putInCache(String userId, Integer year, TaxFileResponse taxFileResponse) {
        log.debug("No-op cache: Skipping cache put for tax file: userId={}, year={}", userId, year);
    }
    
    @Override
    public void evictFromCache(String userId, Integer year) {
        log.debug("No-op cache: Skipping cache eviction for tax file: userId={}, year={}", userId, year);
    }
    
    @Override
    public void evictAllForUser(String userId) {
        log.debug("No-op cache: Skipping cache eviction for user: {}", userId);
    }
    
    @Override
    public void evictAll() {
        log.debug("No-op cache: Skipping cache eviction for all");
    }
}
