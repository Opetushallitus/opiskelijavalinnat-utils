package fi.vm.sade.javautils.nio.cas.impl;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class CircuitBreakerSupplierTest {

  private static final int MINIMUM_NUMBER_OF_CALLS = 6;
  private static final int PERMITTED_IN_HALF_OPEN = 4;
  private static final int WAIT_IN_OPEN_STATE = 100;

  CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
      .failureRateThreshold(50)
      .slowCallRateThreshold(50)
      .waitDurationInOpenState(Duration.ofMillis(WAIT_IN_OPEN_STATE))
      .slowCallDurationThreshold(Duration.ofSeconds(10))
      .permittedNumberOfCallsInHalfOpenState(4)
      .minimumNumberOfCalls(6)
      .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
      .slidingWindowSize(10)
      .build();

  @Test
  public void testGet() throws Exception {
    CircuitBreakerSupplier<String> s = new CircuitBreakerSupplier<>("get-test", circuitBreakerConfig,
        () -> CompletableFuture.completedFuture("test"));
    Assert.assertEquals("test", s.get().get());
  }

  @Test
  public void testOpens() {
    CircuitBreakerSupplier<String> s = new CircuitBreakerSupplier<>("opens-test", circuitBreakerConfig,
        () -> CompletableFuture.failedFuture(new RuntimeException("failure")));

    // after a configured number of failed calls, circuitbreaker will start producing CallNotPermittedExceptions
    for(int i=0;i<MINIMUM_NUMBER_OF_CALLS;i++) {
      try {
        s.get().get();
      } catch (Exception e) {}
    }

    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertTrue("expected CallNotPermittedException but got " + e.getCause(),
          e.getCause() instanceof CallNotPermittedException);
      return;
    } catch (InterruptedException e) {
      Assert.fail();
    }
    Assert.fail();
  }

  @Test
  public void testCircuitBreakerExceptionIsAFailedFutureNotASynchronousThrow() {
    CircuitBreakerSupplier<String> s = new CircuitBreakerSupplier<>("not-sync-throw-test", circuitBreakerConfig,
        () -> CompletableFuture.failedFuture(new RuntimeException("failure")));

    for(int i=0;i<MINIMUM_NUMBER_OF_CALLS;i++) {
      try { s.get().get(); } catch (Exception e) {}
    }

    // get() must NOT throw synchronously when the breaker is open; it must return a failed future.
    CompletableFuture<String> future;
    try {
      future = s.get();
    } catch (Throwable t) {
      Assert.fail("get() threw synchronously: " + t);
      return;
    }
    Assert.assertTrue(future.isCompletedExceptionally());
  }

  @Test
  public void testRecovery() throws Exception {
    CircuitBreakerSupplier<String> s = new CircuitBreakerSupplier<>("recovery-test", circuitBreakerConfig, new Supplier<CompletableFuture<String>>() {
      Queue<Boolean> queue = new LinkedList<>();
      {
        // calls in open state
        queue.add(false);
        queue.add(false);
        queue.add(false);
        queue.add(false);
        queue.add(false);
        queue.add(false);

        // calls in half-open state
        queue.add(true);
        queue.add(true);
        queue.add(true);
        queue.add(true);

        // calls in open state
        queue.add(false);
      }

      @Override
      public CompletableFuture<String> get() {
        if(queue.remove()) {
          return CompletableFuture.completedFuture("success");
        }
        return CompletableFuture.failedFuture(new RuntimeException("failure"));
      }
    });

    // after a configured number of failed calls, circuitbreaker will open
    for(int i=0;i<MINIMUM_NUMBER_OF_CALLS;i++) {
      try { s.get().get(); } catch (Exception e) {}
    }

    // ...  and start producing CallNotPermittedExceptions
    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof CallNotPermittedException);
    }

    // after we wait for circuit breaker to turn into half open state
    Thread.sleep(WAIT_IN_OPEN_STATE);

    // ... limited number of calls go through to original supplier
    for(int i=0;i<PERMITTED_IN_HALF_OPEN;i++) {
      s.get().get();
    }

    // ... and as more than half succeed, the circuitbreaker opens again
    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertEquals("failure", e.getCause().getMessage());
      return;
    }
    Assert.fail();
  }
}
