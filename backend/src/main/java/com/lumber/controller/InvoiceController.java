package com.lumber.controller;

import com.lumber.model.Invoice;
import com.lumber.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 請求書 REST Controller
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /** GET /api/invoices */
    @GetMapping
    public ResponseEntity<List<Invoice>> getAll(
            @RequestParam(required = false) Invoice.Status status) {
        List<Invoice> result = (status != null)
                ? invoiceService.findByStatus(status)
                : invoiceService.findAll();
        return ResponseEntity.ok(result);
    }

    /** GET /api/invoices/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    /**
     * POST /api/invoices/issue?orderId={orderId}&issueDate={date}&dueDate={date}
     * 受注から請求書を発行する
     */
    @PostMapping("/issue")
    public ResponseEntity<Invoice> issue(
            @RequestParam Long orderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        Invoice issued = invoiceService.issueInvoiceFromOrder(orderId, issueDate, dueDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(issued);
    }

    /** PATCH /api/invoices/{id}/pay - 支払済みに更新 */
    @PatchMapping("/{id}/pay")
    public ResponseEntity<Invoice> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.markAsPaid(id));
    }

    /** POST /api/invoices/check-overdue - 期限切れ更新 */
    @PostMapping("/check-overdue")
    public ResponseEntity<Map<String, Integer>> checkOverdue() {
        int count = invoiceService.updateOverdueInvoices(LocalDate.now());
        return ResponseEntity.ok(Map.of("updatedCount", count));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
