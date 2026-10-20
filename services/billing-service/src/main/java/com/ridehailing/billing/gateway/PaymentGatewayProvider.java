package com.ridehailing.billing.gateway;

public interface PaymentGatewayProvider {

    PaymentChargeResult processCharge(PaymentChargeRequest request);
}
