package com.paymenttech.service;


import com.paymenttech.dto.PaymentRequest;
import com.paymenttech.dto.PaymentResponse;
import java.util.Optional;

public interface PaymentService {
    
    PaymentResponse processPayment(PaymentRequest request);
    
    Optional<PaymentResponse> getPaymentStatus(String transactionId);
    
    void handlePaymentSuccess(String transactionId);
    
    void handlePaymentFailure(String transactionId, String reason);
}
