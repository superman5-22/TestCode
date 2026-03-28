package com.lumber.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    private Long productId;
    private String productName;
    private int quantity;
    private String unit;
    private int threshold; // 補充基準数

    /** 在庫が補充基準を下回っているか */
    public boolean isLow() {
        return this.quantity <= this.threshold;
    }
}
