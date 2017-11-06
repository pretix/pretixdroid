package eu.pretix.libpretixsync.config;

public interface ConfigStore {

    public boolean isDebug();

    public boolean isConfigured();

    public int getApiVersion();

    public String getApiUrl();

    public String getApiKey();

    public boolean getShowInfo();

    public boolean getAllowSearch();

    public String getLastStatusData();

    public long getLastDownload();

    public void setLastDownload(long val);

    public long getLastSync();

    public void setLastSync(long val);

    public long getLastFailedSync();

    public void setLastFailedSync(long val);

    public String getLastFailedSyncMsg();

    public void setLastFailedSyncMsg(String val);
}
