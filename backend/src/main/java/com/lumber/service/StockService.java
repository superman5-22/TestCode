package com.lumber.service;

import com.lumber.model.Stock;
import com.lumber.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 在庫サービス
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    /** 全在庫取得 */
    public List<Stock> findAll() {
        return stockRepository.findAll();
    }

    /** 補充基準を下回っている在庫一覧 */
    public List<Stock> findLowStocks() {
        return stockRepository.findLowStocks();
    }

    /**
     * 在庫数の更新
     * @param productId 商品ID
     * @param delta     増減数（正:入庫 / 負:出庫）
     */
    public Stock updateStock(Long productId, int delta) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("在庫情報が見つかりません: productId=" + productId));

        int newQuantity = stock.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new IllegalStateException("在庫数が不足しています。現在庫: " + stock.getQuantity() + ", 要求数: " + Math.abs(delta));
        }
        stock.setQuantity(newQuantity);
        return stockRepository.save(stock);
    }
}
