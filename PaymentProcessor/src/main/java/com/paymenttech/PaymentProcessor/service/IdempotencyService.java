package com.paymenttech.PaymentProcessor.service;


import com.paymenttech.PaymentProcessor.domain.Payment;
import com.paymenttech.PaymentProcessor.dto.PaymentResponse;
import com.paymenttech.PaymentProcessor.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    
    private final PaymentRepository paymentRepository;
    private final CacheService cacheService ;
    
    public Optional<PaymentResponse> getIdempotentResult(String idempotencyKey) {
        log.debug("Checking idempotency for key: {}", idempotencyKey);
        
        // Check cache first
        PaymentResponse cachedResult = cacheService.getCachedPayment(idempotencyKey);
        if (cachedResult != null) {
            log.info("Idempotency cache hit for key: {}", idempotencyKey);
            return Optional.of(cachedResult);
        }
        
        // Check database
        Optional<Payment> payment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (payment.isPresent()) {
            log.info("Idempotency database hit for key: {}", idempotencyKey);
            PaymentResponse response = mapToResponse(payment.get());
            cacheService.cachePayment(idempotencyKey, response);
            return Optional.of(response);
        }
        
        return Optional.empty();
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .transactionId(payment.getTransactionId())
                .idempotencyKey(payment.getIdempotencyKey())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .processedAt(payment.getProcessedAt())
                .message("Payment " + payment.getStatus().toString().toLowerCase())
                .build();
    }
}
