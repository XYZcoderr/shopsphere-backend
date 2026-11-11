package com.shopsphere.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {
  private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
  private final ObjectMapper objectMapper;
  private final InventoryController inventory;
  public OrderCreatedListener(ObjectMapper objectMapper, InventoryController inventory) {
    this.objectMapper = objectMapper;
    this.inventory = inventory;
  }

  @KafkaListener(topics = "orders.created.v1", groupId = "${spring.kafka.consumer.group-id}")
  public void onOrderCreated(String message) throws Exception {
    OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
    var reservation = inventory.reserveForOrder(event.orderId(), event.sku(), event.quantity());
    log.info("component=inventory event=OrderCreated orderId={} sku={} quantity={} reservationStatus={} reason={}",
        event.orderId(), event.sku(), event.quantity(), reservation.status(), reservation.reason());
  }
  public record OrderCreatedEvent(String orderId, String customerId, String sku, int quantity, String createdAt) {}
}
