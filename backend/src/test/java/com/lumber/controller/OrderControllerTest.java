package com.lumber.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumber.model.Order;
import com.lumber.model.OrderItem;
import com.lumber.service.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController 統合テスト（Web層のみ）
 *
 * @WebMvcTest を使用してサーブレットコンテキストをロードし、
 * OrderService は @MockBean で差し替える。
 */
@WebMvcTest(OrderController.class)
@DisplayName("OrderController テスト")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private Order sampleOrder(Long id, Order.Status status) {
        return Order.builder()
                .id(id)
                .orderNo("ORD-2024-00" + id)
                .orderDate(LocalDate.of(2024, 4, 1))
                .deliveryDate(LocalDate.of(2024, 4, 15))
                .customerId(1L)
                .customerName("山田建設")
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
    // GET /api/orders
    // ============================================================
    @Nested
    @DisplayName("GET /api/orders")
    class GetAll {

        @Test
        @DisplayName("正常系: 200 と受注リストを返す")
        void returns200WithOrders() throws Exception {
            when(orderService.findAll()).thenReturn(List.of(
                    sampleOrder(1L, Order.Status.PENDING),
                    sampleOrder(2L, Order.Status.CONFIRMED)
            ));

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].orderNo", is("ORD-2024-001")));
        }

        @Test
        @DisplayName("正常系: statusクエリパラメータでフィルタ")
        void filtersByStatus() throws Exception {
            when(orderService.findByStatus(Order.Status.PENDING))
                    .thenReturn(List.of(sampleOrder(1L, Order.Status.PENDING)));

            mockMvc.perform(get("/api/orders").param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));

            verify(orderService).findByStatus(Order.Status.PENDING);
            verify(orderService, never()).findAll();
        }

        @Test
        @DisplayName("正常系: 受注が0件のとき空配列を返す")
        void returnsEmptyArray_whenNoOrders() throws Exception {
            when(orderService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ============================================================
    // GET /api/orders/{id}
    // ============================================================
    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetById {

        @Test
        @DisplayName("正常系: 200 と受注を返す")
        void returns200() throws Exception {
            when(orderService.findById(1L)).thenReturn(sampleOrder(1L, Order.Status.PENDING));

            mockMvc.perform(get("/api/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.customerName", is("山田建設")));
        }

        @Test
        @DisplayName("異常系: 存在しないIDで 404 を返す")
        void returns404_whenNotFound() throws Exception {
            when(orderService.findById(99L))
                    .thenThrow(new NoSuchElementException("受注が見つかりません: id=99"));

            mockMvc.perform(get("/api/orders/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error", containsString("99")));
        }
    }

    // ============================================================
    // POST /api/orders
    // ============================================================
    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrder {

        @Test
        @DisplayName("正常系: 201 と作成された受注を返す")
        void returns201_withCreatedOrder() throws Exception {
            Order input = sampleOrder(null, null);
            Order created = sampleOrder(10L, Order.Status.PENDING);
            when(orderService.createOrder(any(Order.class))).thenReturn(created);

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        @DisplayName("異常系: 明細なし（サービス側で検証）で 400 を返す")
        void returns400_whenItemsEmpty() throws Exception {
            Order input = sampleOrder(null, null);
            when(orderService.createOrder(any()))
                    .thenThrow(new IllegalArgumentException("受注明細は1件以上必要です"));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("1件以上")));
        }
    }

    // ============================================================
    // PATCH /api/orders/{id}/status
    // ============================================================
    @Nested
    @DisplayName("PATCH /api/orders/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("正常系: 200 と更新後の受注を返す")
        void returns200_withUpdatedOrder() throws Exception {
            Order updated = sampleOrder(1L, Order.Status.CONFIRMED);
            when(orderService.updateStatus(eq(1L), eq(Order.Status.CONFIRMED))).thenReturn(updated);

            mockMvc.perform(patch("/api/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
        }

        @Test
        @DisplayName("異常系: キャンセル済み受注は 409 Conflict を返す")
        void returns409_whenCancelled() throws Exception {
            when(orderService.updateStatus(eq(1L), any()))
                    .thenThrow(new IllegalStateException("キャンセル済み受注のステータスは変更できません"));

            mockMvc.perform(patch("/api/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("キャンセル済み")));
        }
    }

    // ============================================================
    // DELETE /api/orders/{id}
    // ============================================================
    @Nested
    @DisplayName("DELETE /api/orders/{id}")
    class DeleteOrder {

        @Test
        @DisplayName("正常系: 204 No Content を返す")
        void returns204() throws Exception {
            doNothing().when(orderService).deleteOrder(1L);

            mockMvc.perform(delete("/api/orders/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("異常系: CONFIRMED受注削除で 409 Conflict を返す")
        void returns409_whenNotPending() throws Exception {
            doThrow(new IllegalStateException("確認待ち状態の受注のみ削除できます"))
                    .when(orderService).deleteOrder(1L);

            mockMvc.perform(delete("/api/orders/1"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("異常系: 存在しないIDで 404 を返す")
        void returns404() throws Exception {
            doThrow(new NoSuchElementException("受注が見つかりません: id=99"))
                    .when(orderService).deleteOrder(99L);

            mockMvc.perform(delete("/api/orders/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
