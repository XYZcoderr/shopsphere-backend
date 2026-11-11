package com.shopsphere.inventory;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
  private final JdbcTemplate jdbc;
  public InventoryController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @GetMapping("/{sku}")
  public InventoryView getStock(@PathVariable String sku) {
    List<Integer> rows = jdbc.query("SELECT available FROM inventory_stock WHERE sku = ?", (rs, row) -> rs.getInt("available"), sku);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown SKU");
    return new InventoryView(sku, rows.get(0));
  }

  @PostMapping("/{sku}/reserve")
  @Transactional
  public InventoryView reserve(@PathVariable String sku, @RequestParam(defaultValue = "1") int quantity) {
    if (quantity < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
    int changed = jdbc.update("UPDATE inventory_stock SET available = available - ? WHERE sku = ? AND available >= ?", quantity, sku, quantity);
    if (changed == 1) return getStock(sku);
    if (!skuExists(sku)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown SKU");
    throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
  }

  @GetMapping("/reservations/{orderId}")
  public ReservationView getReservation(@PathVariable String orderId) {
    List<ReservationView> rows = jdbc.query("SELECT order_id, sku, quantity, status, reason FROM inventory_reservations WHERE order_id = ?",
        (rs, row) -> new ReservationView(rs.getString("order_id"), rs.getString("sku"), rs.getInt("quantity"), rs.getString("status"), rs.getString("reason")), orderId);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found; the order event may still be processing");
    return rows.get(0);
  }

  @Transactional
  public ReservationView reserveForOrder(String orderId, String sku, int quantity) {
    int inserted = jdbc.update("INSERT INTO inventory_reservations(order_id, sku, quantity, status) VALUES (?, ?, ?, 'PROCESSING') ON CONFLICT (order_id) DO NOTHING", orderId, sku, quantity);
    if (inserted == 0) return getReservation(orderId);
    int changed = jdbc.update("UPDATE inventory_stock SET available = available - ? WHERE sku = ? AND available >= ?", quantity, sku, quantity);
    String status = changed == 1 ? "RESERVED" : "REJECTED";
    String reason = changed == 1 ? null : (skuExists(sku) ? "INSUFFICIENT_STOCK" : "UNKNOWN_SKU");
    jdbc.update("UPDATE inventory_reservations SET status = ?, reason = ? WHERE order_id = ?", status, reason, orderId);
    return getReservation(orderId);
  }

  private boolean skuExists(String sku) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM inventory_stock WHERE sku = ?", Integer.class, sku);
    return count != null && count > 0;
  }
  public record InventoryView(String sku, int available) {}
  public record ReservationView(String orderId, String sku, int quantity, String status, String reason) {}
}
