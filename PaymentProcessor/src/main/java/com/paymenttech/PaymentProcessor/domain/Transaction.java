package com.paymenttech.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_payment_id", columnList = "paymentId"),
    @Index(name = "idx_retry_count", columnList = "retryCount")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String paymentId;
    
    @Column(nullable = false)
    private int retryCount;
    
    @Column(nullable = false)
    private LocalDateTime lastRetryAt;
    
    @Column(columnDefinition = "TEXT")
    private String errorLog;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime resolvedAt;
}