package fi.vm.sade.javautils.nio.cas.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FutureSupplier<T> implements Supplier<CompletableFuture<T>> {

  private final Supplier<T> supplier;

  public FutureSupplier(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @Override
  public CompletableFuture<T> get() {
    try {
      T value = this.supplier.get();
      return CompletableFuture.completedFuture(value);
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }
}
