package com.badhtaxfileserv.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnMissingBean(PubSubService.class)
public class NoOpPubSubService implements PubSubServiceInterface {

    public NoOpPubSubService() {
        log.info("NoOpPubSubService constructor called - using no-op implementation");
    }

    @Override
    public void publishRefundUpdate(String message) {
        log.info("Pub/Sub disabled - skipping refund update message: {}", message);
    }

    @Override
    public void publishSendRefund(String message) {
        log.info("Pub/Sub disabled - skipping send refund message: {}", message);
    }
}
