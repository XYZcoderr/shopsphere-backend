package com.shopsphere.inventory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
  private static final String CORRELATION_HEADER = "X-Correlation-ID";
  private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String correlationId = correlationId(request);
    String method = request.getMethod();
    String path = request.getRequestURI();
    long startedAt = System.nanoTime();
    boolean completed = false;

    response.setHeader(CORRELATION_HEADER, correlationId);
    log.info("http request started component=inventory correlationId={} method={} path={}",
        correlationId, method, path);

    try {
      filterChain.doFilter(request, response);
      completed = true;
    } finally {
      long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.info("http request completed component=inventory correlationId={} method={} path={} status={} durationMs={} outcome={}",
          correlationId, method, path, response.getStatus(), durationMs,
          completed ? "completed" : "error");
    }
  }

  private String correlationId(HttpServletRequest request) {
    String candidate = request.getHeader(CORRELATION_HEADER);
    return candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()
        ? candidate
        : UUID.randomUUID().toString();
  }
}
