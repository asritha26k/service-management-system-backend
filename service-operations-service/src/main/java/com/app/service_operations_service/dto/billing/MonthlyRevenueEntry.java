package com.app.service_operations_service.dto.billing;

import java.math.BigDecimal;

public class MonthlyRevenueEntry {

    private int year;
    private int month; // 1-12
    private BigDecimal totalRevenue;
    private long paidInvoiceCount;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getPaidInvoiceCount() {
        return paidInvoiceCount;
    }

    public void setPaidInvoiceCount(long paidInvoiceCount) {
        this.paidInvoiceCount = paidInvoiceCount;
    }
}
