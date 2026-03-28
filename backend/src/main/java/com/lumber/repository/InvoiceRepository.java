package com.lumber.repository;

import com.lumber.model.Invoice;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 請求書リポジトリ（DB不使用・インメモリダミーデータ）
 */
@Repository
public class InvoiceRepository {

    private final AtomicLong idSequence = new AtomicLong(3L);
    private final Map<Long, Invoice> store = new LinkedHashMap<>();

    public InvoiceRepository() {
        Invoice inv1 = Invoice.builder()
                .id(1L)
                .invoiceNo("INV-2024-001")
                .issueDate(LocalDate.of(2024, 4, 30))
                .dueDate(LocalDate.of(2024, 5, 31))
                .customerId(1L)
                .customerName("山田建設株式会社")
                .orderId(1L)
                .orderNo("ORD-2024-001")
                .amount(103800)
                .tax(10380)
                .totalWithTax(114180)
                .status(Invoice.Status.PAID)
                .build();

        Invoice inv2 = Invoice.builder()
                .id(2L)
                .invoiceNo("INV-2024-002")
                .issueDate(LocalDate.of(2024, 4, 30))
                .dueDate(LocalDate.of(2024, 5, 31))
                .customerId(2L)
                .customerName("鈴木工務店")
                .orderId(2L)
                .orderNo("ORD-2024-002")
                .amount(90000)
                .tax(9000)
                .totalWithTax(99000)
                .status(Invoice.Status.UNPAID)
                .build();

        store.put(1L, inv1);
        store.put(2L, inv2);
    }

    public List<Invoice> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Invoice> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Invoice> findByStatus(Invoice.Status status) {
        return store.values().stream()
                .filter(i -> i.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Invoice> findByCustomerId(Long customerId) {
        return store.values().stream()
                .filter(i -> customerId.equals(i.getCustomerId()))
                .collect(Collectors.toList());
    }

    public Invoice save(Invoice invoice) {
        if (invoice.getId() == null) {
            invoice.setId(idSequence.getAndIncrement());
        }
        store.put(invoice.getId(), invoice);
        return invoice;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }
}
