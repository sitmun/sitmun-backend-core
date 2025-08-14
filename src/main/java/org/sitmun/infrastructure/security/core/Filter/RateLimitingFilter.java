package org.sitmun.infrastructure.security.core.Filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;

public class RateLimitingFilter implements Filter {

  // Map pour stocker le compteur et le timestamp par IP/hostname
  private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

  private final int maxRequests;
  private final int durationInMinutes;

  // Scheduler pour réinitialiser les compteurs
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public RateLimitingFilter(int maxRequests, int durationInMinutes) {
    this.maxRequests = maxRequests;
    this.durationInMinutes = durationInMinutes;

    // Planifier une réinitialisation périodique
    scheduler.scheduleAtFixedRate(
        requestCounts::clear, durationInMinutes, durationInMinutes, TimeUnit.MINUTES);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String clientIp = httpRequest.getRemoteAddr();

    requestCounts.putIfAbsent(clientIp, new RequestCounter());
    RequestCounter counter = requestCounts.get(clientIp);

    int requests = counter.increment();

    if (requests > maxRequests) {
      httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      httpResponse.getWriter().write("Too many requests. Please try again later.");
      return;
    }

    chain.doFilter(request, response);
  }

  private static class RequestCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    int increment() {
      return count.incrementAndGet();
    }
  }

  @Override
  public void destroy() {
    scheduler.shutdown();
  }
}
