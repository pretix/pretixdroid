package eu.pretix.libpretixsync;


public class DummySentryImplementation implements SentryInterface {
    @Override
    public void addHttpBreadcrumb(String url, String method, int statusCode) {

    }

    @Override
    public void addBreadcrumb(String a, String b) {

    }

    @Override
    public void captureException(Throwable t) {

    }

    @Override
    public void captureException(Throwable t, String message) {

    }
}
