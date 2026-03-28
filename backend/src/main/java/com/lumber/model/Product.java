package com.lumber.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;

    @NotBlank(message = "商品コードは必須です")
    private String code;

    @NotBlank(message = "商品名は必須です")
    private String name;

    @NotBlank(message = "単位は必須です")
    private String unit;

    @Positive(message = "単価は正の値である必要があります")
    private int price;
}
