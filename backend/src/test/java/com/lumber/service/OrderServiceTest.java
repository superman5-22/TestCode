package com.lumber.service;

import com.lumber.model.Order;
import com.lumber.model.OrderItem;
import com.lumber.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService 単体テスト
 *
 * テスト方針:
 * - OrderRepository を @Mock で差し替え（DB不要）
 * - @InjectMocks で OrderService にモックを注入
 * - 各メソッドの正常系・異常系・境界値を網羅
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService テスト")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    // -------------------------------------------------------
    // テスト用ヘルパー
    // -------------------------------------------------------
    private Order buildOrder(Long id, Order.Status status) {
        return Order.builder()
                .id(id)
                .orderNo("ORD-TEST-" + id)
                .orderDate(LocalDate.of(2024, 4, 1))
                .customerId(1L)
                .customerName("テスト顧客")
                .status(status)
                .items(List.of(
                        OrderItem.builder()
                                .productId(1L).productName("SPF 2×4材")
                                .quantity(10).unitPrice(398).subtotal(3980)
                                .build()
                ))
                .totalAmount(3980)
                .build();
    }

    // ============================================================
    // findAll
    // ============================================================
    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("正常系: 受注リストを返す")
        void returnsAllOrders() {
            List<Order> expected = List.of(
                    buildOrder(1L, Order.Status.PENDING),
                    buildOrder(2L, Order.Status.CONFIRMED)
            );
            when(orderRepository.findAll()).thenReturn(expected);

            List<Order> result = orderService.findAll();

            assertThat(result).hasSize(2);
            verify(orderRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("正常系: データが0件のとき空リストを返す")
        void returnsEmptyList_whenNoOrders() {
            when(orderRepository.findAll()).thenReturn(Collections.emptyList());

            List<Order> result = orderService.findAll();

            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // findById
    // ============================================================
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("正常系: IDが存在する場合、受注を返す")
        void returnsOrder_whenExists() {
            Order order = buildOrder(1L, Order.Status.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Order result = orderService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOrderNo()).isEqualTo("ORD-TEST-1");
        }

        @Test
        @DisplayName("異常系: IDが存在しない場合、NoSuchElementException をスロー")
        void throwsException_whenNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("境界値: ID=1 （最小値）で正常取得できる")
        void returnsOrder_whenIdIsOne() {
            Order order = buildOrder(1L, Order.Status.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatCode(() -> orderService.findById(1L)).doesNotThrowAnyException();
        }
    }

    // ============================================================
    // findByStatus
    // ============================================================
    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("正常系: PENDING の受注を返す")
        void returnsPendingOrders() {
            List<Order> pending = List.of(buildOrder(1L, Order.Status.PENDING));
            when(orderRepository.findByStatus(Order.Status.PENDING)).thenReturn(pending);

            List<Order> result = orderService.findByStatus(Order.Status.PENDING);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Order.Status.PENDING);
        }

        @ParameterizedTest
        @EnumSource(Order.Status.class)
        @DisplayName("正常系: すべてのステータス値でリポジトリが呼ばれる")
        void callsRepository_forAllStatuses(Order.Status status) {
            when(orderRepository.findByStatus(status)).thenReturn(Collections.emptyList());

            orderService.findByStatus(status);

            verify(orderRepository).findByStatus(status);
        }

        @Test
        @DisplayName("異常系: statusがnullのとき IllegalArgumentException をスロー")
        void throwsException_whenStatusIsNull() {
            assertThatThrownBy(() -> orderService.findByStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }
    }

    // ============================================================
    // findByCustomerId
    // ============================================================
    @Nested
    @DisplayName("findByCustomerId()")
    class FindByCustomerId {

        @Test
        @DisplayName("正常系: 顧客IDに対応する受注を返す")
        void returnsOrders_forCustomer() {
            List<Order> orders = List.of(buildOrder(1L, Order.Status.CONFIRMED));
            when(orderRepository.findByCustomerId(1L)).thenReturn(orders);

            assertThat(orderService.findByCustomerId(1L)).hasSize(1);
        }

        @Test
        @DisplayName("異常系: 顧客IDが0のとき IllegalArgumentException をスロー")
        void throwsException_whenCustomerIdIsZero() {
            assertThatThrownBy(() -> orderService.findByCustomerId(0L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("異常系: 顧客IDが負のとき IllegalArgumentException をスロー")
        void throwsException_whenCustomerIdIsNegative() {
            assertThatThrownBy(() -> orderService.findByCustomerId(-1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("異常系: 顧客IDがnullのとき IllegalArgumentException をスロー")
        void throwsException_whenCustomerIdIsNull() {
            assertThatThrownBy(() -> orderService.findByCustomerId(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // createOrder
    // ============================================================
    @Nested
    @DisplayName("createOrder()")
    class CreateOrder {

        @Test
        @DisplayName("正常系: 受注を保存して返す（ステータスがPENDINGになる）")
        void savesOrder_withPendingStatus() {
            Order input = buildOrder(null, null);
            Order saved = buildOrder(10L, Order.Status.PENDING);
            when(orderRepository.save(any(Order.class))).thenReturn(saved);

            Order result = orderService.createOrder(input);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getStatus()).isEqualTo(Order.Status.PENDING);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("正常系: 合計金額が明細から自動計算される")
        void recalculatesTotalFromItems() {
            Order input = Order.builder()
                    .orderNo("ORD-TEST-NEW")
                    .orderDate(LocalDate.now())
                    .customerId(1L)
                    .customerName("テスト顧客")
                    .items(List.of(
                            OrderItem.builder().productId(1L).productName("商品A").quantity(5).unitPrice(1000).build(),
                            OrderItem.builder().productId(2L).productName("商品B").quantity(3).unitPrice(500).build()
                    ))
                    .build();

            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.createOrder(input);

            // 5×1000 + 3×500 = 6500
            assertThat(result.getTotalAmount()).isEqualTo(6500);
        }

        @Test
        @DisplayName("異常系: orderがnullのとき IllegalArgumentException をスロー")
        void throwsException_whenOrderIsNull() {
            assertThatThrownBy(() -> orderService.createOrder(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("異常系: 明細が空のとき IllegalArgumentException をスロー")
        void throwsException_whenItemsEmpty() {
            Order order = buildOrder(null, null);
            order.setItems(Collections.emptyList());

            assertThatThrownBy(() -> orderService.createOrder(order))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1件以上");
        }

        @Test
        @DisplayName("異常系: 明細がnullのとき IllegalArgumentException をスロー")
        void throwsException_whenItemsNull() {
            Order order = buildOrder(null, null);
            order.setItems(null);

            assertThatThrownBy(() -> orderService.createOrder(order))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // updateStatus
    // ============================================================
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("正常系: PENDING → CONFIRMED へ更新できる")
        void updatesStatus_fromPendingToConfirmed() {
            Order order = buildOrder(1L, Order.Status.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.updateStatus(1L, Order.Status.CONFIRMED);

            assertThat(result.getStatus()).isEqualTo(Order.Status.CONFIRMED);
        }

        @Test
        @DisplayName("正常系: CONFIRMED → DELIVERED へ更新できる")
        void updatesStatus_fromConfirmedToDelivered() {
            Order order = buildOrder(1L, Order.Status.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Order result = orderService.updateStatus(1L, Order.Status.DELIVERED);

            assertThat(result.getStatus()).isEqualTo(Order.Status.DELIVERED);
        }

        @Test
        @DisplayName("異常系: CANCELLED 受注のステータスは変更できない")
        void throwsException_whenOrderIsCancelled() {
            Order order = buildOrder(1L, Order.Status.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateStatus(1L, Order.Status.CONFIRMED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("キャンセル済み");
        }

        @Test
        @DisplayName("異常系: DELIVERED 受注を CANCELLED にはできない")
        void throwsException_whenDeliveredToCancelled() {
            Order order = buildOrder(1L, Order.Status.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateStatus(1L, Order.Status.CANCELLED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("納品済み");
        }

        @Test
        @DisplayName("異常系: 存在しないIDの場合 NoSuchElementException をスロー")
        void throwsException_whenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateStatus(999L, Order.Status.CONFIRMED))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ============================================================
    // deleteOrder
    // ============================================================
    @Nested
    @DisplayName("deleteOrder()")
    class DeleteOrder {

        @Test
        @DisplayName("正常系: PENDING受注は削除できる")
        void deletesOrder_whenStatusIsPending() {
            Order order = buildOrder(1L, Order.Status.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatCode(() -> orderService.deleteOrder(1L)).doesNotThrowAnyException();
            verify(orderRepository).deleteById(1L);
        }

        @Test
        @DisplayName("異常系: CONFIRMED受注は削除できない")
        void throwsException_whenStatusIsConfirmed() {
            Order order = buildOrder(1L, Order.Status.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.deleteOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("確認待ち");

            verify(orderRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("異常系: DELIVERED受注は削除できない")
        void throwsException_whenStatusIsDelivered() {
            Order order = buildOrder(1L, Order.Status.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.deleteOrder(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    // countOrders
    // ============================================================
    @Nested
    @DisplayName("countOrders()")
    class CountOrders {

        @Test
        @DisplayName("正常系: 受注件数を返す")
        void returnsCount() {
            when(orderRepository.count()).thenReturn(5);

            assertThat(orderService.countOrders()).isEqualTo(5);
        }

        @Test
        @DisplayName("境界値: 件数が0のとき0を返す")
        void returnsZero_whenEmpty() {
            when(orderRepository.count()).thenReturn(0);

            assertThat(orderService.countOrders()).isEqualTo(0);
        }
    }
}
