package com.paymenttech.PaymentProcessor.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.paymenttech.PaymentProcessor.domain.Transaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    Optional<Transaction> findByPaymentId(String paymentId);
    
    List<Transaction> findByRetryCountGreaterThan(int retryCount);
    
    List<Transaction> findByResolvedAtIsNull();
}
