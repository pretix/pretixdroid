package eu.pretix.pretixdroid.ui;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.net.api.ApiException;
import eu.pretix.pretixdroid.net.api.PretixApi;

/**
 * This class is the activity for the Eventinfo page to let the user see statistics about their
 * event.
 * @author jfwiebe
 */
public class EventinfoActivity extends AppCompatActivity {
    private AppConfig config;
    private PretixApi api;

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventinfo);

        this.mListView = (ListView)findViewById(R.id.eventinfo_list);
        this.mListView.setAdapter(new EventItemAdapter(getApplicationContext()));

        this.config = new AppConfig(getApplicationContext());
        this.api = PretixApi.fromConfig(config);

        new StatusTask().execute();
    }

    public class StatusTask extends AsyncTask<String, Integer, JSONObject> {

        /**
         * exexutes an asyncron request to obtain status information from the pretix instance
         * @param params are ignored
         * @return the associated json object recieved from the pretix-endpoint or
         * @return null if the request was not successful
         */
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                return EventinfoActivity.this.api.status();
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * it parses the answer of the pretix endpoint into objects and adds them to the list
         * @param result the answer of the pretix status endpoint
         */
        @Override
        protected void onPostExecute(JSONObject result) {
            TextView testTextView = (TextView)findViewById(R.id.testTextView);
            try {
                testTextView.setText(result.getJSONObject("event").getString("name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                JSONArray items = result.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    // TODO parse json arrays to EventItemCardItems and add it to the adapter
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * implementation of an adapter for a listview to hold EventCards and EventItemCards
     */
    public class EventItemAdapter extends BaseAdapter {
        public static final int TYPE_EVENTCARD = 0;
        public static final int TYPE_EVENTITEMCARD = 1;
        private static final int MAX_TYPES = 2;

        private final ArrayList<EventinfoListItem> mData = new ArrayList<>();
        public final LayoutInflater mInflater;

        public EventItemAdapter(Context ctx) {
            mInflater = LayoutInflater.from(ctx);
        }

        public void addItem(EventinfoListItem item) {
            this.mData.add(item);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return this.mData.get(position).getType();
        }

        @Override
        public int getViewTypeCount() {
            return MAX_TYPES;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public EventinfoListItem getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            if (convertView == null) {
                EventinfoListItem item = this.getItem(position);
                return item.getCard(mInflater, parent);
            } else {
                ((EventinfoListItem) convertView.getTag()).fillView(convertView);
                return convertView;
            }

        }

    }

    /**
     * interface of all CardItems that are displayed on the eventinfo page
     */
    public interface EventinfoListItem {
        /**
         * returns an integer for the adapter to distinguish between cards
         * @return an integer for the adapter to distinguish between cards
         */
        int getType();

        /**
         * returns a newly inflated card with the content of this item
         * @param inflater the inflater to use
         * @param parent the parent ViewGroup
         * @return a newly inflated card with the content of this item
         */
        View getCard(LayoutInflater inflater, ViewGroup parent);

        /**
         * returns a recycled view filled with the contents of this item
         * @param view a recycled view filled with the contents of this item
         */
        void fillView(View view);
    }

    /**
     * is the handler of a card that displays basic information about the event
     */
    public class EventCardItem implements EventinfoListItem {
        @Override
        public int getType() {
            return EventItemAdapter.TYPE_EVENTCARD;
        }

        @Override
        public View getCard(LayoutInflater inflater, ViewGroup parent) {
            View v = inflater.inflate(R.layout.listitem_eventcard, parent, false);
            fillView(v);
            v.setTag(this);
            return v;
        }

        @Override
        public void fillView(View view) {
            // TODO implement filling of a card as an EventCard
        }
    }

    /**
     * is the handler of a card that displays information about each item of an event
     */
    public class EventItemCardItem implements EventinfoListItem {
        @Override
        public int getType() {
            return EventItemAdapter.TYPE_EVENTITEMCARD;
        }

        @Override
        public View getCard(LayoutInflater inflater, ViewGroup parent) {
            View v = inflater.inflate(R.layout.listitem_eventitemcard, parent, false);
            fillView(v);
            v.setTag(this);
            return v;
        }

        @Override
        public void fillView(View view) {
            // TODO implement filling of a card as an EventItemCard
        }
    }
}
