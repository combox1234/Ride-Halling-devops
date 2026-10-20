package com.ridehailing.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.billing.dto.InvoiceResponse;
import com.ridehailing.billing.dto.PaymentReceiptResponse;
import com.ridehailing.billing.dto.ProcessPaymentRequest;
import com.ridehailing.billing.service.BillingService;
import com.ridehailing.common.dto.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillingController.class)
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BillingService billingService;

    @Test
    void shouldProcessPaymentEndpointSuccessfully() throws Exception {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
            100L, 200L, BigDecimal.valueOf(35.00), "PAY-RIDE-100", "CREDIT_CARD"
        );

        PaymentReceiptResponse receipt = new PaymentReceiptResponse(
            1L, 100L, 200L, BigDecimal.valueOf(35.00), PaymentStatus.SUCCESS,
            "CREDIT_CARD", "TXN-SIM-1234", "PAY-RIDE-100", null, null, "INV-100-XYZ", Instant.now()
        );
        when(billingService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(receipt);

        mockMvc.perform(post("/api/v1/billing/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.idempotencyKey").value("PAY-RIDE-100"))
            .andExpect(jsonPath("$.data.transactionRef").value("TXN-SIM-1234"));
    }

    @Test
    void shouldGetPaymentByIdempotencyKey() throws Exception {
        PaymentReceiptResponse receipt = new PaymentReceiptResponse(
            1L, 100L, 200L, BigDecimal.valueOf(35.00), PaymentStatus.SUCCESS,
            "CREDIT_CARD", "TXN-SIM-1234", "PAY-RIDE-100", null, null, "INV-100-XYZ", Instant.now()
        );
        when(billingService.getPaymentByIdempotencyKey("PAY-RIDE-100")).thenReturn(receipt);

        mockMvc.perform(get("/api/v1/billing/payments/PAY-RIDE-100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotencyKey").value("PAY-RIDE-100"));
    }

    @Test
    void shouldGetPaymentByRideId() throws Exception {
        PaymentReceiptResponse receipt = new PaymentReceiptResponse(
            1L, 100L, 200L, BigDecimal.valueOf(35.00), PaymentStatus.SUCCESS,
            "CREDIT_CARD", "TXN-SIM-1234", "PAY-RIDE-100", null, null, "INV-100-XYZ", Instant.now()
        );
        when(billingService.getPaymentByRideId(100L)).thenReturn(receipt);

        mockMvc.perform(get("/api/v1/billing/rides/100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rideId").value(100));
    }

    @Test
    void shouldGetInvoiceByRideId() throws Exception {
        InvoiceResponse invoice = new InvoiceResponse(
            10L, "INV-100-XYZ", 1L, 100L, 200L, BigDecimal.valueOf(35.00), "USD", Instant.now()
        );
        when(billingService.getInvoiceByRideId(100L)).thenReturn(invoice);

        mockMvc.perform(get("/api/v1/billing/invoices/100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.invoiceNumber").value("INV-100-XYZ"))
            .andExpect(jsonPath("$.data.amount").value(35.00));
    }

    @Test
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        when(billingService.getPaymentByIdempotencyKey("UNKNOWN"))
            .thenThrow(new NoSuchElementException("Payment transaction not found for key: UNKNOWN"));

        mockMvc.perform(get("/api/v1/billing/payments/UNKNOWN"))
            .andExpect(status().isNotFound());
    }
}
