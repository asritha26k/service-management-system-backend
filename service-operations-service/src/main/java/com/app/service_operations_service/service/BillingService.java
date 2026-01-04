package com.app.service_operations_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.service_operations_service.dto.billing.CreateInvoiceRequest;
import com.app.service_operations_service.dto.billing.InvoiceResponse;
import com.app.service_operations_service.dto.billing.MonthlyRevenueEntry;
import com.app.service_operations_service.dto.billing.PaymentUpdateRequest;
import com.app.service_operations_service.dto.billing.RevenueReportResponse;
import com.app.service_operations_service.exception.BadRequestException;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.Invoice;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.PaymentStatus;
import com.app.service_operations_service.repository.InvoiceRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;

@Service
@Transactional
public class BillingService {

    private static final String INVOICE_NOT_FOUND = "Invoice not found: ";

    private final InvoiceRepository invoiceRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceItemRepository serviceItemRepository;

    public BillingService(InvoiceRepository invoiceRepository, ServiceRequestRepository requestRepository,
                         ServiceItemRepository serviceItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.requestRepository = requestRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        if (!requestRepository.existsById(request.getRequestId())) {
            throw new BadRequestException("Service request not found for invoice: " + request.getRequestId());
        }
        
        // Validate amounts are positive
        if (request.getServiceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Service amount must be positive");
        }
        if (request.getTaxAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Tax amount cannot be negative");
        }
        if (request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Total amount must be positive");
        }
        
        Invoice invoice = new Invoice();
        invoice.setRequestId(request.getRequestId());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setServiceAmount(request.getServiceAmount());
        invoice.setTaxAmount(request.getTaxAmount());
        invoice.setTotalAmount(request.getTotalAmount());
        return toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse getById(String id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND + id));
        return toResponse(invoice);
    }

    public List<InvoiceResponse> getByCustomer(String customerId) {
        return invoiceRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public InvoiceResponse updatePayment(String id, PaymentUpdateRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND + id));
        invoice.setPaymentStatus(request.getPaymentStatus());
        invoice.setPaymentMethod(request.getPaymentMethod());
        if (request.getPaymentStatus() == PaymentStatus.PAID && invoice.getPaidAt() == null) {
            invoice.setPaidAt(Instant.now());
        }
        return toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse getByRequestId(String requestId) {
        Invoice invoice = invoiceRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Invoice not found for request: " + requestId));
        return toResponse(invoice);
    }

    public InvoiceResponse generateInvoiceForCompletedRequest(String requestId) {
        // Check if invoice already exists
        if (invoiceRepository.existsByRequestId(requestId)) {
            return invoiceRepository.findByRequestId(requestId)
                    .map(this::toResponse)
                    .orElseThrow(() -> new NotFoundException("Invoice not found for request: " + requestId));
        }

        // Get the service request
        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + requestId));

        // Get the service item for pricing
        ServiceItem serviceItem = serviceItemRepository.findById(serviceRequest.getServiceId())
                .orElse(null);

        // Use default pricing if service item not found or has no price
        BigDecimal serviceAmount;
        if (serviceItem == null || serviceItem.getBasePrice() == null) {
            serviceAmount = new BigDecimal("100.00"); // Default service amount
        } else {
            serviceAmount = serviceItem.getBasePrice();
        }

        // Calculate amounts
        BigDecimal taxRate = new BigDecimal("0.10"); // 10% tax
        BigDecimal taxAmount = serviceAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = serviceAmount.add(taxAmount);

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setRequestId(requestId);
        invoice.setCustomerId(serviceRequest.getCustomerId());
        invoice.setServiceAmount(serviceAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        return toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse payInvoice(String id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND + id));
        
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Invoice is already paid");
        }
        
        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setPaymentMethod("Online Payment");
        invoice.setPaidAt(Instant.now());
        
        return toResponse(invoiceRepository.save(invoice));
    }

    public RevenueReportResponse revenueReport() {
        List<Invoice> invoices = invoiceRepository.findAll();
        BigDecimal totalService = invoices.stream()
                .map(Invoice::getServiceAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = invoices.stream()
                .map(Invoice::getTaxAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = invoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidCount = invoices.stream()
                .filter(i -> i.getPaymentStatus() == PaymentStatus.PAID)
                .count();

        RevenueReportResponse response = new RevenueReportResponse();
        response.setTotalServiceAmount(totalService);
        response.setTotalTaxAmount(totalTax);
        response.setTotalRevenue(totalRevenue);
        response.setInvoiceCount(invoices.size());
        response.setPaidCount(paidCount);
        return response;
    }

    public List<MonthlyRevenueEntry> monthlyRevenue() {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getPaymentStatus() == PaymentStatus.PAID)
                .toList();

        Map<String, List<Invoice>> grouped = invoices.stream()
                .collect(Collectors.groupingBy(inv -> {
                    ZonedDateTime zdt = inv.getCreatedAt()
                            .atZone(ZoneId.systemDefault());
                    int year = zdt.getYear();
                    int month = zdt.getMonthValue();
                    return year + "-" + month;
                }));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<Invoice> group = entry.getValue();
                    if (group.isEmpty()) {
                        return null;
                    }
                    ZonedDateTime zdt = group.get(0).getCreatedAt().atZone(ZoneId.systemDefault());
                    int year = zdt.getYear();
                    int month = zdt.getMonthValue();

                    MonthlyRevenueEntry mr = new MonthlyRevenueEntry();
                    mr.setYear(year);
                    mr.setMonth(month);
                    mr.setTotalRevenue(group.stream()
                            .map(Invoice::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    mr.setPaidInvoiceCount(group.size());
                    return mr;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    if (a.getYear() != b.getYear()) {
                        return Integer.compare(a.getYear(), b.getYear());
                    }
                    return Integer.compare(a.getMonth(), b.getMonth());
                })
                .toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setRequestId(invoice.getRequestId());
        response.setCustomerId(invoice.getCustomerId());
        
        // Get service name from request
        String serviceName = null;
        try {
            ServiceRequest serviceRequest = requestRepository.findById(invoice.getRequestId()).orElse(null);
            if (serviceRequest != null && serviceRequest.getServiceId() != null) {
                ServiceItem serviceItem = serviceItemRepository.findById(serviceRequest.getServiceId()).orElse(null);
                if (serviceItem != null) {
                    serviceName = serviceItem.getName();
                }
            }
        } catch (Exception e) {
            // Ignore errors, serviceName will remain null
        }
        response.setServiceName(serviceName);
        
        response.setServiceAmount(invoice.getServiceAmount());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setPaymentStatus(invoice.getPaymentStatus());
        response.setPaymentMethod(invoice.getPaymentMethod());
        response.setPaidAt(invoice.getPaidAt());
        response.setCreatedAt(invoice.getCreatedAt());
        return response;
    }
}
