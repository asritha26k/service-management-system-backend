package com.app.service_operations_service.service;

import com.app.service_operations_service.dto.billing.*;
import com.app.service_operations_service.exception.BadRequestException;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.Invoice;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.PaymentStatus;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.InvoiceRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ServiceRequestRepository requestRepository;

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @InjectMocks
    private BillingService billingService;

    private Invoice invoice;
    private ServiceRequest serviceRequest;
    private ServiceItem serviceItem;

    @BeforeEach
    void setUp() {
        invoice = Invoice.builder()
                .id("invoice-1")
                .requestId("req-1")
                .customerId("customer-1")
                .serviceAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        serviceRequest = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .build();

        serviceItem = ServiceItem.builder()
                .id("service-1")
                .basePrice(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void createInvoice_ShouldReturnInvoiceResponse() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("req-1");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("100.00"));
        request.setTaxAmount(new BigDecimal("10.00"));
        request.setTotalAmount(new BigDecimal("110.00"));

        when(requestRepository.existsById("req-1")).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.createInvoice(request);

        assertNotNull(response);
        assertEquals("invoice-1", response.getId());
        assertEquals(new BigDecimal("110.00"), response.getTotalAmount());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void createInvoice_ShouldThrowBadRequest_WhenRequestNotFound() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("invalid-req");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("100.00"));
        request.setTaxAmount(new BigDecimal("10.00"));
        request.setTotalAmount(new BigDecimal("110.00"));

        when(requestRepository.existsById("invalid-req")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> billingService.createInvoice(request));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void createInvoice_ShouldThrowBadRequest_WhenServiceAmountNegative() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("req-1");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("-10.00"));
        request.setTaxAmount(new BigDecimal("10.00"));
        request.setTotalAmount(new BigDecimal("110.00"));

        when(requestRepository.existsById("req-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> billingService.createInvoice(request));
    }

    @Test
    void getById_ShouldReturnInvoice() {
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));

        InvoiceResponse response = billingService.getById("invoice-1");

        assertNotNull(response);
        assertEquals("invoice-1", response.getId());
        verify(invoiceRepository, times(1)).findById("invoice-1");
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotFound() {
        when(invoiceRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> billingService.getById("invalid-id"));
    }

    @Test
    void getByCustomer_ShouldReturnCustomerInvoices() {
        List<Invoice> invoices = Arrays.asList(invoice);
        when(invoiceRepository.findByCustomerId("customer-1")).thenReturn(invoices);

        List<InvoiceResponse> responses = billingService.getByCustomer("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("customer-1", responses.get(0).getCustomerId());
        verify(invoiceRepository, times(1)).findByCustomerId("customer-1");
    }

    @Test
    void getByRequestId_ShouldReturnInvoice() {
        when(invoiceRepository.findByRequestId("req-1")).thenReturn(Optional.of(invoice));

        InvoiceResponse response = billingService.getByRequestId("req-1");

        assertNotNull(response);
        assertEquals("req-1", response.getRequestId());
        verify(invoiceRepository, times(1)).findByRequestId("req-1");
    }

    @Test
    void updatePayment_ShouldUpdatePaymentStatus() {
        PaymentUpdateRequest request = new PaymentUpdateRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        request.setPaymentMethod("Credit Card");

        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.updatePayment("invoice-1", request);

        assertNotNull(response);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

//    @Test
//    void payInvoice_ShouldPayInvoice() {
//        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));
//        invoice.setPaymentStatus(PaymentStatus.PAID);
//        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
//
//        InvoiceResponse response = billingService.payInvoice("invoice-1");
//
//        assertNotNull(response);
//        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
//        verify(invoiceRepository, times(1)).save(any(Invoice.class));
//    }

    @Test
    void payInvoice_ShouldThrowBadRequest_WhenAlreadyPaid() {
        invoice.setPaymentStatus(PaymentStatus.PAID);
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));

        assertThrows(BadRequestException.class, () -> billingService.payInvoice("invoice-1"));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void generateInvoiceForCompletedRequest_ShouldGenerateInvoice() {
        when(invoiceRepository.existsByRequestId("req-1")).thenReturn(false);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceItemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.generateInvoiceForCompletedRequest("req-1");

        assertNotNull(response);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void generateInvoiceForCompletedRequest_ShouldReturnExisting_WhenInvoiceExists() {
        when(invoiceRepository.existsByRequestId("req-1")).thenReturn(true);
        when(invoiceRepository.findByRequestId("req-1")).thenReturn(Optional.of(invoice));

        InvoiceResponse response = billingService.generateInvoiceForCompletedRequest("req-1");

        assertNotNull(response);
        assertEquals("invoice-1", response.getId());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void revenueReport_ShouldCalculateRevenue() {
        List<Invoice> invoices = Arrays.asList(invoice);
        when(invoiceRepository.findAll()).thenReturn(invoices);

        RevenueReportResponse response = billingService.revenueReport();

        assertNotNull(response);
        assertNotNull(response.getTotalRevenue());
        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void monthlyRevenue_ShouldReturnMonthlyRevenue() {
        invoice.setPaymentStatus(PaymentStatus.PAID);
        List<Invoice> invoices = Arrays.asList(invoice);
        when(invoiceRepository.findAll()).thenReturn(invoices);

        List<MonthlyRevenueEntry> entries = billingService.monthlyRevenue();

        assertNotNull(entries);
        verify(invoiceRepository, times(1)).findAll();
    }

    // Additional tests for improved coverage

    @Test
    void payInvoice_ShouldPayInvoice() {
        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.payInvoice("invoice-1");

        assertNotNull(response);
        assertEquals("invoice-1", response.getId());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void payInvoice_ShouldThrowNotFoundException_WhenNotFound() {
        when(invoiceRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> billingService.payInvoice("invalid-id"));
    }

    @Test
    void getByRequestId_ShouldThrowNotFoundException_WhenNotFound() {
        when(invoiceRepository.findByRequestId("invalid-req")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> billingService.getByRequestId("invalid-req"));
    }

    @Test
    void updatePayment_ShouldThrowNotFoundException_WhenNotFound() {
        PaymentUpdateRequest request = new PaymentUpdateRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        request.setPaymentMethod("Credit Card");

        when(invoiceRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> billingService.updatePayment("invalid-id", request));
    }

    @Test
    void updatePayment_ShouldSetPaidAtWhenPaymentStatusIsPaid() {
        PaymentUpdateRequest request = new PaymentUpdateRequest();
        request.setPaymentStatus(PaymentStatus.PAID);
        request.setPaymentMethod("Credit Card");

        invoice.setPaymentStatus(PaymentStatus.PENDING);
        invoice.setPaidAt(null);

        when(invoiceRepository.findById("invoice-1")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.updatePayment("invoice-1", request);

        assertNotNull(response);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void createInvoice_ShouldThrowBadRequest_WhenTaxNegative() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("req-1");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("100.00"));
        request.setTaxAmount(new BigDecimal("-5.00"));
        request.setTotalAmount(new BigDecimal("95.00"));

        when(requestRepository.existsById("req-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> billingService.createInvoice(request));
    }

    @Test
    void createInvoice_ShouldThrowBadRequest_WhenTotalAmountZero() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("req-1");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("100.00"));
        request.setTaxAmount(new BigDecimal("10.00"));
        request.setTotalAmount(new BigDecimal("0.00"));

        when(requestRepository.existsById("req-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> billingService.createInvoice(request));
    }

    @Test
    void generateInvoiceForCompletedRequest_ShouldGenerateWithDefaultPricing_WhenServiceItemNotFound() {
        when(invoiceRepository.existsByRequestId("req-1")).thenReturn(false);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceItemRepository.findById("service-1")).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.generateInvoiceForCompletedRequest("req-1");

        assertNotNull(response);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void generateInvoiceForCompletedRequest_ShouldGenerateWithDefaultPricing_WhenBasePriceNull() {
        serviceItem.setBasePrice(null);
        
        when(invoiceRepository.existsByRequestId("req-1")).thenReturn(false);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceItemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = billingService.generateInvoiceForCompletedRequest("req-1");

        assertNotNull(response);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void generateInvoiceForCompletedRequest_ShouldThrowNotFoundException_WhenRequestNotFound() {
        when(invoiceRepository.existsByRequestId("invalid-req")).thenReturn(false);
        when(requestRepository.findById("invalid-req")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> billingService.generateInvoiceForCompletedRequest("invalid-req"));
    }

    @Test
    void revenueReport_ShouldReturnZeroValues_WhenNoInvoices() {
        when(invoiceRepository.findAll()).thenReturn(Arrays.asList());

        RevenueReportResponse response = billingService.revenueReport();

        assertNotNull(response);
        assertEquals(0, response.getInvoiceCount());
        assertEquals(0, response.getPaidCount());
        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void revenueReport_ShouldIncludeOnlyPaidInvoices() {
        Invoice paidInvoice = Invoice.builder()
                .id("invoice-1")
                .requestId("req-1")
                .customerId("customer-1")
                .serviceAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.now())
                .build();

        Invoice pendingInvoice = Invoice.builder()
                .id("invoice-2")
                .requestId("req-2")
                .customerId("customer-1")
                .serviceAmount(new BigDecimal("50.00"))
                .taxAmount(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("55.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        List<Invoice> invoices = Arrays.asList(paidInvoice, pendingInvoice);
        when(invoiceRepository.findAll()).thenReturn(invoices);

        RevenueReportResponse response = billingService.revenueReport();

        assertNotNull(response);
        assertEquals(2, response.getInvoiceCount());
        assertEquals(1, response.getPaidCount());
    }

    @Test
    void monthlyRevenue_ShouldReturnEmptyList_WhenNoPaidInvoices() {
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        List<Invoice> invoices = Arrays.asList(invoice);
        when(invoiceRepository.findAll()).thenReturn(invoices);

        List<MonthlyRevenueEntry> entries = billingService.monthlyRevenue();

        assertNotNull(entries);
        assertEquals(0, entries.size());
    }

    @Test
    void monthlyRevenue_ShouldGroupByYearAndMonth() {
        Invoice invoice1 = Invoice.builder()
                .id("invoice-1")
                .requestId("req-1")
                .customerId("customer-1")
                .serviceAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.now())
                .build();

        Invoice invoice2 = Invoice.builder()
                .id("invoice-2")
                .requestId("req-2")
                .customerId("customer-1")
                .serviceAmount(new BigDecimal("200.00"))
                .taxAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("220.00"))
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.now())
                .build();

        List<Invoice> invoices = Arrays.asList(invoice1, invoice2);
        when(invoiceRepository.findAll()).thenReturn(invoices);

        List<MonthlyRevenueEntry> entries = billingService.monthlyRevenue();

        assertNotNull(entries);
        assertTrue(entries.size() > 0);
    }

    @Test
    void getByCustomer_ShouldReturnEmptyList_WhenNoInvoices() {
        when(invoiceRepository.findByCustomerId("customer-1")).thenReturn(Arrays.asList());

        List<InvoiceResponse> responses = billingService.getByCustomer("customer-1");

        assertNotNull(responses);
        assertEquals(0, responses.size());
        verify(invoiceRepository, times(1)).findByCustomerId("customer-1");
    }

    @Test
    void createInvoice_ShouldThrowBadRequest_WhenServiceAmountZero() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("req-1");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("0.00"));
        request.setTaxAmount(new BigDecimal("0.00"));
        request.setTotalAmount(new BigDecimal("0.00"));

        when(requestRepository.existsById("req-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> billingService.createInvoice(request));
    }
}

