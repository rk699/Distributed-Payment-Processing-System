package com.paymenttech.PaymentProcessor.service;



import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymenttech.PaymentProcessor.domain.Payment;
import com.paymenttech.PaymentProcessor.domain.PaymentStatus;
import com.paymenttech.PaymentProcessor.domain.Transaction;
import com.paymenttech.PaymentProcessor.dto.PaymentEvent;
import com.paymenttech.PaymentProcessor.dto.PaymentRequest;
import com.paymenttech.PaymentProcessor.dto.PaymentResponse;
import com.paymenttech.PaymentProcessor.kafka.PaymentProducer;
import com.paymenttech.PaymentProcessor.repository.PaymentRepository;
import com.paymenttech.PaymentProcessor.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
	
	
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProducer paymentProducer;
    private final IdempotencyService idempotencyService;
    private final CacheService cacheService;
    
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment with idempotency key: {}", request.getIdempotencyKey());
        
        // Check idempotency
        Optional<PaymentResponse> existingPayment = idempotencyService.getIdempotentResult(request.getIdempotencyKey());
        if (existingPayment.isPresent()) {
            log.info("Returning cached payment for idempotency key: {}", request.getIdempotencyKey());
            return existingPayment.get();
        }
        
        // Create payment record
        String transactionId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .sourceAccount(request.getSourceAccount())
                .destinationAccount(request.getDestinationAccount())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        payment = paymentRepository.save(payment);
        
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .paymentId(payment.getId())
                .retryCount(0)
                .lastRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);
        
        // Publish event to Kafka
        PaymentEvent event = PaymentEvent.builder()
                .transactionId(transactionId)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .sourceAccount(request.getSourceAccount())
                .destinationAccount(request.getDestinationAccount())
                .status(PaymentStatus.PENDING)
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .build();
        
        paymentProducer.publishPaymentEvent(event);
        
        // Build response
        PaymentResponse response = PaymentResponse.builder()
                .transactionId(transactionId)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .createdAt(payment.getCreatedAt())
                .message("Payment initiated successfully")
                .build();
        
        // Cache response
        cacheService.cachePayment(request.getIdempotencyKey(), response);
        
        return response;
    }
    
    @Override
    public Optional<PaymentResponse> getPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(this::mapToResponse);
    }
    
    @Override
    @Transactional
    public void handlePaymentSuccess(String transactionId) {
        log.info("Handling payment success for transaction: {}", transactionId);
        
        paymentRepository.findByTransactionId(transactionId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            transactionRepository.findByPaymentId(payment.getId()).ifPresent(transaction -> {
                transaction.setResolvedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
            });
            
            cacheService.cachePayment(payment.getIdempotencyKey(), mapToResponse(payment));
        });
    }
    
    @Override
    @Transactional
    public void handlePaymentFailure(String transactionId, String reason) {
        log.warn("Handling payment failure for transaction: {} with reason: {}", transactionId, reason);
        
        paymentRepository.findByTransactionId(transactionId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProcessedAt(LocalDateTime.now());
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
            
            cacheService.cachePayment(payment.getIdempotencyKey(), mapToResponse(payment));
        });
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
