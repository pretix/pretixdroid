package eu.pretix.pretixdroid;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import eu.pretix.libpretixsync.api.HttpClientFactory;
import okhttp3.OkHttpClient;

public class AndroidHttpClientFactory implements HttpClientFactory {
    @Override
    public OkHttpClient buildClient() {
        if (BuildConfig.DEBUG) {
            return new OkHttpClient.Builder()
                    .addNetworkInterceptor(new StethoInterceptor())
                    .build();
        } else {
            return new OkHttpClient.Builder()
                    .build();
        }
    }
}
