package com.paymenttech.kafka;


import com.paymenttech.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {
    
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    private static final String PAYMENT_TOPIC = "payment-events";
    private static final String RETRY_TOPIC = "payment-retry";
    private static final String DLQ_TOPIC = "payment-dlq";
    
    public void publishPaymentEvent(PaymentEvent event) {
        try {
            Message<PaymentEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, PAYMENT_TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getTransactionId())
                    .build();
            
            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish payment event: {}", event.getTransactionId(), ex);
                } else {
                    log.info("Payment event published: {}", event.getTransactionId());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing payment event", e);
            throw new RuntimeException("Payment event publication failed", e);
        }
    }
    
    public void publishToRetryTopic(PaymentEvent event) {
        try {
            Message<PaymentEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, RETRY_TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getTransactionId())
                    .build();
            
            kafkaTemplate.send(message);
            log.info("Retry event published: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error publishing retry event", e);
        }
    }
    
    public void publishToDLQ(PaymentEvent event) {
        try {
            Message<PaymentEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, DLQ_TOPIC)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, event.getTransactionId())
                    .build();
            
            kafkaTemplate.send(message);
            log.error("Payment sent to DLQ: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error publishing to DLQ", e);
        }
    }
}