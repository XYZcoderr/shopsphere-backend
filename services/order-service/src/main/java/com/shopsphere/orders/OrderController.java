package com.shopsphere.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, OrderCreatedEvent> kafka;
  public OrderController(JdbcTemplate jdbc, KafkaTemplate<String, OrderCreatedEvent> kafka) {
    this.jdbc = jdbc; this.kafka = kafka;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderView create(@Valid @RequestBody CreateOrderRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    // Intentional teaching flaw: direct DB + Kafka dual write. Replace with transactional outbox in chapter 5.
    String id = UUID.randomUUID().toString();
    Instant now = Instant.now();
    jdbc.update("INSERT INTO orders(id, customer_id, sku, quantity, status, created_at, idempotency_key) VALUES (?, ?, ?, ?, 'PENDING', ?, ?)",
      id, request.customerId(), request.sku(), request.quantity(), java.sql.Timestamp.from(now), idempotencyKey);
    kafka.send("orders.created.v1", id,
      new OrderCreatedEvent(id, request.customerId(), request.sku(), request.quantity(), now.toString()));
    return new OrderView(id, request.customerId(), request.sku(), request.quantity(), "PENDING", now);
  }

  @GetMapping("/{id}")
  public OrderView get(@PathVariable String id) {
    List<OrderView> rows = jdbc.query("SELECT id, customer_id, sku, quantity, status, created_at FROM orders WHERE id = ?",
      (rs, row) -> new OrderView(rs.getString("id"), rs.getString("customer_id"), rs.getString("sku"),
        rs.getInt("quantity"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()), id);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
    return rows.get(0);
  }

  public record CreateOrderRequest(@NotBlank String customerId, @NotBlank String sku, @Min(1) int quantity) {}
  public record OrderView(String id, String customerId, String sku, int quantity, String status, Instant createdAt) {}
  public record OrderCreatedEvent(String orderId, String customerId, String sku, int quantity, String createdAt) {}
}
