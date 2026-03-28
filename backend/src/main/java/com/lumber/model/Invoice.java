package com.lumber.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    public enum Status {
        UNPAID, PAID, OVERDUE
    }

    private Long id;
    private String invoiceNo;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Long customerId;
    private String customerName;
    private Long orderId;
    private String orderNo;
    private int amount;       // 税抜
    private int tax;          // 消費税
    private int totalWithTax; // 税込
    private Status status;

    /** 税抜金額から消費税・税込合計を計算 (税率10%) */
    public void calculateTax() {
        this.tax = (int) Math.floor(this.amount * 0.1);
        this.totalWithTax = this.amount + this.tax;
    }
}
