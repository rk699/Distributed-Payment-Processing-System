package com.paymenttech.PaymentProcessor.kafka;

import com.paymenttech.PaymentProcessor.dto.PaymentEvent;
import com.paymenttech.PaymentProcessor.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {
    
    private final PaymentService paymentService = null;
    private final Random random = new Random();
    
    @KafkaListener(topics = "payment-events", groupId = "payment-processor-group", concurrency = "10")
    public void processPaymentEvent(PaymentEvent event) {
        try {
            log.info("Processing payment event: {}", event.getTransactionId());
            
            // Simulate payment processing with random success (simulating 95% success rate)
            if (random.nextDouble() < 0.95) {
                paymentService.handlePaymentSuccess(event.getTransactionId());
                log.info("Payment processed successfully: {}", event.getTransactionId());
            } else {
                paymentService.handlePaymentFailure(event.getTransactionId(), "Simulated processing failure");
                log.warn("Payment processing failed: {}", event.getTransactionId());
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", event.getTransactionId(), e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }
    
    @KafkaListener(topics = "payment-retry", groupId = "payment-retry-group", concurrency = "5")
    public void processRetryEvent(PaymentEvent event) {
        try {
            log.info("Processing retry event: {} (attempt {})", event.getTransactionId(), event.getRetryCount());
            
            if (random.nextDouble() < 0.98) {
                paymentService.handlePaymentSuccess(event.getTransactionId());
            } else {
                paymentService.handlePaymentFailure(event.getTransactionId(), "Retry failed");
            }
        } catch (Exception e) {
            log.error("Error processing retry event", e);
        }
    }
    
    @KafkaListener(topics = "payment-dlq", groupId = "payment-dlq-group")
    public void processDLQEvent(PaymentEvent event) {
        log.error("Payment sent to DLQ - Manual intervention required: {}", event.getTransactionId());
        // TODO: Send alert to ops team
    }
}