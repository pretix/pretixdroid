package eu.pretix.pretixdroid.ui;

import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.CheckException;
import eu.pretix.pretixdroid.check.TicketCheckProvider;
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

    private ListView mListView;
    private EventItemAdapter mAdapter;
    private TicketCheckProvider checkProvider;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventinfo);

        this.mListView = (ListView) findViewById(R.id.eventinfo_list);
        this.mAdapter = new EventItemAdapter(getBaseContext());
        this.mListView.setAdapter(this.mAdapter);

        this.mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        this.mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                new StatusTask().execute();
            }
        });

        this.config = new AppConfig(this);
        this.checkProvider = ((PretixDroid) getApplication()).getNewCheckProvider();

        new StatusTask().execute();
    }

    public class StatusTask extends AsyncTask<String, Integer, TicketCheckProvider.StatusResult> {

        Exception e;

        /**
         * exexutes an asyncron request to obtain status information from the pretix instance
         *
         * @param params are ignored
         * @return the associated json object recieved from the pretix-endpoint or null if the request was not successful
         */
        @Override
        protected TicketCheckProvider.StatusResult doInBackground(String... params) {
            try {
                return EventinfoActivity.this.checkProvider.status();
            } catch (CheckException e) {
                e.printStackTrace();
                this.e = e;
            }
            return null;
        }

        /**
         * it parses the answer of the pretix endpoint into objects and adds them to the list
         *
         * @param result the answer of the pretix status endpoint
         */
        @Override
        protected void onPostExecute(TicketCheckProvider.StatusResult result) {
            EventinfoActivity.this.mSwipeRefreshLayout.setRefreshing(false);
            if (this.e != null) {
                Toast.makeText(EventinfoActivity.this, R.string.no_connection, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(ProgressBar.GONE);
            EventItemAdapter eia = EventinfoActivity.this.mAdapter;
            eia.clear();
            try {
                EventCardItem ici = new EventCardItem(EventinfoActivity.this, result);
                eia.addItem(ici);

                List<TicketCheckProvider.StatusResultItem> items = result.getItems();
                for (TicketCheckProvider.StatusResultItem item : items) {
                    EventItemCardItem eici = new EventItemCardItem(EventinfoActivity.this, item);
                    eia.addItem(eici);
                }
            } catch (JSONException e) {
                Toast.makeText(EventinfoActivity.this, R.string.err_unknown, Toast.LENGTH_LONG).show();
                finish();
                return;
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
                this.getItem(position).fillView(convertView, mInflater, parent);
                return convertView;
            }

        }

    }

}
