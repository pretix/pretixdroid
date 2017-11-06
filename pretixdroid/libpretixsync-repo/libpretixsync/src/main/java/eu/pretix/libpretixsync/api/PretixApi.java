package eu.pretix.libpretixsync.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.SSLException;

import eu.pretix.libpretixsync.DummySentryImplementation;
import eu.pretix.libpretixsync.SentryInterface;
import eu.pretix.libpretixsync.config.ConfigStore;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PretixApi {
    /**
     * See https://docs.pretix.eu/en/latest/plugins/pretixdroid.html for API documentation
     */

    public static final int SUPPORTED_API_VERSION = 3;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private String url;
    private String key;
    private int version;
    private OkHttpClient client;
    private SentryInterface sentry;

    public PretixApi(String url, String key, int version, HttpClientFactory httpClientFactory) {
        this.url = url;
        this.key = key;
        this.version = version;
        this.client = httpClientFactory.buildClient();
        this.sentry = new DummySentryImplementation();
    }

    public static PretixApi fromConfig(ConfigStore config, HttpClientFactory httpClientFactory) {
        return new PretixApi(config.getApiUrl(), config.getApiKey(), config.getApiVersion(),
                httpClientFactory);
    }

    public static PretixApi fromConfig(ConfigStore config) {
        return PretixApi.fromConfig(config, new DefaultHttpClientFactory());
    }

    public JSONObject redeem(String secret) throws ApiException {
        return redeem(secret, null, false, null);
    }

    public JSONObject redeem(String secret, Date datetime, boolean force, String nonce) throws ApiException {
        FormBody.Builder body = new FormBody.Builder()
                .add("secret", secret);
        if (datetime != null) {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            body.add("datetime", df.format(datetime));
        }
        if (force) {
            body.add("force", "true");
        }
        if (nonce != null) {
            body.add("nonce", nonce);
        }
        Request request = new Request.Builder()
                .url(url + "redeem/?key=" + key)
                .post(body.build())
                .build();
        return apiCall(request);
    }

    public JSONObject status() throws ApiException {
        Request request = new Request.Builder()
                .url(url + "status/?key=" + key)
                .get()
                .build();
        return apiCall(request);
    }

    public JSONObject search(String query) throws ApiException {
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(url + "search/?key=" + key + "&query=" + URLEncoder.encode(query, "UTF-8"))
                    .get()
                    .build();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return apiCall(request);
    }

    public JSONObject download() throws ApiException {
        if (version < 3) {
            throw new ApiException("Unsupported in API versions lower than 3.");
        }
        Request request = new Request.Builder()
                .url(url + "download/?key=" + key)
                .get()
                .build();
        return apiCall(request);
    }

    public SentryInterface getSentry() {
        return sentry;
    }

    public void setSentry(SentryInterface sentry) {
        this.sentry = sentry;
    }

    private JSONObject apiCall(Request request) throws ApiException {
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (SSLException e) {
            e.printStackTrace();
            throw new ApiException("Error while creating a secure connection.", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApiException("Connection error.", e);
        }

        String safe_url = request.url().toString().replaceAll("^(.*)key=([0-9A-Za-z]+)([^0-9A-Za-z]*)", "$1key=redacted$3");
        sentry.addHttpBreadcrumb(safe_url, request.method(), response.code());

        if (response.code() >= 500) {
            throw new ApiException("Server error.");
        } else if (response.code() == 404) {
            throw new ApiException("Invalid configuration, please reset and reconfigure.");
        } else if (response.code() == 403) {
            throw new ApiException("Permission error, please try again or reset and reconfigure.");
        }
        try {
            return new JSONObject(response.body().string());
        } catch (JSONException e) {
            e.printStackTrace();
            sentry.captureException(e);
            throw new ApiException("Invalid JSON received.", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApiException("Connection error.", e);
        }
    }

}
