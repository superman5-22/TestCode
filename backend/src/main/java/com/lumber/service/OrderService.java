package com.lumber.service;

import com.lumber.model.Order;
import com.lumber.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 受注サービス
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    /** 全受注取得 */
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /** ID指定で受注取得 */
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("受注が見つかりません: id=" + id));
    }

    /** ステータスで絞り込み */
    public List<Order> findByStatus(Order.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("ステータスはnullにできません");
        }
        return orderRepository.findByStatus(status);
    }

    /** 顧客IDで絞り込み */
    public List<Order> findByCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("顧客IDは正の値である必要があります");
        }
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * 受注の新規登録
     * - 合計金額を自動計算する
     * - 初期ステータスは PENDING
     */
    public Order createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("受注データはnullにできません");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("受注明細は1件以上必要です");
        }
        order.setStatus(Order.Status.PENDING);
        order.recalculateTotal();
        return orderRepository.save(order);
    }

    /**
     * 受注ステータスの更新
     * - DELIVERED → CANCELLED は不可
     * - CANCELLED → 他ステータスへの変更は不可
     */
    public Order updateStatus(Long id, Order.Status newStatus) {
        Order order = findById(id);

        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new IllegalStateException("キャンセル済み受注のステータスは変更できません");
        }
        if (order.getStatus() == Order.Status.DELIVERED && newStatus == Order.Status.CANCELLED) {
            throw new IllegalStateException("納品済み受注はキャンセルできません");
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    /** 受注削除（PENDING のみ削除可） */
    public void deleteOrder(Long id) {
        Order order = findById(id);
        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalStateException("確認待ち状態の受注のみ削除できます");
        }
        orderRepository.deleteById(id);
    }

    /** 受注件数取得 */
    public int countOrders() {
        return orderRepository.count();
    }
}
