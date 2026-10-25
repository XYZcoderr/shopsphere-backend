package com.shopsphere.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
  private final ConcurrentMap<String, Integer> stock = new ConcurrentHashMap<>();
  public InventoryController() { stock.put("BK-001", 25); stock.put("BK-002", 10); }

  @GetMapping("/{sku}")
  public InventoryView getStock(@PathVariable String sku) {
    Integer n = stock.get(sku);
    if (n == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown SKU");
    return new InventoryView(sku, n);
  }

  @PostMapping("/{sku}/reserve")
  public InventoryView reserve(@PathVariable String sku, @RequestParam(defaultValue = "1") int quantity) {
    if (quantity < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
    try {
      Integer updated = stock.compute(sku, (key, current) -> {
        if (current == null) throw new UnknownSkuException();
        if (current < quantity) throw new InsufficientStockException();
        return current - quantity;
      });
      return new InventoryView(sku, updated);
    } catch (UnknownSkuException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown SKU");
    } catch (InsufficientStockException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
    }
  }

  public record InventoryView(String sku, int available) {}
  static class UnknownSkuException extends RuntimeException {}
  static class InsufficientStockException extends RuntimeException {}
}
