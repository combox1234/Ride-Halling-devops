package com.ridehailing.billing.service;

import com.ridehailing.billing.dto.InvoiceResponse;
import com.ridehailing.billing.dto.PaymentReceiptResponse;
import com.ridehailing.billing.dto.ProcessPaymentRequest;
import com.ridehailing.billing.entity.InvoiceEntity;
import com.ridehailing.billing.entity.PaymentTransactionEntity;
import com.ridehailing.billing.gateway.MockPaymentGatewayProvider;
import com.ridehailing.billing.gateway.PaymentGatewayProvider;
import com.ridehailing.billing.publisher.BillingEventPublisher;
import com.ridehailing.billing.repository.InvoiceRepository;
import com.ridehailing.billing.repository.PaymentTransactionRepository;
import com.ridehailing.common.dto.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Spy
    private PaymentGatewayProvider paymentGatewayProvider = new MockPaymentGatewayProvider();

    @Mock
    private BillingEventPublisher eventPublisher;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(
            paymentTransactionRepository,
            invoiceRepository,
            paymentGatewayProvider,
            eventPublisher
        );
    }

    @Test
    void shouldProcessPaymentSuccessfullyAndGenerateInvoice() {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
            101L, 202L, BigDecimal.valueOf(25.50), "PAY-RIDE-101", "CREDIT_CARD"
        );

        when(paymentTransactionRepository.findByIdempotencyKey("PAY-RIDE-101")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.saveAndFlush(any(PaymentTransactionEntity.class))).thenAnswer(i -> {
            PaymentTransactionEntity entity = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", 1L);
            return entity;
        });
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(i -> {
            PaymentTransactionEntity entity = i.getArgument(0);
            if (entity.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", 1L);
            }
            return entity;
        });
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(i -> {
            InvoiceEntity entity = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", 10L);
            return entity;
        });

        PaymentReceiptResponse receipt = billingService.processPayment(request);

        assertNotNull(receipt);
        assertEquals(PaymentStatus.SUCCESS, receipt.status());
        assertEquals(BigDecimal.valueOf(25.50), receipt.amount());
        assertEquals("PAY-RIDE-101", receipt.idempotencyKey());
        assertNotNull(receipt.transactionRef());
        assertTrue(receipt.transactionRef().startsWith("TXN-SIM-"));
        assertNotNull(receipt.invoiceNumber());
        assertTrue(receipt.invoiceNumber().startsWith("INV-101-"));

        verify(paymentGatewayProvider, times(1)).processCharge(any());
        verify(eventPublisher, times(1)).publishPaymentProcessed(any(), isNull());
    }

    @Test
    void shouldBeIdempotentOnDuplicatePaymentRequests() {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
            101L, 202L, BigDecimal.valueOf(25.50), "PAY-RIDE-101", "CREDIT_CARD"
        );

        PaymentTransactionEntity existingTx = new PaymentTransactionEntity(
            "PAY-RIDE-101", 101L, 202L, BigDecimal.valueOf(25.50), PaymentStatus.SUCCESS, "CREDIT_CARD"
        );
        existingTx.setTransactionRef("TXN-SIM-ORIGINAL-1234");
        InvoiceEntity existingInvoice = new InvoiceEntity(
            "INV-101-ORIGINAL", existingTx, 101L, 202L, BigDecimal.valueOf(25.50), "USD"
        );

        when(paymentTransactionRepository.findByIdempotencyKey("PAY-RIDE-101"))
            .thenReturn(Optional.of(existingTx));
        when(invoiceRepository.findByPaymentTransactionId(any()))
            .thenReturn(Optional.of(existingInvoice));

        // Call processPayment with duplicate idempotencyKey
        PaymentReceiptResponse receipt = billingService.processPayment(request);

        assertNotNull(receipt);
        assertEquals(PaymentStatus.SUCCESS, receipt.status());
        assertEquals("PAY-RIDE-101", receipt.idempotencyKey());
        assertEquals("TXN-SIM-ORIGINAL-1234", receipt.transactionRef());
        assertEquals("INV-101-ORIGINAL", receipt.invoiceNumber());

        // CRITICAL IDEMPOTENCY CHECK: Gateway charge is NEVER executed for existing transaction!
        verify(paymentGatewayProvider, never()).processCharge(any());
        verify(eventPublisher, never()).publishPaymentProcessed(any(), any());
    }

    @Test
    void shouldHandleDeclinedPaymentSimulation() {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
            102L, 202L, BigDecimal.valueOf(30.00), "FAIL-CARD-DECLINE-102", "CREDIT_CARD"
        );

        when(paymentTransactionRepository.findByIdempotencyKey("FAIL-CARD-DECLINE-102")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.saveAndFlush(any(PaymentTransactionEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(i -> i.getArgument(0));

        PaymentReceiptResponse receipt = billingService.processPayment(request);

        assertNotNull(receipt);
        assertEquals(PaymentStatus.FAILED, receipt.status());
        assertEquals("CARD_DECLINED", receipt.errorCode());
        assertNull(receipt.invoiceNumber());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldRetrievePaymentByIdempotencyKey() {
        PaymentTransactionEntity tx = new PaymentTransactionEntity(
            "PAY-KEY-999", 999L, 202L, BigDecimal.valueOf(15.00), PaymentStatus.SUCCESS, "WALLET"
        );
        tx.setTransactionRef("TXN-999");
        when(paymentTransactionRepository.findByIdempotencyKey("PAY-KEY-999")).thenReturn(Optional.of(tx));

        PaymentReceiptResponse response = billingService.getPaymentByIdempotencyKey("PAY-KEY-999");
        assertNotNull(response);
        assertEquals("PAY-KEY-999", response.idempotencyKey());
        assertEquals(BigDecimal.valueOf(15.00), response.amount());
    }

    @Test
    void shouldThrowWhenPaymentNotFound() {
        when(paymentTransactionRepository.findByIdempotencyKey("NON_EXISTENT")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> billingService.getPaymentByIdempotencyKey("NON_EXISTENT"));
    }

    @Test
    void shouldRetrieveInvoiceByRideId() {
        PaymentTransactionEntity tx = new PaymentTransactionEntity(
            "PAY-KEY-101", 101L, 202L, BigDecimal.valueOf(20.00), PaymentStatus.SUCCESS, "CREDIT_CARD"
        );
        InvoiceEntity invoice = new InvoiceEntity("INV-101-ABCD", tx, 101L, 202L, BigDecimal.valueOf(20.00), "USD");
        when(invoiceRepository.findByRideId(101L)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = billingService.getInvoiceByRideId(101L);
        assertNotNull(response);
        assertEquals("INV-101-ABCD", response.invoiceNumber());
        assertEquals(101L, response.rideId());
        assertEquals(BigDecimal.valueOf(20.00), response.amount());
    }
}
