package eu.pretix.pretixdroid.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.CheckException;
import eu.pretix.pretixdroid.check.OnlineCheckProvider;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

public class SearchActivity extends AppCompatActivity {

    private int loading = 0;
    private EditText etQuery;
    private ListView lvResults;
    private TicketCheckProvider checkProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkProvider = new OnlineCheckProvider(this);

        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toggleLoadingIndicator();

        lvResults = (ListView) findViewById(R.id.lvResults);
        etQuery = (EditText) findViewById(R.id.etQuery);
        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                startSearch(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void startSearch(String query) {
        if (query.length() < 4) {
            SearchResultAdapter adapter = new SearchResultAdapter(
                    this, R.layout.listitem_searchresult, R.id.tvAttendeeName,
                    new ArrayList<TicketCheckProvider.SearchResult>());
            lvResults.setAdapter(adapter);
            return;
        }
        new SearchTask().execute(query);
    }

    public class SearchTask extends AsyncTask<String, Integer, List<TicketCheckProvider.SearchResult>> {
        String error = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading++;
            toggleLoadingIndicator();
        }

        @Override
        protected List<TicketCheckProvider.SearchResult> doInBackground(String... params) {
            try {
                return checkProvider.search(params[0]);
            } catch (CheckException e) {
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<TicketCheckProvider.SearchResult> checkResult) {
            if (checkResult == null) {
                Toast.makeText(SearchActivity.this, error, Toast.LENGTH_SHORT).show();
            } else {
                showList(checkResult);
            }
            loading--;
            toggleLoadingIndicator();
        }
    }

    private void showList(List<TicketCheckProvider.SearchResult> checkResult) {
        SearchResultAdapter adapter = new SearchResultAdapter(
                this, R.layout.listitem_searchresult, R.id.tvAttendeeName, checkResult);
        lvResults.setAdapter(adapter);
    }

    private void toggleLoadingIndicator() {
        if (loading > 0) {
            findViewById(R.id.toolbar_progress_bar).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.toolbar_progress_bar).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
