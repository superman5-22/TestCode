package com.lumber.service;

import com.lumber.model.Invoice;
import com.lumber.model.Order;
import com.lumber.model.OrderItem;
import com.lumber.repository.InvoiceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * InvoiceService 単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService テスト")
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private InvoiceService invoiceService;

    private Order deliveredOrder() {
        return Order.builder()
                .id(1L)
                .orderNo("ORD-TEST-001")
                .customerId(1L)
                .customerName("テスト顧客")
                .status(Order.Status.DELIVERED)
                .totalAmount(10000)
                .items(List.of(
                        OrderItem.builder().productId(1L).productName("商品A").quantity(10).unitPrice(1000).subtotal(10000).build()
                ))
                .build();
    }

    private Invoice unpaidInvoice(Long id) {
        return Invoice.builder()
                .id(id)
                .invoiceNo("INV-TEST-" + id)
                .issueDate(LocalDate.of(2024, 4, 1))
                .dueDate(LocalDate.of(2024, 5, 1))
                .customerId(1L)
                .customerName("テスト顧客")
                .orderId(1L)
                .orderNo("ORD-TEST-001")
                .amount(10000)
                .tax(1000)
                .totalWithTax(11000)
                .status(Invoice.Status.UNPAID)
                .build();
    }

    // ============================================================
    // findAll / findById
    // ============================================================
    @Nested
    @DisplayName("findAll() / findById()")
    class Retrieval {

        @Test
        @DisplayName("正常系: 全請求書を返す")
        void returnsAllInvoices() {
            when(invoiceRepository.findAll()).thenReturn(List.of(unpaidInvoice(1L), unpaidInvoice(2L)));

            assertThat(invoiceService.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("正常系: IDで請求書を取得できる")
        void findsById() {
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice(1L)));

            Invoice result = invoiceService.findById(1L);

            assertThat(result.getInvoiceNo()).isEqualTo("INV-TEST-1");
        }

        @Test
        @DisplayName("異常系: 存在しないIDで NoSuchElementException")
        void throwsException_whenNotFound() {
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.findById(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("99");
        }
    }

    // ============================================================
    // findByStatus
    // ============================================================
    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("正常系: UNPAIDで絞り込み")
        void filtersUnpaid() {
            when(invoiceRepository.findByStatus(Invoice.Status.UNPAID)).thenReturn(List.of(unpaidInvoice(1L)));

            assertThat(invoiceService.findByStatus(Invoice.Status.UNPAID)).hasSize(1);
        }

        @Test
        @DisplayName("異常系: statusがnullで IllegalArgumentException")
        void throwsException_whenNull() {
            assertThatThrownBy(() -> invoiceService.findByStatus(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // issueInvoiceFromOrder
    // ============================================================
    @Nested
    @DisplayName("issueInvoiceFromOrder()")
    class IssueInvoice {

        @Test
        @DisplayName("正常系: 納品済み受注から請求書を発行できる")
        void issuesInvoice_fromDeliveredOrder() {
            Order order = deliveredOrder();
            when(orderService.findById(1L)).thenReturn(order);
            when(invoiceRepository.findAll()).thenReturn(Collections.emptyList());
            when(invoiceRepository.save(any())).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(10L);
                return i;
            });

            LocalDate issueDate = LocalDate.of(2024, 5, 1);
            LocalDate dueDate = LocalDate.of(2024, 5, 31);
            Invoice result = invoiceService.issueInvoiceFromOrder(1L, issueDate, dueDate);

            assertThat(result.getAmount()).isEqualTo(10000);
            assertThat(result.getTax()).isEqualTo(1000);       // 10%
            assertThat(result.getTotalWithTax()).isEqualTo(11000);
            assertThat(result.getStatus()).isEqualTo(Invoice.Status.UNPAID);
        }

        @Test
        @DisplayName("正常系: 請求書番号が自動採番される")
        void generatesInvoiceNumber() {
            Order order = deliveredOrder();
            when(orderService.findById(1L)).thenReturn(order);
            when(invoiceRepository.findAll()).thenReturn(List.of(unpaidInvoice(1L))); // 既存1件
            when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Invoice result = invoiceService.issueInvoiceFromOrder(1L,
                    LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31));

            assertThat(result.getInvoiceNo()).isNotBlank();
            assertThat(result.getInvoiceNo()).startsWith("INV-2024-");
        }

        @Test
        @DisplayName("異常系: 受注ステータスが DELIVERED でない場合 IllegalStateException")
        void throwsException_whenOrderNotDelivered() {
            Order pendingOrder = deliveredOrder();
            pendingOrder.setStatus(Order.Status.CONFIRMED);
            when(orderService.findById(1L)).thenReturn(pendingOrder);

            assertThatThrownBy(() -> invoiceService.issueInvoiceFromOrder(1L,
                    LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("納品済み");
        }

        @Test
        @DisplayName("異常系: 支払期限が発行日より前の場合 IllegalArgumentException")
        void throwsException_whenDueDateBeforeIssueDate() {
            Order order = deliveredOrder();
            when(orderService.findById(1L)).thenReturn(order);

            assertThatThrownBy(() -> invoiceService.issueInvoiceFromOrder(1L,
                    LocalDate.of(2024, 5, 31), LocalDate.of(2024, 5, 1))) // 逆転
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("支払期限");
        }

        @Test
        @DisplayName("境界値: 発行日と支払期限が同じ日でも発行できる")
        void issuesInvoice_whenIssueDateEqualsDueDate() {
            Order order = deliveredOrder();
            when(orderService.findById(1L)).thenReturn(order);
            when(invoiceRepository.findAll()).thenReturn(Collections.emptyList());
            when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDate sameDate = LocalDate.of(2024, 5, 1);
            assertThatCode(() -> invoiceService.issueInvoiceFromOrder(1L, sameDate, sameDate))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================
    // markAsPaid
    // ============================================================
    @Nested
    @DisplayName("markAsPaid()")
    class MarkAsPaid {

        @Test
        @DisplayName("正常系: UNPAID → PAID に更新できる")
        void marksPaid() {
            Invoice invoice = unpaidInvoice(1L);
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Invoice result = invoiceService.markAsPaid(1L);

            assertThat(result.getStatus()).isEqualTo(Invoice.Status.PAID);
        }

        @Test
        @DisplayName("冪等性: 既に PAID の請求書を再度 markAsPaid しても正常終了する")
        void idempotent_whenAlreadyPaid() {
            Invoice invoice = unpaidInvoice(1L);
            invoice.setStatus(Invoice.Status.PAID);
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            Invoice result = invoiceService.markAsPaid(1L);

            assertThat(result.getStatus()).isEqualTo(Invoice.Status.PAID);
            // save が呼ばれていないことを確認（冪等処理）
            verify(invoiceRepository, never()).save(any());
        }
    }

    // ============================================================
    // updateOverdueInvoices
    // ============================================================
    @Nested
    @DisplayName("updateOverdueInvoices()")
    class UpdateOverdue {

        @Test
        @DisplayName("正常系: 期限切れの UNPAID 請求書が OVERDUE になる")
        void updatesOverdue() {
            Invoice overdue = unpaidInvoice(1L);
            overdue.setDueDate(LocalDate.of(2024, 1, 1)); // 過去日

            Invoice current = unpaidInvoice(2L);
            current.setDueDate(LocalDate.of(2099, 12, 31)); // 未来日

            when(invoiceRepository.findByStatus(Invoice.Status.UNPAID))
                    .thenReturn(List.of(overdue, current));
            when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int count = invoiceService.updateOverdueInvoices(LocalDate.of(2024, 6, 1));

            assertThat(count).isEqualTo(1);
            assertThat(overdue.getStatus()).isEqualTo(Invoice.Status.OVERDUE);
            assertThat(current.getStatus()).isEqualTo(Invoice.Status.UNPAID);
        }

        @Test
        @DisplayName("境界値: 期限当日はまだ OVERDUE にならない")
        void doesNotOverdue_onDueDate() {
            Invoice invoice = unpaidInvoice(1L);
            LocalDate today = LocalDate.of(2024, 5, 1);
            invoice.setDueDate(today);

            when(invoiceRepository.findByStatus(Invoice.Status.UNPAID))
                    .thenReturn(List.of(invoice));

            int count = invoiceService.updateOverdueInvoices(today);

            assertThat(count).isEqualTo(0);
            assertThat(invoice.getStatus()).isEqualTo(Invoice.Status.UNPAID);
        }

        @Test
        @DisplayName("正常系: 期限切れ請求書が0件のとき 0 を返す")
        void returnsZero_whenNoOverdue() {
            when(invoiceRepository.findByStatus(Invoice.Status.UNPAID))
                    .thenReturn(Collections.emptyList());

            int count = invoiceService.updateOverdueInvoices(LocalDate.now());

            assertThat(count).isEqualTo(0);
        }
    }
}
