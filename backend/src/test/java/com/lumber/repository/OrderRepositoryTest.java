package com.lumber.repository;

import com.lumber.model.Order;
import com.lumber.model.OrderItem;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * OrderRepository インメモリテスト
 * DB不使用のインメモリ実装のため、Spring コンテキスト不要
 */
@DisplayName("OrderRepository テスト")
class OrderRepositoryTest {

    private OrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new OrderRepository(); // ダミーデータが3件ある状態
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("初期データ3件が取得できる")
        void returnsInitialData() {
            assertThat(repository.findAll()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("正常系: 存在するIDで Optional に値が入る")
        void findsExistingOrder() {
            Optional<Order> result = repository.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getOrderNo()).isEqualTo("ORD-2024-001");
        }

        @Test
        @DisplayName("正常系: 存在しないIDで Optional.empty() を返す")
        void returnsEmpty_whenNotFound() {
            assertThat(repository.findById(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("PENDING の受注を返す")
        void returnsPendingOrders() {
            List<Order> result = repository.findByStatus(Order.Status.PENDING);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Order.Status.PENDING);
        }

        @Test
        @DisplayName("CANCELLED（存在しない）のとき空リスト")
        void returnsEmpty_whenNoneMatch() {
            assertThat(repository.findByStatus(Order.Status.CANCELLED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("新規保存でIDが自動採番される")
        void assignsId_onNewSave() {
            Order newOrder = Order.builder()
                    .orderNo("ORD-2024-NEW")
                    .orderDate(LocalDate.now())
                    .customerId(1L)
                    .customerName("新規顧客")
                    .status(Order.Status.PENDING)
                    .items(List.of(
                            OrderItem.builder().productId(1L).productName("商品A").quantity(1).unitPrice(100).build()
                    ))
                    .build();

            Order saved = repository.save(newOrder);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isGreaterThan(0L);
        }

        @Test
        @DisplayName("既存IDで保存すると上書きされる")
        void updatesExistingOrder() {
            Order existing = repository.findById(1L).get();
            existing.setStatus(Order.Status.CONFIRMED);

            repository.save(existing);

            assertThat(repository.findById(1L).get().getStatus()).isEqualTo(Order.Status.CONFIRMED);
        }

        @Test
        @DisplayName("保存後に件数が増える")
        void countIncreasesAfterSave() {
            int before = repository.count();
            Order newOrder = Order.builder()
                    .orderNo("ORD-NEW")
                    .orderDate(LocalDate.now())
                    .customerId(2L)
                    .customerName("顧客")
                    .status(Order.Status.PENDING)
                    .items(List.of(
                            OrderItem.builder().productId(1L).productName("商品").quantity(1).unitPrice(100).build()
                    ))
                    .build();

            repository.save(newOrder);

            assertThat(repository.count()).isEqualTo(before + 1);
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test
        @DisplayName("存在するIDを削除すると true を返す")
        void returnsTrueOnSuccess() {
            assertThat(repository.deleteById(1L)).isTrue();
            assertThat(repository.findById(1L)).isEmpty();
        }

        @Test
        @DisplayName("存在しないIDを削除すると false を返す")
        void returnsFalse_whenNotFound() {
            assertThat(repository.deleteById(999L)).isFalse();
        }

        @Test
        @DisplayName("削除後に件数が減る")
        void countDecreasesAfterDelete() {
            int before = repository.count();

            repository.deleteById(1L);

            assertThat(repository.count()).isEqualTo(before - 1);
        }
    }
}
