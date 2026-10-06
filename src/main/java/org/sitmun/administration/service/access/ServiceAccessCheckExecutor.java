package org.sitmun.administration.service.access;

import io.github.resilience4j.timelimiter.TimeLimiter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.sitmun.administration.service.access.AccessProbePlan.Authorization;
import org.sitmun.administration.service.access.AccessProbePlan.Authorization.Basic;
import org.sitmun.administration.service.extractor.capabilities.AccessProbePlanner;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheck;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.upstream.http.BasicAuthorization;
import org.sitmun.upstream.http.UpstreamHttpClient;
import org.sitmun.upstream.signal.Classification;
import org.sitmun.upstream.signal.ExchangeSignals;
import org.sitmun.upstream.signal.RequestContext;
import org.sitmun.upstream.signal.ServiceCheckClassifier;
import org.sitmun.upstream.signal.ServiceStatuses;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.stereotype.Service
public class ServiceAccessCheckExecutor {

  private final ServiceRepository serviceRepository;
  private final ServiceCheckStore store;
  private final ServiceCheckClassifier classifier;
  private final UpstreamHttpClient httpClient;
  private final TimeLimiter timeLimiter;
  private final ServiceCheckInFlight inFlight;
  private final ExecutorService executor;
  private final ServiceCheckProperties properties;

  public ServiceAccessCheckExecutor(
      ServiceRepository serviceRepository,
      ServiceCheckStore store,
      ServiceCheckClassifier classifier,
      UpstreamHttpClient httpClient,
      TimeLimiter timeLimiter,
      ServiceCheckInFlight inFlight,
      @Qualifier("serviceCheckExecutor") ExecutorService executor,
      ServiceCheckProperties properties) {
    this.serviceRepository = serviceRepository;
    this.store = store;
    this.classifier = classifier;
    this.httpClient = httpClient;
    this.timeLimiter = timeLimiter;
    this.inFlight = inFlight;
    this.executor = executor;
    this.properties = properties;
  }

  public Optional<ServiceAccessObservation> check(Integer serviceId) {
    return check(serviceId, true);
  }

  public Optional<ServiceAccessObservation> check(Integer serviceId, boolean acquirePermit) {
    Service service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Service " + serviceId + " not found"));
    AccessProbePlan plan = AccessProbePlanner.plan(service);
    Outcome outcome = probe(plan, acquirePermit);
    if (outcome.classification().isEmpty()) {
      return Optional.empty();
    }
    Classification classification = outcome.classification().get();
    ServiceCheck saved =
        store.record(
            service,
            ServiceCheckStore.BACKEND,
            classification.status(),
            outcome.elapsedMs(),
            detail(outcome.context(), classification.evidence(), properties.detailMaxLength()),
            outcome.observedAt());
    return Optional.of(ServiceAccessObservation.from(saved));
  }

  private Outcome probe(AccessProbePlan plan, boolean acquirePermit) {
    if (acquirePermit) {
      try {
        inFlight.acquire();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "access check interrupted");
      }
    }
    long started = System.nanoTime();
    try {
      return timeLimiter.executeFutureSupplier(
          () -> CompletableFuture.supplyAsync(() -> exchange(plan), executor));
    } catch (TimeoutException ex) {
      return timeout(plan, elapsedMs(started));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "access check interrupted");
    } catch (Exception ex) {
      Throwable cause = ex.getCause() == null ? ex : ex.getCause();
      if (cause instanceof TimeoutException) {
        return timeout(plan, elapsedMs(started));
      }
      if (cause instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "access check interrupted");
      }
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new IllegalStateException(cause);
    } finally {
      if (acquirePermit) {
        inFlight.release();
      }
    }
  }

  private Outcome exchange(AccessProbePlan plan) {
    long started = System.nanoTime();
    Attempt kept = fetch(plan, plan.url());
    if (plan.fallbackUrl() != null && shouldFallback(kept.signals())) {
      kept = fetch(plan, plan.fallbackUrl());
    }
    RequestContext context = context(plan, kept.uri());
    return new Outcome(
        context, classifier.classify(context, kept.signals()), elapsedMs(started), Instant.now());
  }

  private Attempt fetch(AccessProbePlan plan, URI uri) {
    Request request = request(plan, uri);
    try (Response response = httpClient.execute(request)) {
      return new Attempt(uri, read(response, plan.interpretBody()));
    } catch (IOException ex) {
      return new Attempt(uri, ExchangeSignals.transport(ex));
    }
  }

  private ExchangeSignals read(Response response, boolean interpretBody) throws IOException {
    ExchangeSignals signals = ExchangeSignals.http(response.code());
    ResponseBody body = response.body();
    if (body == null) {
      return signals;
    }
    try (InputStream input = body.byteStream()) {
      if (!interpretBody) {
        input.readNBytes(properties.scanMaxBytes());
        return signals;
      }
      return signals.scan(input, properties.scanMaxBytes(), properties.exceptionTextMaxLength());
    }
  }

  private static Request request(AccessProbePlan plan, URI uri) {
    Request.Builder builder = new Request.Builder().url(uri.toString()).get();
    Authorization authorization = plan.authorization();
    if (authorization instanceof Basic basic) {
      BasicAuthorization.headerValue(basic.username(), basic.password())
          .ifPresent(header -> builder.header(BasicAuthorization.HEADER, header));
    }
    return builder.build();
  }

  private static boolean shouldFallback(ExchangeSignals signals) {
    Integer status = signals.httpStatus();
    if (status == null) {
      return false;
    }
    if (status >= 400) {
      return true;
    }
    return (signals.ogcExceptionText() != null && !signals.ogcExceptionText().isBlank())
        || (signals.ogcExceptionCode() != null && !signals.ogcExceptionCode().isBlank());
  }

  private static Outcome timeout(AccessProbePlan plan, long elapsedMs) {
    return new Outcome(
        context(plan, plan.url()),
        Optional.of(new Classification(ServiceStatuses.TIMEOUT, "TimeoutException")),
        elapsedMs,
        Instant.now());
  }

  public static String detail(RequestContext context, String evidence, int maxLength) {
    if (evidence == null || evidence.isEmpty()) {
      return "";
    }
    String host = context.host() == null ? "" : context.host();
    String path = context.path() == null ? "" : context.path();
    String line = context.request() + " " + host + path + " | " + evidence;
    if (line.length() <= maxLength) {
      return line;
    }
    if (maxLength <= 0) {
      return "";
    }
    return line.substring(0, maxLength);
  }

  private static RequestContext context(AccessProbePlan plan, URI uri) {
    String path = uri.getRawPath() == null ? "" : uri.getRawPath();
    return new RequestContext(plan.protocol(), plan.request(), true, uri.getHost(), path);
  }

  private static long elapsedMs(long startedNanos) {
    return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
  }

  private record Attempt(URI uri, ExchangeSignals signals) {}

  private record Outcome(
      RequestContext context,
      Optional<Classification> classification,
      long elapsedMs,
      Instant observedAt) {}
}
