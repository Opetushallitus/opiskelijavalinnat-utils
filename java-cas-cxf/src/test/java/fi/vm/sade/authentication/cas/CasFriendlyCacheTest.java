package fi.vm.sade.authentication.cas;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.vm.sade.authentication.cas.CasFriendlyCache;

public class CasFriendlyCacheTest {

    CasFriendlyCache cache = null;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // Default TTL
        cache = new CasFriendlyCache();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testMain() throws Exception {
        Assert.assertTrue(true);
    }

    @Test
    public void testAddToCache() {
        String callerService = "any";
        String targetUrl = "https://localhost:8443/organisaatio-ui";
        String userName = "test";
        String id = "testId";
        this.cache.setSessionId(callerService, targetUrl, userName, id);
        String tId = this.cache.getSessionId(callerService, targetUrl, userName);
        CasFriendlyCache cache2 = new CasFriendlyCache();
        String tId2 = cache2.getSessionId(callerService, targetUrl, userName);
        Assert.assertEquals(id, tId);
        Assert.assertEquals(id, tId2);
    }

    @Test
    public void testRemoveFromCache() {
        String callerService = "any";
        String targetUrl = "https://localhost:8443/organisaatio-ui";
        String userName = "test";
        String id = "testId";
        this.cache.setSessionId(callerService, targetUrl, userName, id);
        String tId = this.cache.getSessionId(callerService, targetUrl, userName);
        Assert.assertEquals(id, tId);
        this.cache.removeSessionId(callerService, targetUrl, userName);
        tId = this.cache.getSessionId(callerService, targetUrl, userName);
        Assert.assertNull(tId);
    }

    @Test
    public void testExpiration() {
        CasFriendlyCache shortCache = new CasFriendlyCache(1, "short");
        String callerService = "any";
        String targetUrl = "https://localhost:8443/organisaatio-ui";
        String userName = "test_short";
        String id = "testId";
        shortCache.setSessionId(callerService, targetUrl, userName, id);
        try { 
            Thread.sleep(2000);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        String tId = shortCache.getSessionId(callerService, targetUrl, userName);
        Assert.assertNull(tId);
    }

    /**
     * Trivial tests for concurrent access with same key.
     */
    @Test
    public void testConcurrentRequests() {
        final String callerService = "any";
        final String targetUrl = "https://localhost:8443/organisaatio-ui";
        final String userName = "test";
        final String id = "testId";

        Thread t1 = new Thread() {
            public void run() {
                cache.waitOrFlagForRunningRequest(callerService, targetUrl, userName, 10000, true);
                try { Thread.sleep(500); } catch(Exception ex) {}
                cache.releaseRequest(callerService, targetUrl, userName);
            }
        };

        Thread t2 = new Thread() {
            public void run() {
                cache.waitOrFlagForRunningRequest(callerService, targetUrl, userName, 10000, true);
                cache.releaseRequest(callerService, targetUrl, userName);
            }
        };

        Thread t3 = new Thread() {
            public void run() {
                cache.waitOrFlagForRunningRequest(callerService, targetUrl, userName, 10000, true);
                cache.releaseRequest(callerService, targetUrl, userName);
            }
        };

        Assert.assertTrue("Active count must be == 0 after release.", cache.getActiveCount() == 0);
        cache.waitOrFlagForRunningRequest(callerService, targetUrl, userName, 1000, true);
        Assert.assertTrue("Active count must be > 0 before release.", cache.getActiveCount() > 0);
        cache.waitOrFlagForRunningRequest(callerService, targetUrl, userName, 100, true);
        cache.releaseRequest(callerService, targetUrl, userName);
        Assert.assertTrue("Active count must be == 0 after release.", cache.getActiveCount() == 0);

        // Test with threads
//        t1.start();
//        try { Thread.sleep(100); } catch(Exception ex) {}
//        t2.start();
//        t3.start();
//        Assert.assertTrue("Active count must be > 0 before release.", cache.getActiveCount() > 0);
//        cache.releaseRequest(callerService, targetUrl, userName);

        // Set sessionId
        cache.setSessionId(callerService, targetUrl, userName, id);

        try {
            t1.join();
            t2.join();
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

}
