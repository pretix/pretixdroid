package eu.pretix.pretixdroid.net.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.net.ssl.SSLException;

import eu.pretix.pretixdroid.AppConfig;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PretixApi {
    public static final int API_VERSION = 2;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private String url;
    private String key;

    public PretixApi(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public static PretixApi fromConfig(AppConfig config) {
        return new PretixApi(config.getApiUrl(), config.getApiKey());
    }

    public JSONObject redeem(String secret) throws ApiException {
        RequestBody body = new FormBody.Builder()
                .add("secret", secret)
                .build();
        Request request = new Request.Builder()
                .url(url + "redeem/?key=" + key)
                .post(body)
                .build();
        return apiCall(request);
    }

    public JSONObject search(String query) throws ApiException {
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(url + "redeem/?key=" + key + "&query=" + URLEncoder.encode(query, "UTF-8"))
                    .get()
                    .build();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return apiCall(request);
    }

    private JSONObject apiCall(Request request) throws ApiException {
        OkHttpClient client = new OkHttpClient();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (SSLException e) {
            e.printStackTrace();
            throw new ApiException("Error while creating a secure connection.");
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApiException("Connection error.");
        }
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
            throw new ApiException("Invalid JSON received.");
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApiException("Connection error.");
        }
    }

}
