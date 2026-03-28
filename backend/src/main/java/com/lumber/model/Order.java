package com.lumber.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    public enum Status {
        PENDING, CONFIRMED, DELIVERED, CANCELLED
    }

    private Long id;

    @NotBlank(message = "受注番号は必須です")
    private String orderNo;

    @NotNull(message = "受注日は必須です")
    private LocalDate orderDate;

    private LocalDate deliveryDate;

    @NotNull(message = "顧客IDは必須です")
    private Long customerId;

    private String customerName;

    @NotNull(message = "ステータスは必須です")
    private Status status;

    private List<OrderItem> items;

    private int totalAmount;

    /** 受注明細から合計金額を再計算する */
    public void recalculateTotal() {
        if (items == null || items.isEmpty()) {
            this.totalAmount = 0;
            return;
        }
        this.totalAmount = items.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        items.forEach(item -> item.setSubtotal(item.getUnitPrice() * item.getQuantity()));
    }
}
