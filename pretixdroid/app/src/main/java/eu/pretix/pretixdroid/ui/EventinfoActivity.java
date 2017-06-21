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
import java.util.Iterator;
import java.util.LinkedList;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.net.api.ApiException;
import eu.pretix.pretixdroid.net.api.PretixApi;

/**
 * This class is the activity for the Eventinfo page to let the user see statistics about their
 * event.
 *
 * @author jfwiebe
 */
public class EventinfoActivity extends AppCompatActivity {
    private AppConfig config;
    private PretixApi api;

    private ListView mListView;
    private EventItemAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventinfo);

        this.mListView = (ListView) findViewById(R.id.eventinfo_list);
        this.mAdapter = new EventItemAdapter(getBaseContext());
        this.mListView.setAdapter(this.mAdapter);

        this.config = new AppConfig(this);
        this.api = PretixApi.fromConfig(config);

        new StatusTask().execute();
    }

    public class StatusTask extends AsyncTask<String, Integer, JSONObject> {

        /**
         * exexutes an asyncron request to obtain status information from the pretix instance
         *
         * @param params are ignored
         * @return the associated json object recieved from the pretix-endpoint or null if the request was not successful
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
         *
         * @param result the answer of the pretix status endpoint
         */
        @Override
        protected void onPostExecute(JSONObject result) {
            EventItemAdapter eia = EventinfoActivity.this.mAdapter;
            eia.clear();
            try {
                EventCardItem ici = new EventCardItem(result);
                eia.addItem(ici);

                JSONArray items = result.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    EventItemCardItem eici = new EventItemCardItem(items.getJSONObject(i));
                    eia.addItem(eici);
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

        public void clear() {
            this.mData.clear();
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
                ((EventinfoListItem) convertView.getTag()).fillView(convertView, mInflater, parent);
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
         *
         * @return an integer for the adapter to distinguish between cards
         */
        int getType();

        /**
         * returns a newly inflated card with the content of this item
         *
         * @param inflater the inflater to use
         * @param parent   the parent ViewGroup
         * @return a newly inflated card with the content of this item
         */
        View getCard(LayoutInflater inflater, ViewGroup parent);

        /**
         * returns a recycled view filled with the contents of this item
         *
         * @param view a recycled view filled with the contents of this item
         */
        void fillView(View view, LayoutInflater inflater, ViewGroup parent);
    }

    /**
     * is the handler of a card that displays basic information about the event
     */
    public class EventCardItem implements EventinfoListItem {

        private final String eventName;
        private final int totalTickets;
        private final int alreadyScanned;

        EventCardItem(String name, int totalTickets, int alreadyScanned) {
            this.eventName = name;
            this.totalTickets = totalTickets;
            this.alreadyScanned = alreadyScanned;
        }

        EventCardItem(JSONObject json) throws JSONException {
            eventName = json.getJSONObject("event").getString("name");
            totalTickets = json.getInt("total");
            alreadyScanned = json.getInt("checkins");
        }

        public String getEventName() {
            return eventName;
        }

        public int getTotalTickets() {
            return totalTickets;
        }

        public int getAlreadyScanned() {
            return alreadyScanned;
        }

        // --- used for the adapter --- //
        @Override
        public int getType() {
            return EventItemAdapter.TYPE_EVENTCARD;
        }

        @Override
        public View getCard(LayoutInflater inflater, ViewGroup parent) {
            View v = inflater.inflate(R.layout.listitem_eventcard, parent, false);
            fillView(v, inflater, parent);
            v.setTag(this);
            return v;
        }

        @Override
        public void fillView(View view, LayoutInflater inflater, ViewGroup parent) {
            ((TextView) view.findViewById(R.id.eventTitle)).setText(this.getEventName());
            ((TextView) view.findViewById(R.id.tickets_sold)).setText(String.valueOf(this.getTotalTickets()));
            ((TextView) view.findViewById(R.id.total_scanned)).setText(String.valueOf((this.getAlreadyScanned())));
        }

    }

    /**
     * is the handler of a card that displays information about each item of an event
     */
    public class EventItemCardItem implements EventinfoListItem {

        private final String name;
        private final int checkins;
        private final int total;
        private final int variationCount;

        private final LinkedList<Variation> variations = new LinkedList<>();

        EventItemCardItem(JSONObject json) throws JSONException {
            this.name = json.getString("name");
            this.checkins = json.getInt("checkins");
            this.total = json.getInt("total");

            JSONArray vars = json.getJSONArray("variations");
            this.variationCount = vars.length();

            for (int i = 0; i < this.variationCount; i++) {
                this.variations.add(new Variation(vars.getJSONObject(i)));
            }
        }

        public String getName() {
            return name;
        }

        public int getCheckins() {
            return checkins;
        }

        public int getTotal() {
            return total;
        }

        public int getVariationCount() {
            return variationCount;
        }

        public LinkedList<Variation> getVariations() {
            return variations;
        }

        private class Variation {
            private final String name;
            private final int checkins;
            private final int total;

            public Variation(JSONObject json) throws JSONException {
                this.name = json.getString("name");
                this.checkins = json.getInt("checkins");
                this.total = json.getInt("total");
            }

            public String getName() {
                return name;
            }

            public int getCheckins() {
                return checkins;
            }

            public int getTotal() {
                return total;
            }
        }

        // --- used for the adapter --- //
        @Override
        public int getType() {
            return EventItemAdapter.TYPE_EVENTITEMCARD;
        }

        @Override
        public View getCard(LayoutInflater inflater, ViewGroup parent) {
            View v = inflater.inflate(R.layout.listitem_eventitemcard, parent, false);
            fillView(v, inflater, parent);
            v.setTag(this);
            return v;
        }

        @Override
        public void fillView(View view, LayoutInflater inflater, ViewGroup parent) {
            ((TextView) view.findViewById(R.id.itemTitle)).setText(this.getName());
            ((TextView) view.findViewById(R.id.itemQuantity)).setText(String.valueOf(this.getCheckins()) + "/" + String.valueOf(this.getTotal()));

            Iterator<Variation> iterator = this.variations.iterator();
            while (iterator.hasNext()) {
                Variation current = iterator.next();

                View variationLine = inflater.inflate(R.layout.listitem_eventitemvariation, parent, false);
                ((TextView) variationLine.findViewById(R.id.itemVariationTitle)).setText(current.getName());
                ((TextView) variationLine.findViewById(R.id.itemVariationQuantity)).setText(String.valueOf(current.getCheckins()) + "/" + String.valueOf(current.getTotal()));

                ViewGroup variationList = ((ViewGroup) view.findViewById(R.id.variationList));

                variationList.removeAllViews();
                variationList.addView(variationLine);
            }
        }
    }
}
