package com.paymenttech.PaymentProcessor.domain;


public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    RETRY_SCHEDULED
}