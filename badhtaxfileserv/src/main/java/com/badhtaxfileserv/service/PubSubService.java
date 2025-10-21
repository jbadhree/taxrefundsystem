package com.badhtaxfileserv.service;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(name = "pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class PubSubService implements PubSubServiceInterface {

    @Value("${spring.cloud.gcp.project-id:}")
    private String projectId;

    @Value("${pubsub.refund-update-topic:refund-update-from-irs}")
    private String refundUpdateTopic;

    @Value("${pubsub.send-refund-topic:send-refund-to-irs}")
    private String sendRefundTopic;

    private Publisher refundUpdatePublisher;
    private Publisher sendRefundPublisher;

    @PostConstruct
    public void initializePublishers() {
        // Initialize publishers asynchronously to avoid blocking startup
        new Thread(() -> {
            try {
                if (projectId != null && !projectId.isEmpty()) {
                    refundUpdatePublisher = Publisher.newBuilder(
                        TopicName.of(projectId, refundUpdateTopic)
                    ).build();

                    sendRefundPublisher = Publisher.newBuilder(
                        TopicName.of(projectId, sendRefundTopic)
                    ).build();

                    log.info("Initialized Pub/Sub publishers for topics: {}, {}", refundUpdateTopic, sendRefundTopic);
                } else {
                    log.warn("Project ID not configured, Pub/Sub publishers not initialized");
                }
            } catch (IOException e) {
                log.error("Failed to initialize Pub/Sub publishers", e);
            }
        }).start();
    }

    @PreDestroy
    public void shutdownPublishers() {
        try {
            if (refundUpdatePublisher != null) {
                refundUpdatePublisher.shutdown();
                refundUpdatePublisher.awaitTermination(1, TimeUnit.MINUTES);
            }
            if (sendRefundPublisher != null) {
                sendRefundPublisher.shutdown();
                sendRefundPublisher.awaitTermination(1, TimeUnit.MINUTES);
            }
            log.info("Pub/Sub publishers shutdown completed");
        } catch (Exception e) {
            log.error("Error during Pub/Sub publishers shutdown", e);
        }
    }

    public void publishRefundUpdate(String message) {
        publishMessage(refundUpdatePublisher, refundUpdateTopic, message);
    }

    public void publishSendRefund(String message) {
        publishMessage(sendRefundPublisher, sendRefundTopic, message);
    }

    private void publishMessage(Publisher publisher, String topicName, String message) {
        if (publisher == null) {
            log.warn("Publisher for topic {} is not initialized yet, skipping message publish", topicName);
            return;
        }

        try {
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(message))
                .build();

            publisher.publish(pubsubMessage).get(10, TimeUnit.SECONDS);
            log.info("Successfully published message to topic {}: {}", topicName, message);
        } catch (Exception e) {
            log.error("Failed to publish message to topic {}: {}", topicName, message, e);
        }
    }
}
