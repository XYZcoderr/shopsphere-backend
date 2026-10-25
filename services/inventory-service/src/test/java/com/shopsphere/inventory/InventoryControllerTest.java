package com.shopsphere.inventory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {
  @Autowired MockMvc mvc;
  @Test void seededSkuReturnsStock() throws Exception {
    mvc.perform(get("/api/inventory/BK-001")).andExpect(status().isOk())
      .andExpect(jsonPath("$.sku").value("BK-001")).andExpect(jsonPath("$.available").value(25));
  }
  @Test void unknownSkuReturns404() throws Exception {
    mvc.perform(get("/api/inventory/UNKNOWN")).andExpect(status().isNotFound());
  }
  @Test void invalidQuantityReturns400() throws Exception {
    mvc.perform(post("/api/inventory/BK-001/reserve").param("quantity", "0")).andExpect(status().isBadRequest());
  }
}
