package fi.vm.sade.urlconnectiontester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class Tester {

    private static final Logger logger = LoggerFactory.getLogger(Tester.class);

    static class SlowRequestAlerter implements Runnable {
        public long currentTime = 0;
        public final long slowTresholdSecs;

        SlowRequestAlerter(long slowTresholdSecs) {
            this.slowTresholdSecs = slowTresholdSecs;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.currentThread().sleep(slowTresholdSecs / 2);
                    currentTime += slowTresholdSecs / 2;
                    if(currentTime >= slowTresholdSecs) {
                        logger.warn("Slow request underway: allready {} seconds", currentTime / 1000.0);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws MalformedURLException {
        if(args.length == 0) {
            throw new IllegalArgumentException("Give destination URL as first parameter");
        }
        URL url =  new URL(args[0]);
        int iterations = args.length > 1 ?  Integer.parseInt(args[1]) : 1;
        int slowTreshold = args.length > 2 ?  Integer.parseInt(args[2]) : 2;
        testConnection(url, iterations, slowTreshold, true);
    }

    private static ConnectionTestStats testConnection(URL url, long iterations, long slowTresholdSecs, boolean logOnSystemExit) {
        logger.info("Calling {} {} times", url, iterations);
        ConnectionTestStats totalStats = new ConnectionTestStats();
        ConnectionTestStats slowStats = new ConnectionTestStats();
        if(logOnSystemExit) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logStats(url, slowTresholdSecs, totalStats, slowStats);
            }));
        }
        int requestCount = 0;
        // warm up once
        ConnectionStats stats = testConnection(url);
        logger.info("Warm up request done. {}", stats);
        SlowRequestAlerter slowRequestAlerter = new SlowRequestAlerter(slowTresholdSecs * 1000);
        Thread slowRequestAlerterThread = new Thread(slowRequestAlerter);
        slowRequestAlerterThread.start();
        while (requestCount < iterations) {
            stats = testConnection(url);
            totalStats.update(stats);
            slowRequestAlerter.currentTime = 0;
            requestCount++;
            if (stats.length > slowTresholdSecs * 1000) {
                slowStats.update(stats);
                logger.warn("Slow request with {}",  stats);
            } else if (requestCount % 100 == 0) {
                logger.info("Request {}/{} done. Mean {}. Last {}", requestCount, iterations, totalStats, stats);
            }
        }
        slowRequestAlerterThread.interrupt();
        if(!logOnSystemExit) {
            logStats(url, slowTresholdSecs, totalStats, slowStats);
        }
        return totalStats;
    }

    private static void logStats(URL url, long slowTresholdSecs, ConnectionTestStats totalStats, ConnectionTestStats slowStats) {
        logger.info("Url {} connections mean {}", url, totalStats);
        if(slowStats.count == 0) {
            logger.info("No queries took over {} seconds", slowTresholdSecs);
        } else {
            logger.warn("Slow query {}", slowStats);
        }
    }

    private static ConnectionStats testConnection(URL url) {
        long starttime = System.currentTimeMillis();
        final StringBuilder builder = new StringBuilder(255);
        Exception e = null;
        InputStreamReader in = null;
        try {
            in = new InputStreamReader(url.openConnection().getInputStream());
            int byteRead;
            while ((byteRead = in.read()) != -1) {
                builder.append((char) byteRead);
            }
            // CAS client version
            //builder.append(CommonUtils.getResponseFromServer(url, null));
        } catch (Exception ie) {
            e = ie;
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (Exception i) {
                    //IGNORE}
                }
            }
        }
        return new ConnectionStats(starttime, System.currentTimeMillis() - starttime, builder.length(), e);
    }
}