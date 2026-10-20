package com.ridehailing.billing.gateway;

public record PaymentChargeResult(
    boolean success,
    String transactionRef,
    String errorCode,
    String errorMessage
) {
    public static PaymentChargeResult success(String transactionRef) {
        return new PaymentChargeResult(true, transactionRef, null, null);
    }

    public static PaymentChargeResult failure(String errorCode, String errorMessage) {
        return new PaymentChargeResult(false, null, errorCode, errorMessage);
    }
}
