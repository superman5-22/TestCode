package com.lumber.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Invoice モデルのビジネスロジックテスト
 * calculateTax() の正確性を検証する
 */
@DisplayName("Invoice モデルテスト")
class InvoiceModelTest {

    @Nested
    @DisplayName("calculateTax()")
    class CalculateTax {

        @ParameterizedTest(name = "税抜={0}円 → 税={1}円, 税込={2}円")
        @CsvSource({
                "10000, 1000, 11000",
                "100000, 10000, 110000",
                "1, 0, 1",           // 1円（端数切り捨て）
                "9, 0, 9",           // 9円（端数切り捨て）
                "10, 1, 11",         // 10円
                "0, 0, 0",           // 0円
                "99999, 9999, 109998" // 端数切り捨て確認
        })
        @DisplayName("正常系: 各金額の消費税計算")
        void calculatesCorrectly(int amount, int expectedTax, int expectedTotal) {
            Invoice invoice = Invoice.builder().amount(amount).build();

            invoice.calculateTax();

            assertThat(invoice.getTax()).isEqualTo(expectedTax);
            assertThat(invoice.getTotalWithTax()).isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("正常系: 税率は10%（小数切り捨て）")
        void taxRateIs10Percent_withFloor() {
            Invoice invoice = Invoice.builder().amount(15).build(); // 15 * 0.1 = 1.5 → 1

            invoice.calculateTax();

            assertThat(invoice.getTax()).isEqualTo(1);
            assertThat(invoice.getTotalWithTax()).isEqualTo(16);
        }
    }
}
