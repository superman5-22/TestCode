package com.lumber.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumber.model.Invoice;
import com.lumber.service.InvoiceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * InvoiceController 統合テスト（Web層のみ）
 */
@WebMvcTest(InvoiceController.class)
@DisplayName("InvoiceController テスト")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private Invoice sampleInvoice(Long id, Invoice.Status status) {
        return Invoice.builder()
                .id(id)
                .invoiceNo("INV-2024-00" + id)
                .issueDate(LocalDate.of(2024, 4, 30))
                .dueDate(LocalDate.of(2024, 5, 31))
                .customerId(1L)
                .customerName("山田建設")
                .orderId(1L)
                .orderNo("ORD-2024-001")
                .amount(10000)
                .tax(1000)
                .totalWithTax(11000)
                .status(status)
                .build();
    }

    // ============================================================
    // GET /api/invoices
    // ============================================================
    @Nested
    @DisplayName("GET /api/invoices")
    class GetAll {

        @Test
        @DisplayName("正常系: 200 と請求書リストを返す")
        void returns200() throws Exception {
            when(invoiceService.findAll()).thenReturn(List.of(
                    sampleInvoice(1L, Invoice.Status.PAID),
                    sampleInvoice(2L, Invoice.Status.UNPAID)
            ));

            mockMvc.perform(get("/api/invoices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("正常系: statusフィルタが効く")
        void filtersByStatus() throws Exception {
            when(invoiceService.findByStatus(Invoice.Status.UNPAID))
                    .thenReturn(List.of(sampleInvoice(2L, Invoice.Status.UNPAID)));

            mockMvc.perform(get("/api/invoices").param("status", "UNPAID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("UNPAID")));
        }
    }

    // ============================================================
    // GET /api/invoices/{id}
    // ============================================================
    @Nested
    @DisplayName("GET /api/invoices/{id}")
    class GetById {

        @Test
        @DisplayName("正常系: 200 と請求書を返す")
        void returns200() throws Exception {
            when(invoiceService.findById(1L)).thenReturn(sampleInvoice(1L, Invoice.Status.PAID));

            mockMvc.perform(get("/api/invoices/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.invoiceNo", is("INV-2024-001")))
                    .andExpect(jsonPath("$.totalWithTax", is(11000)));
        }

        @Test
        @DisplayName("異常系: 存在しないIDで 404 を返す")
        void returns404() throws Exception {
            when(invoiceService.findById(99L))
                    .thenThrow(new NoSuchElementException("請求書が見つかりません: id=99"));

            mockMvc.perform(get("/api/invoices/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================================
    // POST /api/invoices/issue
    // ============================================================
    @Nested
    @DisplayName("POST /api/invoices/issue")
    class Issue {

        @Test
        @DisplayName("正常系: 201 と発行された請求書を返す")
        void returns201() throws Exception {
            Invoice issued = sampleInvoice(10L, Invoice.Status.UNPAID);
            when(invoiceService.issueInvoiceFromOrder(anyLong(), any(), any())).thenReturn(issued);

            mockMvc.perform(post("/api/invoices/issue")
                            .param("orderId", "1")
                            .param("issueDate", "2024-05-01")
                            .param("dueDate", "2024-05-31"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(10)));
        }

        @Test
        @DisplayName("異常系: 未納品受注への発行は 409 を返す")
        void returns409_whenNotDelivered() throws Exception {
            when(invoiceService.issueInvoiceFromOrder(anyLong(), any(), any()))
                    .thenThrow(new IllegalStateException("納品済み受注のみ請求書を発行できます"));

            mockMvc.perform(post("/api/invoices/issue")
                            .param("orderId", "2")
                            .param("issueDate", "2024-05-01")
                            .param("dueDate", "2024-05-31"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("納品済み")));
        }

        @Test
        @DisplayName("異常系: 支払期限が発行日より前の場合 400 を返す")
        void returns400_whenDueDateBeforeIssueDate() throws Exception {
            when(invoiceService.issueInvoiceFromOrder(anyLong(), any(), any()))
                    .thenThrow(new IllegalArgumentException("支払期限は発行日以降である必要があります"));

            mockMvc.perform(post("/api/invoices/issue")
                            .param("orderId", "1")
                            .param("issueDate", "2024-05-31")
                            .param("dueDate", "2024-05-01"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // PATCH /api/invoices/{id}/pay
    // ============================================================
    @Nested
    @DisplayName("PATCH /api/invoices/{id}/pay")
    class MarkAsPaid {

        @Test
        @DisplayName("正常系: 200 と支払済み請求書を返す")
        void returns200() throws Exception {
            Invoice paid = sampleInvoice(1L, Invoice.Status.PAID);
            when(invoiceService.markAsPaid(1L)).thenReturn(paid);

            mockMvc.perform(patch("/api/invoices/1/pay"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("PAID")));
        }

        @Test
        @DisplayName("異常系: 存在しないIDで 404 を返す")
        void returns404() throws Exception {
            when(invoiceService.markAsPaid(99L))
                    .thenThrow(new NoSuchElementException("請求書が見つかりません: id=99"));

            mockMvc.perform(patch("/api/invoices/99/pay"))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================================
    // POST /api/invoices/check-overdue
    // ============================================================
    @Nested
    @DisplayName("POST /api/invoices/check-overdue")
    class CheckOverdue {

        @Test
        @DisplayName("正常系: 200 と更新件数を返す")
        void returns200WithCount() throws Exception {
            when(invoiceService.updateOverdueInvoices(any(LocalDate.class))).thenReturn(3);

            mockMvc.perform(post("/api/invoices/check-overdue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updatedCount", is(3)));
        }

        @Test
        @DisplayName("正常系: 更新対象なしのとき updatedCount=0 を返す")
        void returns0_whenNoneOverdue() throws Exception {
            when(invoiceService.updateOverdueInvoices(any())).thenReturn(0);

            mockMvc.perform(post("/api/invoices/check-overdue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updatedCount", is(0)));
        }
    }
}
