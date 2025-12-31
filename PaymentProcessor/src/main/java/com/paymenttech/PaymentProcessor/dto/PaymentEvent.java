package com.paymenttech.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.paymenttech.domain.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String transactionId;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private String sourceAccount;
    private String destinationAccount;
    private PaymentStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int retryCount;
    private String failureReason;
}
