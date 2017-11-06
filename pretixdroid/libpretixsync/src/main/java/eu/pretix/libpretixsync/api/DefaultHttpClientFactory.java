package eu.pretix.libpretixsync.api;

import okhttp3.OkHttpClient;

public class DefaultHttpClientFactory implements HttpClientFactory {
    @Override
    public OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .build();
    }
}
