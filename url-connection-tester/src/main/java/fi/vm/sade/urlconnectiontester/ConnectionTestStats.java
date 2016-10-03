package fi.vm.sade.urlconnectiontester;

import java.util.ArrayList;
import java.util.List;

public class ConnectionTestStats {
    final long starttime;
    long count = 0;
    long totalLength = 0;
    long meanLength = 0;
    long maxLength = 0;
    final List<Exception> errors = new ArrayList<>();

    public ConnectionTestStats() {
        this.starttime = System.currentTimeMillis();
    }

    public ConnectionTestStats update(ConnectionStats stats) {
        this.count++;
        this.totalLength += stats.length;
        this.meanLength = totalLength / count;
        if(stats.length > this.maxLength) {
            this.maxLength = stats.length;
        }
        if(stats.error != null) {
            errors.add(stats.error);
        }
        return this;
    }

    public String toString() {
        return "stats of " + count + " requests: mean length: " +  meanLength + "ms, max length: " + maxLength / 1000.0 + "s, errors: " + errors.size() + " kpl";
    }
}
