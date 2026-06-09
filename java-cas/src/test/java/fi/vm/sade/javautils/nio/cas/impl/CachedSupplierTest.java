package fi.vm.sade.javautils.nio.cas.impl;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class CachedSupplierTest {

  @Test
  public void testGet() throws Exception {
    CachedSupplier<String> s = new CachedSupplier<>(10000, () -> CompletableFuture.completedFuture("test"));
    Assert.assertEquals("test", s.get().get());
  }

  @Test
  public void testCachedWithinTTL() throws Exception {
    CachedSupplier<String> s = new CachedSupplier<>(10000, new Supplier<>() {
      boolean called = false;

      @Override
      public CompletableFuture<String> get() {
        if(called) throw new RuntimeException();
        called = true;
        return CompletableFuture.completedFuture("test");
      }
    });
    Assert.assertEquals("test", s.get().get());
    Assert.assertEquals("test", s.get().get());
  }

  @Test
  public void testRefreshWhenExpired() throws Exception {
    CachedSupplier<String> s = new CachedSupplier<>(100, new Supplier<>() {
      Queue<String> queue = new LinkedList<>();
      {
        queue.add("test");
        queue.add("test2");
      }

      @Override
      public CompletableFuture<String> get() {
        return CompletableFuture.completedFuture(queue.remove());
      }
    });
    Assert.assertEquals("test", s.get().get());
    Thread.sleep(150);
    Assert.assertEquals("test2", s.get().get());
  }

  @Test
  public void testExceptionPropagated() {
    CachedSupplier<String> s = new CachedSupplier<>(100, () -> CompletableFuture.failedFuture(new RuntimeException("test")));

    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertEquals("test", e.getCause().getMessage());
    } catch (InterruptedException e) {
      Assert.fail();
    }
  }

  @Test
  public void testSynchronousExceptionWrappedAsFailedFuture() {
    CachedSupplier<String> s = new CachedSupplier<>(100, () -> {
      throw new RuntimeException("sync");
    });

    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertEquals("sync", e.getCause().getMessage());
    } catch (InterruptedException e) {
      Assert.fail();
    }
  }

  @Test
  public void testExceptionNotCached() {
    CachedSupplier<String> s = new CachedSupplier<>(100000, new Supplier<>() {
      Queue<String> queue = new LinkedList<>();
      {
        queue.add("test");
        queue.add("test2");
      }

      @Override
      public CompletableFuture<String> get() {
        return CompletableFuture.failedFuture(new RuntimeException(queue.remove()));
      }
    });

    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertEquals("test", e.getCause().getMessage());
    } catch (InterruptedException e) {
      Assert.fail();
    }
    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertEquals("test2", e.getCause().getMessage());
    } catch (InterruptedException e) {
      Assert.fail();
    }
  }

  @Test
  public void testSynchronized() {
    final int NUMBER_OF_THREADS = 50;
    final int INVOCATIONS_PER_THREAD = 100;

    // supplier contains a non atomic counter
    CachedSupplier<String> s = new CachedSupplier<>(100000, new Supplier<>() {
      int counter = 0;

      @Override
      public CompletableFuture<String> get() {
        return CompletableFuture.failedFuture(new RuntimeException(counter++ + ""));
      }
    });

    // have multiple threads increment the counter
    Collection<CompletableFuture<Void>> futures = new ArrayList<>();
    for(int i = 0; i < NUMBER_OF_THREADS; i++) {
      CompletableFuture<Void> f = new CompletableFuture<>();
      futures.add(f);
      new Thread(() -> {
        for(int j = 0; j < INVOCATIONS_PER_THREAD; j++) {
          try {
            s.get().get();
          } catch (Exception e) {}
        }
        f.complete(null);
      }).start();
    }

    // increments match total number of invocations
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    try {
      s.get().get();
      Assert.fail();
    } catch (ExecutionException e) {
      Assert.assertEquals(NUMBER_OF_THREADS*INVOCATIONS_PER_THREAD + "", e.getCause().getMessage());
    } catch (InterruptedException e) {
      Assert.fail();
    }
  }

  @Test
  public void testInFlightSharedAcrossCallers() throws Exception {
    final CompletableFuture<String> gate = new CompletableFuture<>();
    final int[] callCount = {0};
    CachedSupplier<String> s = new CachedSupplier<>(100000, () -> {
      callCount[0]++;
      return gate;
    });

    // First call installs the in-flight future
    CompletableFuture<String> f1 = s.get();
    // Concurrent call should share the same in-flight future, not start a second fetch
    CompletableFuture<String> f2 = s.get();
    Assert.assertSame(f1, f2);
    Assert.assertEquals(1, callCount[0]);

    gate.complete("done");
    Assert.assertEquals("done", f1.get());
    Assert.assertEquals("done", f2.get());
  }
}
