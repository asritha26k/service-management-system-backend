package com.app.service_operations_service.controller;

import com.app.service_operations_service.dto.IdMessageResponse;
import com.app.service_operations_service.dto.billing.*;
import com.app.service_operations_service.model.enums.PaymentStatus;
import com.app.service_operations_service.service.BillingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BillingController.class, excludeAutoConfiguration = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    EurekaClientAutoConfiguration.class
})
@TestPropertySource(properties = {
    "logging.level.root=INFO",
    "logging.level.com.app.service_operations_service=INFO",
    "spring.application.name=service-operations-service-test",
    "server.port=0"
})
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingService billingService;

    @Autowired
    private ObjectMapper objectMapper;

    private InvoiceResponse invoiceResponse;

    @BeforeEach
    void setUp() {
        invoiceResponse = new InvoiceResponse();
        invoiceResponse.setId("invoice-1");
        invoiceResponse.setRequestId("req-1");
        invoiceResponse.setCustomerId("customer-1");
        invoiceResponse.setServiceAmount(new BigDecimal("100.00"));
        invoiceResponse.setTaxAmount(new BigDecimal("10.00"));
        invoiceResponse.setTotalAmount(new BigDecimal("110.00"));
        invoiceResponse.setPaymentStatus(PaymentStatus.PENDING);
        invoiceResponse.setCreatedAt(Instant.now());
    }

    @Test
    void createInvoice_ShouldReturnCreated() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId("req-1");
        request.setCustomerId("customer-1");
        request.setServiceAmount(new BigDecimal("100.00"));
        request.setTaxAmount(new BigDecimal("10.00"));
        request.setTotalAmount(new BigDecimal("110.00"));

        when(billingService.createInvoice(any(CreateInvoiceRequest.class))).thenReturn(invoiceResponse);

        mockMvc.perform(post("/api/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("invoice-1"))
                .andExpect(jsonPath("$.message").value("Invoice created successfully"));
    }

    @Test
    void createInvoice_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setRequestId(""); // Invalid: blank requestId

        mockMvc.perform(post("/api/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInvoice_ShouldReturnOk() throws Exception {
        when(billingService.getById("invoice-1")).thenReturn(invoiceResponse);

        mockMvc.perform(get("/api/billing/invoices/invoice-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("invoice-1"))
                .andExpect(jsonPath("$.totalAmount").value(110.00));
    }

    @Test
    void getByCustomer_ShouldReturnOk() throws Exception {
        List<InvoiceResponse> invoices = Arrays.asList(invoiceResponse);
        when(billingService.getByCustomer("customer-1")).thenReturn(invoices);

        mockMvc.perform(get("/api/billing/invoices/customer/customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void getMyInvoices_ShouldReturnOk() throws Exception {
        List<InvoiceResponse> invoices = Arrays.asList(invoiceResponse);
        when(billingService.getByCustomer("customer-1")).thenReturn(invoices);

        mockMvc.perform(get("/api/billing/my-invoices")
                        .header("X-User-Id", "customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void getByRequestId_ShouldReturnOk() throws Exception {
        when(billingService.getByRequestId("req-1")).thenReturn(invoiceResponse);

        mockMvc.perform(get("/api/billing/invoices/request/req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-1"));
    }

    @Test
    void payInvoice_ShouldReturnOk() throws Exception {
        invoiceResponse.setPaymentStatus(PaymentStatus.PAID);
        when(billingService.payInvoice("invoice-1")).thenReturn(invoiceResponse);

        mockMvc.perform(post("/api/billing/invoices/invoice-1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("invoice-1"))
                .andExpect(jsonPath("$.message").value("Invoice paid successfully"));
    }

//    @Test
//    void updatePayment_ShouldReturnNoContent() throws Exception {
//        PaymentUpdateRequest request = new PaymentUpdateRequest();
//        request.setPaymentStatus(PaymentStatus.PAID);
//        request.setPaymentMethod("Credit Card");
//
//        when(billingService.updatePayment(eq("invoice-1"), any(PaymentUpdateRequest.class)))
//                .thenReturn(invoiceResponse);
//
//        mockMvc.perform(put("/api/billing/invoices/invoice-1/payment")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNoContent());
//    }

    @Test
    void revenueReport_ShouldReturnOk() throws Exception {
        RevenueReportResponse report = new RevenueReportResponse();
        report.setTotalRevenue(new BigDecimal("10000.00"));
        report.setTotalServiceAmount(new BigDecimal("9000.00"));
        report.setTotalTaxAmount(new BigDecimal("1000.00"));
        report.setInvoiceCount(100L);
        report.setPaidCount(90L);

        when(billingService.revenueReport()).thenReturn(report);

        mockMvc.perform(get("/api/billing/reports/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(10000.00))
                .andExpect(jsonPath("$.invoiceCount").value(100));
    }

    @Test
    void monthlyRevenue_ShouldReturnOk() throws Exception {
        MonthlyRevenueEntry entry = new MonthlyRevenueEntry();
        entry.setYear(2024);
        entry.setMonth(1);
        entry.setTotalRevenue(new BigDecimal("5000.00"));
        entry.setPaidInvoiceCount(50L);

        List<MonthlyRevenueEntry> entries = Arrays.asList(entry);
        when(billingService.monthlyRevenue()).thenReturn(entries);

        mockMvc.perform(get("/api/billing/reports/revenue/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2024))
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].totalRevenue").value(5000.00));
    }
}

