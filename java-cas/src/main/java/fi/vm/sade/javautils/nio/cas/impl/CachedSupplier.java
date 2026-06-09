package fi.vm.sade.javautils.nio.cas.impl;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CachedSupplier<T> implements Supplier<CompletableFuture<T>> {

  private CompletableFuture<T> cached;
  private Instant validUntil;
  private final long ttlMs;

  private final Supplier<CompletableFuture<T>> supplier;

  public CachedSupplier(long ttlMs, Supplier<CompletableFuture<T>> supplier) {
    this.ttlMs = ttlMs;
    this.supplier = supplier;
  }

  public void clear() {
    synchronized (this) {
      this.cached = null;
      this.validUntil = null;
    }
  }

  @Override
  public CompletableFuture<T> get() {
    synchronized (this) {
      Instant now = Instant.now();
      if (cached == null || validUntil.isBefore(now)) {
        CompletableFuture<T> inFlight;
        try {
          inFlight = supplier.get();
          if (inFlight == null) {
            return CompletableFuture.failedFuture(new NullPointerException("supplier returned null"));
          }
        } catch (Throwable t) {
          this.cached = null;
          this.validUntil = null;
          return CompletableFuture.failedFuture(t);
        }
        cached = inFlight;
        validUntil = Instant.now().plusMillis(this.ttlMs);
        inFlight.whenComplete((v, t) -> {
          if (t != null) {
            clearIfCurrent(inFlight);
          }
        });
        return inFlight;
      }
      return cached;
    }
  }

  private void clearIfCurrent(CompletableFuture<T> expected) {
    synchronized (this) {
      if (cached == expected) {
        cached = null;
        validUntil = null;
      }
    }
  }
}
