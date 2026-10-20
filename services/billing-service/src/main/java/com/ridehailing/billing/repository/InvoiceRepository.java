package com.ridehailing.billing.repository;

import com.ridehailing.billing.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);

    Optional<InvoiceEntity> findByRideId(Long rideId);

    Optional<InvoiceEntity> findByPaymentTransactionId(Long paymentTransactionId);
}
