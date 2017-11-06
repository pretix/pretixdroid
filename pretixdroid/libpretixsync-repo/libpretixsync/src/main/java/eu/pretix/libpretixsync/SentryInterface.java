package eu.pretix.libpretixsync;

import okhttp3.OkHttpClient;

public interface SentryInterface {
    public void addHttpBreadcrumb(String url, String method, int statusCode);

    public void addBreadcrumb(String a, String b);

    public void captureException(Throwable t);

    public void captureException(Throwable t, String message);
}
