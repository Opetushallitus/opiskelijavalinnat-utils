package fi.vm.sade.urlconnectiontester;

public class ConnectionStats {
    final long starttime;
    final long length;
    final long responseSize;
    final Exception error;

    public ConnectionStats(long starttime, long length, long responseSize, Exception error) {
        this.starttime = starttime;
        this.length = length;
        this.responseSize = responseSize;
        this.error = error;
    }

    public String toString() {
        return "request stats: length: " +  length + "ms, response size: " + responseSize + " chars" + (error == null ? "" : ", error:" + error.toString());
    }
}
