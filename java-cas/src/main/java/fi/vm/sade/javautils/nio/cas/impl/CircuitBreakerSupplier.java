package fi.vm.sade.javautils.nio.cas.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class CircuitBreakerSupplier<T> implements Supplier<CompletableFuture<T>> {

  private final Supplier<CompletionStage<T>> decorated;

  public CircuitBreakerSupplier(String name, CircuitBreakerConfig circuitBreakerConfig, Supplier<CompletableFuture<T>> supplier) {
    CircuitBreaker breaker = CircuitBreaker.of(name, circuitBreakerConfig);
    this.decorated = CircuitBreaker.decorateCompletionStage(breaker, supplier::get);
  }

  @Override
  public CompletableFuture<T> get() {
    return decorated.get().toCompletableFuture();
  }
}
