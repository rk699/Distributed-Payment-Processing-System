package com.paymenttech.repository;


import com.paymenttech.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    Optional<Transaction> findByPaymentId(String paymentId);
    
    List<Transaction> findByRetryCountGreaterThan(int retryCount);
    
    List<Transaction> findByResolvedAtIsNull();
}
