package eu.pretix.pretixdroid.ui;

import android.app.usage.UsageEvents;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.TicketCheckProvider;
import eu.pretix.pretixdroid.net.api.ApiException;
import eu.pretix.pretixdroid.net.api.PretixApi;

public class EventinfoActivity extends AppCompatActivity {
    private AppConfig config;
    private PretixApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventinfo);

        this.config = new AppConfig(getApplicationContext());
        this.api = PretixApi.fromConfig(config);

        new StatusTask().execute();
    }

    public class StatusTask extends AsyncTask<String, Integer, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                return EventinfoActivity.this.api.status();
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            TextView testTextView = (TextView)findViewById(R.id.testTextView);
            try {
                testTextView.setText(result.getJSONObject("event").getString("name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
