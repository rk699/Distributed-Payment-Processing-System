package com.paymenttech.PaymentProcessor.service;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.paymenttech.PaymentProcessor.domain.Transaction;
import com.paymenttech.PaymentProcessor.dto.PaymentEvent;
import com.paymenttech.PaymentProcessor.kafka.PaymentProducer;
import com.paymenttech.PaymentProcessor.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {
	
    private final TransactionRepository transactionRepository;
    private final PaymentProducer paymentProducer;
    
    private static final int MAX_RETRIES = 5;
    private static final long BACKOFF_DELAY = 1000; // milliseconds
    
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = 2)
    )
    public void processPaymentWithRetry(PaymentEvent event) {
        try {
            paymentProducer.publishPaymentEvent(event);
            log.info("Payment processed successfully: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Payment processing failed: {}", event.getTransactionId(), e);
            scheduleRetry(event);
            throw e;
        }
    }
    
    private void scheduleRetry(PaymentEvent event) {
        Transaction transaction = transactionRepository.findByPaymentId(event.getTransactionId())
            .orElse(Transaction.builder()
                .paymentId(event.getTransactionId())
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .lastRetryAt(LocalDateTime.now())
                .build());
        
        if (transaction.getRetryCount() < MAX_RETRIES) {
            transaction.setRetryCount(transaction.getRetryCount() + 1);
            transaction.setLastRetryAt(LocalDateTime.now());
            transaction.setErrorLog(transaction.getErrorLog() + "\n[Retry " + transaction.getRetryCount() + "]");
            transactionRepository.save(transaction);
            
            long delayMs = BACKOFF_DELAY * (long) Math.pow(2, transaction.getRetryCount() - 1);
            log.info("Scheduled retry {} for payment {} after {} ms", 
                transaction.getRetryCount(), event.getTransactionId(), delayMs);
        } else {
            log.error("Max retries exceeded for payment: {}", event.getTransactionId());
            paymentProducer.publishToDLQ(event);
        }
    }
    
    public List<Transaction> getPendingRetries() {
        return transactionRepository.findByResolvedAtIsNull();
    }
}