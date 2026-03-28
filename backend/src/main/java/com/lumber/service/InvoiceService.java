package com.lumber.service;

import com.lumber.model.Invoice;
import com.lumber.model.Order;
import com.lumber.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 請求書サービス
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderService orderService;

    /** 全請求書取得 */
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    /** ID指定で取得 */
    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("請求書が見つかりません: id=" + id));
    }

    /** ステータスで絞り込み */
    public List<Invoice> findByStatus(Invoice.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("ステータスはnullにできません");
        }
        return invoiceRepository.findByStatus(status);
    }

    /**
     * 受注IDから請求書を発行する
     * - 対象受注が DELIVERED でない場合は発行不可
     * - 税額は自動計算
     */
    public Invoice issueInvoiceFromOrder(Long orderId, LocalDate issueDate, LocalDate dueDate) {
        Order order = orderService.findById(orderId);

        if (order.getStatus() != Order.Status.DELIVERED) {
            throw new IllegalStateException("納品済み受注のみ請求書を発行できます");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("支払期限は発行日以降である必要があります");
        }

        // 請求書番号生成（実際はDBシーケンス等で採番するが、ここでは簡易実装）
        String invoiceNo = "INV-" + issueDate.getYear() + "-" + String.format("%03d", invoiceRepository.findAll().size() + 1);

        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .amount(order.getTotalAmount())
                .status(Invoice.Status.UNPAID)
                .build();

        invoice.calculateTax();
        return invoiceRepository.save(invoice);
    }

    /**
     * 支払済みに更新
     * - 既に支払済みの場合はそのまま返す
     */
    public Invoice markAsPaid(Long id) {
        Invoice invoice = findById(id);
        if (invoice.getStatus() == Invoice.Status.PAID) {
            return invoice; // 冪等処理
        }
        invoice.setStatus(Invoice.Status.PAID);
        return invoiceRepository.save(invoice);
    }

    /**
     * 期限切れの請求書を OVERDUE に更新する
     * @param today 判定基準日
     * @return 更新された件数
     */
    public int updateOverdueInvoices(LocalDate today) {
        List<Invoice> unpaid = invoiceRepository.findByStatus(Invoice.Status.UNPAID);
        int count = 0;
        for (Invoice inv : unpaid) {
            if (inv.getDueDate().isBefore(today)) {
                inv.setStatus(Invoice.Status.OVERDUE);
                invoiceRepository.save(inv);
                count++;
            }
        }
        return count;
    }
}
