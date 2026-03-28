package com.lumber.repository;

import com.lumber.model.Order;
import com.lumber.model.OrderItem;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 受注リポジトリ（DB不使用・インメモリダミーデータ）
 */
@Repository
public class OrderRepository {

    private final AtomicLong idSequence = new AtomicLong(4L);
    private final Map<Long, Order> store = new LinkedHashMap<>();

    public OrderRepository() {
        // ダミーデータ初期化
        Order o1 = Order.builder()
                .id(1L)
                .orderNo("ORD-2024-001")
                .orderDate(LocalDate.of(2024, 4, 1))
                .deliveryDate(LocalDate.of(2024, 4, 10))
                .customerId(1L)
                .customerName("山田建設株式会社")
                .status(Order.Status.DELIVERED)
                .items(List.of(
                        OrderItem.builder().productId(1L).productName("SPF 2×4材").quantity(100).unitPrice(398).subtotal(39800).build(),
                        OrderItem.builder().productId(5L).productName("構造用合板 12mm").quantity(50).unitPrice(1280).subtotal(64000).build()
                ))
                .totalAmount(103800)
                .build();

        Order o2 = Order.builder()
                .id(2L)
                .orderNo("ORD-2024-002")
                .orderDate(LocalDate.of(2024, 4, 5))
                .deliveryDate(LocalDate.of(2024, 4, 15))
                .customerId(2L)
                .customerName("鈴木工務店")
                .status(Order.Status.CONFIRMED)
                .items(List.of(
                        OrderItem.builder().productId(3L).productName("杉 1×4材").quantity(200).unitPrice(450).subtotal(90000).build()
                ))
                .totalAmount(90000)
                .build();

        Order o3 = Order.builder()
                .id(3L)
                .orderNo("ORD-2024-003")
                .orderDate(LocalDate.of(2024, 4, 8))
                .deliveryDate(LocalDate.of(2024, 4, 20))
                .customerId(3L)
                .customerName("田中リフォーム")
                .status(Order.Status.PENDING)
                .items(List.of(
                        OrderItem.builder().productId(4L).productName("パイン集成材").quantity(10).unitPrice(1980).subtotal(19800).build()
                ))
                .totalAmount(19800)
                .build();

        store.put(1L, o1);
        store.put(2L, o2);
        store.put(3L, o3);
    }

    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Order> findByStatus(Order.Status status) {
        return store.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Order> findByCustomerId(Long customerId) {
        return store.values().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .collect(Collectors.toList());
    }

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(idSequence.getAndIncrement());
        }
        store.put(order.getId(), order);
        return order;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public int count() {
        return store.size();
    }
}
