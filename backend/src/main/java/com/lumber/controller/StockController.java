package com.lumber.controller;

import com.lumber.model.Stock;
import com.lumber.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 在庫 REST Controller
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /** GET /api/stocks */
    @GetMapping
    public ResponseEntity<List<Stock>> getAll() {
        return ResponseEntity.ok(stockService.findAll());
    }

    /** GET /api/stocks/low - 補充基準以下の在庫 */
    @GetMapping("/low")
    public ResponseEntity<List<Stock>> getLowStocks() {
        return ResponseEntity.ok(stockService.findLowStocks());
    }

    /**
     * PATCH /api/stocks/{productId}/adjust
     * Body: {"delta": 50} → 在庫増減
     */
    @PatchMapping("/{productId}/adjust")
    public ResponseEntity<Stock> adjust(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> body) {
        Integer delta = body.get("delta");
        if (delta == null) {
            return ResponseEntity.badRequest().build();
        }
        Stock updated = stockService.updateStock(productId, delta);
        return ResponseEntity.ok(updated);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
