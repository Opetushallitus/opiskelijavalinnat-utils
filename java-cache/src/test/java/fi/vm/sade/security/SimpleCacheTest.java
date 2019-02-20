package fi.vm.sade.security;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SimpleCacheTest {

    int threadsDone = 0;
    int threads = 10;
    int maxCacheSize = 10000;
    Map<String, List<String>> cache = SimpleCache.<String, List<String>>buildCache(maxCacheSize);

    @Test
    public void testCache() throws InterruptedException {
        // add 1 entry
        cache.put("key_first", Arrays.asList("key_first"));
        // add MAX entries in threads
        for (int t = 0; t < threads; t++) {
            final int finalT = t;
            new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < maxCacheSize / threads; i++) {
                        String key = "key_" + (finalT * maxCacheSize / threads + i);
                        cache.put(key, Arrays.asList(key));
                    }
                    threadsDone++;
                }
            }.start();
        }
        while (true) {
            Thread.sleep(100);
            if (threadsDone == threads) break;
        }
        // assert cache - first entry must be evicted
        Assert.assertTrue(cache.containsKey("key_0"));
        Assert.assertTrue(cache.containsKey("key_"+(9999)));
        Assert.assertFalse(cache.containsKey("key_first"));
    }

}
