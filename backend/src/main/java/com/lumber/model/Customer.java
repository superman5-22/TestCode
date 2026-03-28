package com.lumber.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private Long id;

    @NotBlank(message = "顧客コードは必須です")
    private String code;

    @NotBlank(message = "顧客名は必須です")
    private String name;

    private String contact;
    private String tel;
}
