package com.app.service_operations_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.service_operations_service.dto.billing.CreateInvoiceRequest;
import com.app.service_operations_service.dto.billing.InvoiceResponse;
import com.app.service_operations_service.dto.billing.PaymentUpdateRequest;
import com.app.service_operations_service.dto.billing.RevenueReportResponse;
import com.app.service_operations_service.security.RequestUser;
import com.app.service_operations_service.service.BillingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return billingService.createInvoice(request);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceResponse getInvoice(@PathVariable("id") String id) {
        return billingService.getById(id);
    }

    @GetMapping("/invoices/customer/{customerId}")
    public List<InvoiceResponse> getByCustomer(@PathVariable("customerId") String customerId) {
        return billingService.getByCustomer(customerId);
    }

    @GetMapping("/my-invoices")
    public List<InvoiceResponse> getMyInvoices(RequestUser user) {
        String customerId = user.userId();
        return billingService.getByCustomer(customerId);
    }

    @GetMapping("/invoices/request/{requestId}")
    public InvoiceResponse getByRequestId(@PathVariable("requestId") String requestId) {
        return billingService.getByRequestId(requestId);
    }

    @PostMapping("/invoices/{id}/pay")
    public InvoiceResponse payInvoice(@PathVariable("id") String id) {
        return billingService.payInvoice(id);
    }

    @PutMapping("/invoices/{id}/payment")
    public InvoiceResponse updatePayment(@PathVariable("id") String id, @Valid @RequestBody PaymentUpdateRequest request) {
        return billingService.updatePayment(id, request);
    }

    @GetMapping("/reports/revenue")
    public RevenueReportResponse revenueReport() {
        return billingService.revenueReport();
    }
}
