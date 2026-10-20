package com.ridehailing.billing.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Safe simulated payment gateway implementation.
 * Never stores, receives, or transmits real credit card numbers, CVVs, or credentials.
 */
@Component
public class MockPaymentGatewayProvider implements PaymentGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayProvider.class);

    @Override
    public PaymentChargeResult processCharge(PaymentChargeRequest request) {
        log.info("Processing simulated payment charge: rideId={}, amount={}, idempotencyKey={}",
            request.rideId(), request.amount(), request.idempotencyKey());

        // Negative or zero amount validation
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Declining simulated payment due to non-positive amount: {}", request.amount());
            return PaymentChargeResult.failure("INVALID_AMOUNT", "Amount must be greater than zero");
        }

        // Test hook: Idempotency keys starting with "FAIL" simulate a decline
        if (request.idempotencyKey() != null && request.idempotencyKey().startsWith("FAIL")) {
            log.warn("Simulating payment decline for test idempotency key: {}", request.idempotencyKey());
            return PaymentChargeResult.failure("CARD_DECLINED", "Simulated card authorization decline");
        }

        String transactionRef = "TXN-SIM-" + UUID.randomUUID();
        log.info("Simulated payment approved successfully: transactionRef={}", transactionRef);
        return PaymentChargeResult.success(transactionRef);
    }
}
