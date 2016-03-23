package fi.vm.sade.authentication.cas;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A cache implementation to keep service and user specific session ids. Currently hardcoded to use memory only for caching.
 * @author Jouni Stam
 *
 */
@Component
//@Singleton
public class CasFriendlyCache {

    private static final Logger log = LoggerFactory.getLogger(CasFriendlyCache.class);
    
    public static final String CACHE_SESSIONS = CasFriendlyCache.class.getName() + "/sessionCache";
    public static final String CAS_SERVICE_KEY = "CAS";
    CacheManager cacheManager;
    private int ttlInSeconds = 3600;
    private int maxWaitTimeSeconds = 3;
    private String distinctiveName = "";

    // Synchronized map of request start times 
    Map<String, Date> runningRequests = new ConcurrentHashMap<String, Date>();

    public CasFriendlyCache() {
        this(3600, "");
    }

    public CasFriendlyCache(int ttlInSeconds, String distinctiveName) {
        this.ttlInSeconds = ttlInSeconds;
        this.distinctiveName = distinctiveName;
        if(this.distinctiveName == null)
            this.distinctiveName = "";
    }

    /**
     * Clears all from cache.
     */
    public void clearAll() {
        this.getCacheManager().clearAll();
    }
    
    public long getSize() {
        return this.getSessionCache().getSize();
    }
    
    /**
     * Sets sessionId given by CAS to cache.
     * @param serviceUrl
     * @param userName
     * @param sessionId
     */
    public void setCASSessionId(String callerService, String userName, String sessionId) {
        // Sets sessionId given by CAS, uses "CAS" as the distinctive key
        this.setSessionId(callerService, CAS_SERVICE_KEY, userName, sessionId);
    }

    /**
     * Gets sessionId given by CAS from cache.
     * @param serviceUrl
     * @param userName
     * @return
     */
    public String getCASSessionId(String callerService, String userName) {
        return this.getSessionId(callerService, CAS_SERVICE_KEY, userName);
    }

    /**
     * Removes CAS sessionId from cache.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     */
    public void removeCASSessionId(String callerService, String userName) {
        this.removeSessionId(callerService, CAS_SERVICE_KEY, userName);
    }

    /**
     * Sets sessionId for caching.
     * @param serviceUrl
     * @param userName
     * @param sessionId
     */
    public void setSessionId(String callerService, String targetServiceUrl, String userName, String sessionId) {
        if(StringUtils.isEmpty(callerService) || 
                StringUtils.isEmpty(targetServiceUrl) || 
                StringUtils.isEmpty(userName) || 
                StringUtils.isEmpty(sessionId))
            return;
        String key = createKey(callerService, targetServiceUrl, userName);
        Element element = new Element(key, sessionId);
        // Make sure request is released
        this.releaseRequest(callerService, targetServiceUrl, userName);
        // Put to session cache
        this.getSessionCache().put(element);
    }

    /**
     * Gets sessionId from cache.
     * @param serviceUrl
     * @param userName
     * @return
     */
    public String getSessionId(String callerService, String targetServiceUrl, String userName) {
        if(StringUtils.isEmpty(callerService) || StringUtils.isEmpty(targetServiceUrl) || StringUtils.isEmpty(userName))
            return null;
        Element element = this.getSessionCache().get(createKey(callerService, targetServiceUrl, userName));
        if(element != null) {
            return (String)element.getObjectValue();
        } else
            return null;
    }

    /**
     * Removes from cache.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     */
    public void removeSessionId(String callerService, String targetServiceUrl, String userName) {
        this.getSessionCache().remove(createKey(callerService, targetServiceUrl, userName));
    }

    /**
     * Creates a key for caching.
     * @param serviceUrl
     * @param userName
     * @return
     */
    private static String createKey(String callerService, String targetServiceUrl, String userName) {
        // Key does not include more than folder of the URL
        return callerService + ":" + userName + "@" + StringUtils.substringBeforeLast(targetServiceUrl, "/") + "/";
    }

    /**
     * Waits for another one to complete or flags itself as the requestor that others should wait for.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     * @param millis
     * @param lock Lock request if no concurrent request.
     */
    public void waitOrFlagForRunningRequest(String callerService, String targetServiceUrl, String userName, long millis, boolean lock) {
        String key = createKey(callerService, targetServiceUrl, userName);
        // Waits if concurrent, otherwise locks (if lock true)
        if(this.isConcurrentRequest(callerService, targetServiceUrl, userName, lock)) {
            log.debug("Waiting: " + callerService + ": " + targetServiceUrl + ": " + userName);
            InterruptingCacheEventListener icel = new InterruptingCacheEventListener(Thread.currentThread(), key);
            this.getSessionCache().getCacheEventNotificationService().registerListener(icel);			
            try {
                Thread.sleep(millis);
            } catch(InterruptedException iex) {
                // Ignore as okay
            } finally {
                this.getSessionCache().getCacheEventNotificationService().unregisterListener(icel);
            }
        }
    }

    /**
     * Checks if there is another thread registered for the same authentication request.
     * Synchronized on key level.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     * @return
     */
    public boolean isConcurrentRequest(String callerService, String targetServiceUrl, String userName, boolean lock) {
        String key = createKey(callerService, targetServiceUrl, userName);
        synchronized(key) {
            Date val = this.runningRequests.get(key);
            if(val != null) {
                long now = new Date().getTime();
                if(val.after(new Date(now - (this.getMaxWaitTimeSeconds() * 1000)))) {
                    return true;
                } else {
                    // Clean
                    this.runningRequests.remove(key);
                    return false;
                }
            } else {
                if(lock)
                    this.lockRequest(callerService, targetServiceUrl, userName);
                return false;
            }
        }
    }

    /**
     * Locks the request so that subsequential requests will be blocked if needed.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     */
    public void lockRequest(String callerService, String targetServiceUrl, String userName) {
        String key = createKey(callerService, targetServiceUrl, userName);
        this.runningRequests.put(key, new Date());
        log.debug("Locked: " + key);
    }

    /**
     * Releases the request for others.
     * @param callerService
     * @param targetServiceUrl
     * @param userName
     */
    public void releaseRequest(String callerService, String targetServiceUrl, String userName) {
        String key = createKey(callerService, targetServiceUrl, userName);
        this.runningRequests.remove(key);
        log.debug("Released: " + key);
    }

    /**
     * Gets session cache. Creates if not available.
     * @return
     */
    protected Cache getSessionCache() {
        if(!this.getCacheManager().cacheExists(CACHE_SESSIONS + "/" + distinctiveName)) {
            // This is where cache is configured, currently memory only, can be configured from xml as well with minor changes
            // Cache(String name, int maxElementsInMemory, boolean overflowToDisk, boolean eternal, long timeToLiveSeconds, long timeToIdleSeconds) 
            Cache memoryOnlyCache = new Cache(CACHE_SESSIONS + "/" + distinctiveName, 50000, false, false, ttlInSeconds, ttlInSeconds);

            cacheManager.addCacheIfAbsent(memoryOnlyCache);
        }
        return cacheManager.getCache(CACHE_SESSIONS + "/" + distinctiveName);
    }

    /**
     * Gets the cache manager. Creates if not available.
     * @return
     */
    private CacheManager getCacheManager() {
        if(cacheManager == null) 
            cacheManager = CacheManager.create();
        return cacheManager;
    }

    /**
     * Time to live in seconds for cached sessions. 3600 seconds (one hour) by default.
     * @return
     */
    public int getTtlInSeconds() {
        return ttlInSeconds;
    }

    public void setTtlInSeconds(int ttlInSeconds) {
        this.ttlInSeconds = ttlInSeconds;
    }

    /**
     * Maximum wait time for running requests in seconds. 3 seconds by default.
     * @return
     */
    public int getMaxWaitTimeSeconds() {
        return maxWaitTimeSeconds;
    }

    public void setMaxWaitTimeSeconds(int maxWaitTimeSeconds) {
        this.maxWaitTimeSeconds = maxWaitTimeSeconds;
    }

    /**
     * Number of active distinctive requests marked.
     * @return
     */
    public int getActiveCount() {
        return this.runningRequests.size();
    }
    
    class InterruptingCacheEventListener implements CacheEventListener {

        Thread t;
        String key;
        boolean interrupting;

        public InterruptingCacheEventListener(Thread t, String key) {
            this.t = t;
            this.key = key;
        }

        @Override
        public void notifyRemoveAll(Ehcache event) {
            if(!t.isInterrupted())
                t.interrupt();
        }

        @Override
        public void notifyElementUpdated(Ehcache ehCache, Element element)
                throws CacheException {
            if(element.getObjectKey().equals(key) && !t.isInterrupted())
                t.interrupt();
        }

        @Override
        public void notifyElementRemoved(Ehcache ehCache, Element element)
                throws CacheException {
            if(element.getObjectKey().equals(key) && !t.isInterrupted())
                t.interrupt();
        }

        @Override
        public void notifyElementPut(Ehcache ehCache, Element element)
                throws CacheException {
            if(element.getObjectKey().equals(key) && !t.isInterrupted())
                t.interrupt();
        }

        @Override
        public void notifyElementExpired(Ehcache arg0, Element arg1) {
            // Ignore
        }

        @Override
        public void notifyElementEvicted(Ehcache arg0, Element arg1) {
            // Ignore
        }

        @Override
        public void dispose() {
            // Ignore
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public void setInterrupting(boolean interrupting) {
            this.interrupting = interrupting;
        }

    };

}


