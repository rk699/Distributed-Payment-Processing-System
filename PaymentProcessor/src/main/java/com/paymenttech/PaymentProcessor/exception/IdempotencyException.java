package com.paymenttech.PaymentProcessor.exception;

public class IdempotencyException extends RuntimeException {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IdempotencyException(String message) {
        super(message);
    }
}