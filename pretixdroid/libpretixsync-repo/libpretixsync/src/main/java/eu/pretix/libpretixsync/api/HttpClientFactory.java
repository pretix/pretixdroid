package eu.pretix.libpretixsync.api;

import okhttp3.OkHttpClient;

public interface HttpClientFactory {
    public OkHttpClient buildClient();
}
