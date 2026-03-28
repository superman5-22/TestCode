package com.lumber.model;

import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Order モデルのビジネスロジックテスト
 * recalculateTotal() の正確性を検証する
 */
@DisplayName("Order モデルテスト")
class OrderModelTest {

    private OrderItem item(int quantity, int unitPrice) {
        return OrderItem.builder()
                .productId(1L).productName("テスト商品")
                .quantity(quantity).unitPrice(unitPrice)
                .build();
    }

    @Nested
    @DisplayName("recalculateTotal()")
    class RecalculateTotal {

        @Test
        @DisplayName("正常系: 複数明細の合計金額を正しく計算する")
        void calculatesCorrectly() {
            Order order = Order.builder()
                    .items(List.of(
                            item(10, 398),  // 3980
                            item(5, 1000),  // 5000
                            item(2, 200)    // 400
                    ))
                    .build();

            order.recalculateTotal();

            assertThat(order.getTotalAmount()).isEqualTo(9380);
        }

        @Test
        @DisplayName("正常系: 明細の subtotal も同時に更新される")
        void updatesSubtotal() {
            OrderItem orderItem = item(5, 400);
            Order order = Order.builder().items(List.of(orderItem)).build();

            order.recalculateTotal();

            assertThat(orderItem.getSubtotal()).isEqualTo(2000);
        }

        @Test
        @DisplayName("境界値: 明細が空のとき合計=0")
        void returnsZero_whenItemsEmpty() {
            Order order = Order.builder().items(Collections.emptyList()).build();

            order.recalculateTotal();

            assertThat(order.getTotalAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("境界値: 明細がnullのとき合計=0")
        void returnsZero_whenItemsNull() {
            Order order = Order.builder().items(null).build();

            order.recalculateTotal();

            assertThat(order.getTotalAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("境界値: 数量=1, 単価=1 のとき合計=1")
        void minimum_amount() {
            Order order = Order.builder().items(List.of(item(1, 1))).build();

            order.recalculateTotal();

            assertThat(order.getTotalAmount()).isEqualTo(1);
        }
    }
}
