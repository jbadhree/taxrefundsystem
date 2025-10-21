package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.TaxFileResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = true)
public class TaxFileCacheService implements TaxFileCacheServiceInterface {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_PREFIX = "taxfile:";
    private static final long CACHE_TTL_HOURS = 1;
    
    /**
     * Generate cache key for tax file
     */
    private String getCacheKey(String userId, Integer year) {
        return CACHE_PREFIX + userId + ":" + year;
    }
    
    /**
     * Get tax file from cache
     */
    public TaxFileResponse getFromCache(String userId, Integer year) {
        try {
            String cacheKey = getCacheKey(userId, year);
            String cachedValue = (String) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedValue != null) {
                log.debug("Cache hit for tax file: userId={}, year={}", userId, year);
                return objectMapper.readValue(cachedValue, TaxFileResponse.class);
            }
            
            log.debug("Cache miss for tax file: userId={}, year={}", userId, year);
            return null;
        } catch (JsonProcessingException e) {
            log.error("Error deserializing cached tax file for userId={}, year={}", userId, year, e);
            return null;
        }
    }
    
    /**
     * Store tax file in cache
     */
    public void putInCache(String userId, Integer year, TaxFileResponse taxFileResponse) {
        try {
            String cacheKey = getCacheKey(userId, year);
            String jsonValue = objectMapper.writeValueAsString(taxFileResponse);
            
            redisTemplate.opsForValue().set(cacheKey, jsonValue, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Cached tax file: userId={}, year={}", userId, year);
        } catch (JsonProcessingException e) {
            log.error("Error serializing tax file for cache: userId={}, year={}", userId, year, e);
        }
    }
    
    /**
     * Remove tax file from cache
     */
    public void evictFromCache(String userId, Integer year) {
        String cacheKey = getCacheKey(userId, year);
        redisTemplate.delete(cacheKey);
        log.debug("Evicted tax file from cache: userId={}, year={}", userId, year);
    }
    
    /**
     * Evict all tax files for a specific user
     */
    public void evictAllForUser(String userId) {
        try {
            String pattern = CACHE_PREFIX + userId + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("Evicted all tax files from cache for user: {}", userId);
        } catch (Exception e) {
            log.error("Error evicting all tax files for user: {}", userId, e);
        }
    }
    
    /**
     * Evict all tax file caches
     */
    public void evictAll() {
        try {
            String pattern = CACHE_PREFIX + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("Evicted all tax files from cache");
        } catch (Exception e) {
            log.error("Error evicting all tax files from cache", e);
        }
    }
}
