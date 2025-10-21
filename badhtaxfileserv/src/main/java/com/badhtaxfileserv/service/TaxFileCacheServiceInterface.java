package com.badhtaxfileserv.service;

import com.badhtaxfileserv.dto.TaxFileResponse;

public interface TaxFileCacheServiceInterface {
    TaxFileResponse getFromCache(String userId, Integer year);
    void putInCache(String userId, Integer year, TaxFileResponse taxFileResponse);
    void evictFromCache(String userId, Integer year);
    void evictAllForUser(String userId);
    void evictAll();
}
