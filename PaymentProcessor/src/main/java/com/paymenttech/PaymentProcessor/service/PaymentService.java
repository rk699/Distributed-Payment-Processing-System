package com.paymenttech.PaymentProcessor.service;


import java.util.Optional;

import com.paymenttech.PaymentProcessor.dto.PaymentRequest;
import com.paymenttech.PaymentProcessor.dto.PaymentResponse;

public interface PaymentService {
    
    PaymentResponse processPayment(PaymentRequest request);
    
    Optional<PaymentResponse> getPaymentStatus(String transactionId);
    
    void handlePaymentSuccess(String transactionId);
    
    void handlePaymentFailure(String transactionId, String reason);
}
