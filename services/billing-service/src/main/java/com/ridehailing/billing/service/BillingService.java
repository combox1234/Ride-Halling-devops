package com.ridehailing.billing.service;

import com.ridehailing.billing.dto.InvoiceResponse;
import com.ridehailing.billing.dto.PaymentReceiptResponse;
import com.ridehailing.billing.dto.ProcessPaymentRequest;
import com.ridehailing.billing.entity.InvoiceEntity;
import com.ridehailing.billing.entity.PaymentTransactionEntity;
import com.ridehailing.billing.gateway.PaymentChargeRequest;
import com.ridehailing.billing.gateway.PaymentChargeResult;
import com.ridehailing.billing.gateway.PaymentGatewayProvider;
import com.ridehailing.billing.publisher.BillingEventPublisher;
import com.ridehailing.billing.repository.InvoiceRepository;
import com.ridehailing.billing.repository.PaymentTransactionRepository;
import com.ridehailing.common.dto.PaymentStatus;
import com.ridehailing.common.event.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGatewayProvider paymentGatewayProvider;
    private final BillingEventPublisher eventPublisher;

    public BillingService(
        PaymentTransactionRepository paymentTransactionRepository,
        InvoiceRepository invoiceRepository,
        PaymentGatewayProvider paymentGatewayProvider,
        BillingEventPublisher eventPublisher
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentGatewayProvider = paymentGatewayProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentReceiptResponse processPayment(ProcessPaymentRequest request) {
        String idempotencyKey = request.idempotencyKey();

        // 1. Check existing transaction by idempotency key (Idempotency check)
        Optional<PaymentTransactionEntity> existingOpt = paymentTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            PaymentTransactionEntity existing = existingOpt.get();
            log.info("Idempotent payment check matched: key={}, status={}, returning cached receipt",
                idempotencyKey, existing.getStatus());
            Optional<InvoiceEntity> invoiceOpt = invoiceRepository.findByPaymentTransactionId(existing.getId());
            return toReceiptResponse(existing, invoiceOpt.orElse(null));
        }

        // 2. Insert initial pending transaction record
        String paymentMethod = (request.paymentMethod() != null && !request.paymentMethod().isBlank())
            ? request.paymentMethod()
            : "CREDIT_CARD";

        PaymentTransactionEntity transaction = new PaymentTransactionEntity(
            idempotencyKey,
            request.rideId(),
            request.passengerId(),
            request.amount(),
            PaymentStatus.PENDING,
            paymentMethod
        );

        try {
            transaction = paymentTransactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent request with same idempotency key won the race
            log.warn("Concurrent duplicate payment insert detected for idempotencyKey={}, re-reading winning record", idempotencyKey);
            PaymentTransactionEntity won = paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Payment transaction constraint violated but record not found", ex));
            Optional<InvoiceEntity> invoiceOpt = invoiceRepository.findByPaymentTransactionId(won.getId());
            return toReceiptResponse(won, invoiceOpt.orElse(null));
        }

        // 3. Invoke payment gateway provider
        PaymentChargeRequest chargeRequest = new PaymentChargeRequest(
            request.rideId(),
            request.passengerId(),
            request.amount(),
            "USD",
            idempotencyKey,
            paymentMethod
        );

        PaymentChargeResult chargeResult = paymentGatewayProvider.processCharge(chargeRequest);

        // 4. Update transaction & create invoice if successful
        InvoiceEntity savedInvoice = null;
        if (chargeResult.success()) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setTransactionRef(chargeResult.transactionRef());
            transaction = paymentTransactionRepository.save(transaction);

            String invoiceNumber = "INV-" + request.rideId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            InvoiceEntity invoice = new InvoiceEntity(
                invoiceNumber,
                transaction,
                request.rideId(),
                request.passengerId(),
                request.amount(),
                "USD"
            );
            savedInvoice = invoiceRepository.save(invoice);

            // 5. Publish PaymentProcessedEvent to RabbitMQ
            Long paymentId = (transaction.getId() != null) ? transaction.getId() : (request.rideId() != null ? request.rideId() : 1L);
            eventPublisher.publishPaymentProcessed(new PaymentProcessedEvent(
                paymentId,
                transaction.getRideId(),
                transaction.getPassengerId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getTransactionRef(),
                transaction.getIdempotencyKey(),
                Instant.now()
            ), null);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setErrorCode(chargeResult.errorCode());
            transaction.setErrorMessage(chargeResult.errorMessage());
            transaction = paymentTransactionRepository.save(transaction);
        }

        return toReceiptResponse(transaction, savedInvoice);
    }

    @Transactional(readOnly = true)
    public PaymentReceiptResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        PaymentTransactionEntity transaction = paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new NoSuchElementException("Payment transaction not found for key: " + idempotencyKey));
        Optional<InvoiceEntity> invoiceOpt = invoiceRepository.findByPaymentTransactionId(transaction.getId());
        return toReceiptResponse(transaction, invoiceOpt.orElse(null));
    }

    @Transactional(readOnly = true)
    public PaymentReceiptResponse getPaymentByRideId(Long rideId) {
        PaymentTransactionEntity transaction = paymentTransactionRepository.findByRideId(rideId)
            .orElseThrow(() -> new NoSuchElementException("Payment transaction not found for ride ID: " + rideId));
        Optional<InvoiceEntity> invoiceOpt = invoiceRepository.findByPaymentTransactionId(transaction.getId());
        return toReceiptResponse(transaction, invoiceOpt.orElse(null));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByRideId(Long rideId) {
        InvoiceEntity invoice = invoiceRepository.findByRideId(rideId)
            .orElseThrow(() -> new NoSuchElementException("Invoice not found for ride ID: " + rideId));
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getPaymentTransaction().getId(),
            invoice.getRideId(),
            invoice.getPassengerId(),
            invoice.getAmount(),
            invoice.getCurrency(),
            invoice.getIssuedAt()
        );
    }

    private PaymentReceiptResponse toReceiptResponse(PaymentTransactionEntity tx, InvoiceEntity inv) {
        String invoiceNumber = (inv != null) ? inv.getInvoiceNumber() : null;
        return new PaymentReceiptResponse(
            tx.getId(),
            tx.getRideId(),
            tx.getPassengerId(),
            tx.getAmount(),
            tx.getStatus(),
            tx.getPaymentMethod(),
            tx.getTransactionRef(),
            tx.getIdempotencyKey(),
            tx.getErrorCode(),
            tx.getErrorMessage(),
            invoiceNumber,
            tx.getCreatedAt()
        );
    }
}
