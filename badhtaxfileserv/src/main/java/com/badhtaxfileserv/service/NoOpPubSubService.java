package com.badhtaxfileserv.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "pubsub.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPubSubService implements PubSubServiceInterface {

    @Override
    public void publishRefundUpdate(String message) {
        log.debug("Pub/Sub disabled - skipping refund update message: {}", message);
    }

    @Override
    public void publishSendRefund(String message) {
        log.debug("Pub/Sub disabled - skipping send refund message: {}", message);
    }
}
