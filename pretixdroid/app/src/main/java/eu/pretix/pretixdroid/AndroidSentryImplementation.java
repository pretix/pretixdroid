package eu.pretix.pretixdroid;

import com.joshdholtz.sentry.Sentry;

import eu.pretix.libpretixsync.SentryInterface;


public class AndroidSentryImplementation implements SentryInterface {
    @Override
    public void addHttpBreadcrumb(String url, String method, int statusCode) {
        Sentry.addHttpBreadcrumb(url, method, statusCode);
    }

    @Override
    public void addBreadcrumb(String a, String b) {
        Sentry.addBreadcrumb(a, b);
    }

    @Override
    public void captureException(Throwable t) {
        Sentry.captureException(t);
    }

    @Override
    public void captureException(Throwable t, String message) {
        Sentry.captureException(t, message);
    }
}
