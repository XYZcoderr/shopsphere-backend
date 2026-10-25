package com.shopsphere.gateway;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingWebFilter implements WebFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingWebFilter.class);
  private static final String CORRELATION_HEADER = "X-Correlation-ID";
  private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String correlationId = correlationId(exchange.getRequest());
    String path = exchange.getRequest().getURI().getRawPath();
    String method = exchange.getRequest().getMethod() == null
        ? "UNKNOWN"
        : exchange.getRequest().getMethod().name();
    long startedAt = System.nanoTime();

    ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(headers -> headers.set(CORRELATION_HEADER, correlationId))
        .build();
    ServerWebExchange loggedExchange = exchange.mutate().request(request).build();
    loggedExchange.getResponse().beforeCommit(() -> {
      loggedExchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);
      return Mono.empty();
    });

    log.info("http request started component=gateway correlationId={} method={} path={}",
        correlationId, method, path);

    return chain.filter(loggedExchange).doFinally(signal -> {
      var status = loggedExchange.getResponse().getStatusCode();
      long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.info("http request completed component=gateway correlationId={} method={} path={} status={} durationMs={} signal={}",
          correlationId, method, path, status == null ? "uncommitted" : status.value(), durationMs, signal);
    });
  }

  private String correlationId(ServerHttpRequest request) {
    String candidate = request.getHeaders().getFirst(CORRELATION_HEADER);
    return candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()
        ? candidate
        : UUID.randomUUID().toString();
  }
}
