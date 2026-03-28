package com.lumber.repository;

import com.lumber.model.Stock;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 在庫リポジトリ（DB不使用・インメモリダミーデータ）
 */
@Repository
public class StockRepository {

    private final Map<Long, Stock> store = new LinkedHashMap<>();

    public StockRepository() {
        store.put(1L, Stock.builder().productId(1L).productName("SPF 2×4材 (6F)").quantity(350).unit("本").threshold(100).build());
        store.put(2L, Stock.builder().productId(2L).productName("SPF 2×6材 (6F)").quantity(85).unit("本").threshold(100).build());
        store.put(3L, Stock.builder().productId(3L).productName("杉 1×4材 (6F)").quantity(420).unit("本").threshold(200).build());
        store.put(4L, Stock.builder().productId(4L).productName("パイン集成材 18×200×1820").quantity(25).unit("枚").threshold(30).build());
        store.put(5L, Stock.builder().productId(5L).productName("構造用合板 12mm 910×1820").quantity(180).unit("枚").threshold(50).build());
    }

    public List<Stock> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Stock> findByProductId(Long productId) {
        return Optional.ofNullable(store.get(productId));
    }

    public List<Stock> findLowStocks() {
        return store.values().stream()
                .filter(Stock::isLow)
                .collect(Collectors.toList());
    }

    public Stock save(Stock stock) {
        store.put(stock.getProductId(), stock);
        return stock;
    }
}
