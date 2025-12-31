package com.paymenttech.PaymentProcessor.repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.paymenttech.PaymentProcessor.domain.Payment;
import com.paymenttech.PaymentProcessor.domain.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt BETWEEN :startTime AND :endTime")
    List<Payment> findByStatusAndDateRange(PaymentStatus status, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt >= :since")
    long countByStatusSince(PaymentStatus status, LocalDateTime since);
}
