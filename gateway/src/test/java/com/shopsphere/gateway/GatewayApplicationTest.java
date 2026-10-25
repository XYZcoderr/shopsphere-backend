package com.shopsphere.gateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties = {
  "spring.cloud.gateway.routes[0].id=orders",
  "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
  "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/orders/**",
  "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class GatewayApplicationTest { @Test void contextLoads() {} }
