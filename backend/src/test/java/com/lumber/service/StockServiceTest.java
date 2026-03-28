package com.lumber.service;

import com.lumber.model.Stock;
import com.lumber.repository.StockRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * StockService 単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService テスト")
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    private Stock buildStock(Long productId, int quantity, int threshold) {
        return Stock.builder()
                .productId(productId)
                .productName("テスト商品" + productId)
                .quantity(quantity)
                .unit("本")
                .threshold(threshold)
                .build();
    }

    // ============================================================
    // findAll / findLowStocks
    // ============================================================
    @Nested
    @DisplayName("findAll() / findLowStocks()")
    class Retrieval {

        @Test
        @DisplayName("正常系: 全在庫を返す")
        void returnsAll() {
            when(stockRepository.findAll()).thenReturn(List.of(
                    buildStock(1L, 100, 50),
                    buildStock(2L, 20, 50)
            ));

            assertThat(stockService.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("正常系: 補充基準以下の在庫を返す")
        void returnsLowStocks() {
            when(stockRepository.findLowStocks()).thenReturn(List.of(buildStock(2L, 20, 50)));

            assertThat(stockService.findLowStocks()).hasSize(1);
        }
    }

    // ============================================================
    // updateStock
    // ============================================================
    @Nested
    @DisplayName("updateStock()")
    class UpdateStock {

        @Test
        @DisplayName("正常系: 入庫（delta正）で在庫が増える")
        void increasesStock_withPositiveDelta() {
            Stock stock = buildStock(1L, 100, 50);
            when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(stock));
            when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = stockService.updateStock(1L, 50);

            assertThat(result.getQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("正常系: 出庫（delta負）で在庫が減る")
        void decreasesStock_withNegativeDelta() {
            Stock stock = buildStock(1L, 100, 50);
            when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(stock));
            when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = stockService.updateStock(1L, -30);

            assertThat(result.getQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("境界値: delta=0 でも正常終了し在庫数は変わらない")
        void noChange_whenDeltaIsZero() {
            Stock stock = buildStock(1L, 100, 50);
            when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(stock));
            when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = stockService.updateStock(1L, 0);

            assertThat(result.getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("境界値: 在庫数と同じだけ出庫すると在庫数が0になる")
        void stockBecomesZero_whenDeltaEqualsQuantity() {
            Stock stock = buildStock(1L, 100, 50);
            when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(stock));
            when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = stockService.updateStock(1L, -100);

            assertThat(result.getQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("異常系: 出庫数が在庫数を超えると IllegalStateException")
        void throwsException_whenStockInsufficient() {
            Stock stock = buildStock(1L, 10, 5);
            when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(stock));

            assertThatThrownBy(() -> stockService.updateStock(1L, -11))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("在庫数が不足");
        }

        @Test
        @DisplayName("異常系: 存在しない商品IDで NoSuchElementException")
        void throwsException_whenProductNotFound() {
            when(stockRepository.findByProductId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.updateStock(99L, 10))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("異常系: 大量出庫で在庫がマイナスにならないこと")
        void doesNotGoBelowZero() {
            Stock stock = buildStock(1L, 5, 2);
            when(stockRepository.findByProductId(1L)).thenReturn(Optional.of(stock));

            assertThatThrownBy(() -> stockService.updateStock(1L, -100))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    // Stock.isLow() モデルロジックテスト
    // ============================================================
    @Nested
    @DisplayName("Stock.isLow() ロジックテスト")
    class StockIsLow {

        @Test
        @DisplayName("在庫数 < 閾値のとき true")
        void isLow_whenBelowThreshold() {
            Stock stock = buildStock(1L, 49, 50);
            assertThat(stock.isLow()).isTrue();
        }

        @Test
        @DisplayName("在庫数 = 閾値のとき true（等値は補充必要）")
        void isLow_whenEqualsThreshold() {
            Stock stock = buildStock(1L, 50, 50);
            assertThat(stock.isLow()).isTrue();
        }

        @Test
        @DisplayName("在庫数 > 閾値のとき false")
        void isNotLow_whenAboveThreshold() {
            Stock stock = buildStock(1L, 51, 50);
            assertThat(stock.isLow()).isFalse();
        }
    }
}
