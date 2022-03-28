package fi.vm.sade.javautils.nio.cas.impl;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompletableFutureStore<T> {
    private final AtomicReference<Map.Entry<CompletableFuture<T>, Date>> futureStore;
    private final long msDuration;

    public CompletableFutureStore(TimeUnit unit, long duration) {
        this(unit.toMillis(duration));
    }

    /**
     * @return exposes future store for unit tests
     */
    protected AtomicReference<Map.Entry<CompletableFuture<T>, Date>> getFutureStore() {
        return this.futureStore;
    }

    public CompletableFutureStore(long msDuration) {
        this.futureStore = new AtomicReference<>();
        this.msDuration = msDuration;
    }
    protected Map.Entry<CompletableFuture<T>, Date> newEntry(CompletableFuture<T> entry) {
        Date newExpiration = new Date(System.currentTimeMillis() + msDuration);
        return Map.entry(entry, newExpiration);
    }

    public void increaseTimeToLive(CompletableFuture<T> increaseEntryTTL) {
        Map.Entry<CompletableFuture<T>, Date> currentEntry = futureStore.get();
        if(increaseEntryTTL.equals(currentEntry.getKey())) {
            futureStore.compareAndSet(currentEntry, newEntry(increaseEntryTTL));
        }
    }

    public void clear() {
        futureStore.set(null);
    }

    public CompletableFuture<T> getOrSet(Supplier<CompletableFuture<T>> lazyCreateFunction) {
        final Date NOW = new Date();
        final Function<Map.Entry<CompletableFuture<T>, Date>, Boolean> isStillValid = entry -> NOW.before(entry.getValue());
        final CompletableFuture<T> newPromise = new CompletableFuture<>();
        Map.Entry<CompletableFuture<T>, Date> newPromiseEntry = newEntry(newPromise);
        Map.Entry<CompletableFuture<T>, Date> currentEntry = futureStore.accumulateAndGet(newPromiseEntry, (existingEntry, entry) -> {
            if (existingEntry != null && isStillValid.apply(existingEntry)) {
                return existingEntry;
            } else {
                return entry;
            }
        });
        CompletableFuture<T> currentPromise = currentEntry.getKey();
        if(newPromise.equals(currentPromise)) {
            try {
                CompletableFuture<T> realFuture = lazyCreateFunction.get();
                realFuture.whenComplete((response, ex) -> {
                    if (ex != null) {
                        newPromise.completeExceptionally(ex);
                    } else {
                        newPromise.complete(response);
                    }
                });
                return newPromise;
            } catch (Throwable e) {
                newPromise.completeExceptionally(e);
                return newPromise;
            }
        } else {
            return currentPromise;
        }
    }

}
