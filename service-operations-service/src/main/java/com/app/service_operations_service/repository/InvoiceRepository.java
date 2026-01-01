package com.app.service_operations_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.app.service_operations_service.model.Invoice;
import com.app.service_operations_service.model.enums.PaymentStatus;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    List<Invoice> findByCustomerId(String customerId);
    List<Invoice> findByPaymentStatus(PaymentStatus paymentStatus);
    Optional<Invoice> findByRequestId(String requestId);
    boolean existsByRequestId(String requestId);
}
