package com.ridehailing.billing.controller;

import com.ridehailing.billing.dto.InvoiceResponse;
import com.ridehailing.billing.dto.PaymentReceiptResponse;
import com.ridehailing.billing.dto.ProcessPaymentRequest;
import com.ridehailing.billing.service.BillingService;
import com.ridehailing.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentReceiptResponse>> processPayment(
        @Valid @RequestBody ProcessPaymentRequest request
    ) {
        PaymentReceiptResponse receipt = billingService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success(receipt));
    }

    @GetMapping("/payments/{idempotencyKey}")
    public ResponseEntity<ApiResponse<PaymentReceiptResponse>> getPaymentByIdempotencyKey(
        @PathVariable String idempotencyKey
    ) {
        PaymentReceiptResponse receipt = billingService.getPaymentByIdempotencyKey(idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(receipt));
    }

    @GetMapping("/rides/{rideId}")
    public ResponseEntity<ApiResponse<PaymentReceiptResponse>> getPaymentByRideId(
        @PathVariable Long rideId
    ) {
        PaymentReceiptResponse receipt = billingService.getPaymentByRideId(rideId);
        return ResponseEntity.ok(ApiResponse.success(receipt));
    }

    @GetMapping("/invoices/{rideId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByRideId(
        @PathVariable Long rideId
    ) {
        InvoiceResponse invoice = billingService.getInvoiceByRideId(rideId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
