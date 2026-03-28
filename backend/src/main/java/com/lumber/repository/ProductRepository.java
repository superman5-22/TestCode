package com.lumber.repository;

import com.lumber.model.Product;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 商品リポジトリ（DB不使用・インメモリダミーデータ）
 */
@Repository
public class ProductRepository {

    private final AtomicLong idSequence = new AtomicLong(6L);
    private final Map<Long, Product> store = new LinkedHashMap<>();

    public ProductRepository() {
        store.put(1L, Product.builder().id(1L).code("SPF-2x4").name("SPF 2×4材 (6F)").unit("本").price(398).build());
        store.put(2L, Product.builder().id(2L).code("SPF-2x6").name("SPF 2×6材 (6F)").unit("本").price(598).build());
        store.put(3L, Product.builder().id(3L).code("CEDAR-1x4").name("杉 1×4材 (6F)").unit("本").price(450).build());
        store.put(4L, Product.builder().id(4L).code("PINE-BOARD").name("パイン集成材 18×200×1820").unit("枚").price(1980).build());
        store.put(5L, Product.builder().id(5L).code("PLYWOOD-12").name("構造用合板 12mm 910×1820").unit("枚").price(1280).build());
    }

    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<Product> findByCode(String code) {
        return store.values().stream().filter(p -> p.getCode().equals(code)).findFirst();
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(idSequence.getAndIncrement());
        }
        store.put(product.getId(), product);
        return product;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }
}
