package eu.pretix.pretixdroid.api;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import eu.pretix.pretixdroid.storage.TicketProvider;

public class PretixApi {
    public static final String PREFS_NAME = "pretix";

    public static boolean downloadPretixData(Context ctx, String url, String key) throws ApiException {
        URL urlObject = null;
        try {
            ctx.getContentResolver().delete(TicketProvider.CONTENT_URI, "1=1", null);

            urlObject = new URL(url + "?key=" + key);

            HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.connect();
            int status = urlConnection.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    JSONObject jsonObject = new JSONObject(sb.toString());
                    if (jsonObject.getInt("version") != 1) {
                        throw new ApiException("Invalid server version: " + jsonObject.getInt("version"));
                    }

                    for (int i = 0; i < jsonObject.getJSONArray("data").length(); i++) {
                        JSONObject jTicket = jsonObject.getJSONArray("data").getJSONObject(i);

                        ContentValues newValues = new ContentValues();
                        newValues.put("id", jTicket.getString("id"));
                        newValues.put("item", jTicket.getString("variation"));
                        newValues.put("variation", jTicket.optString("variation"));
                        newValues.put("attendee_name", jTicket.optString("attendee_name"));
                        ctx.getContentResolver().insert(TicketProvider.CONTENT_URI, newValues);
                    }
                    return true;
                default:
                    throw new ApiException("HTTP status code " + status);
            }
        } catch (MalformedURLException e) {
            Log.e("PretixAPI", "Invalid URL", e);
            throw new ApiException("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            Log.e("PretixAPI", "IO Exception", e);
            throw new ApiException("IO Exception: " + e.getMessage());
        } catch (JSONException e) {
            Log.e("PretixAPI", "Invalid JSON", e);
            throw new ApiException("Invalid JSON: " + e.getMessage());
        }
    }
}
