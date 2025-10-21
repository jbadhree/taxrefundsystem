package com.badhtaxfileserv.service;

public interface PubSubServiceInterface {
    void publishRefundUpdate(String message);
    void publishSendRefund(String message);
}
