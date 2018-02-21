package eu.pretix.pretixdroid.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.joshdholtz.sentry.Sentry;

import java.util.ArrayList;
import java.util.List;

import eu.pretix.libpretixsync.check.CheckException;
import eu.pretix.libpretixsync.check.TicketCheckProvider;
import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.async.SyncService;

public class SearchActivity extends AppCompatActivity {

    private int loading = 0;
    private EditText etQuery;
    private ListView lvResults;
    private TicketCheckProvider checkProvider;
    private ProgressDialog pdCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkProvider = ((PretixDroid) getApplication()).getNewCheckProvider();

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

        lvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Sentry.addBreadcrumb("search.result.clicked", "Search result clicked");

                TicketCheckProvider.SearchResult item = (TicketCheckProvider.SearchResult) adapterView.getAdapter().getItem(i);

                startRedeem(item.getSecret(), new ArrayList<TicketCheckProvider.Answer>(), false);
            }
        });
    }

    private void startRedeem(String secret, List<TicketCheckProvider.Answer> answers, boolean ignore_pending) {
        new CheckTask().execute(secret, answers, ignore_pending);
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

    public class CheckTask extends AsyncTask<Object, Integer, TicketCheckProvider.CheckResult> {
        private String secret;
        private List<TicketCheckProvider.Answer> answers;
        boolean ignore_unpaid = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pdCheck = ProgressDialog.show(SearchActivity.this, "",
                    getString(R.string.redeeming), true);
        }

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(Object... params) {
            secret = (String) params[0];
            answers = (List<TicketCheckProvider.Answer>) params[1];
            ignore_unpaid = (boolean) params[2];
            if (secret.matches("[0-9A-Za-z-]+")) {
                return checkProvider.check(secret, answers, ignore_unpaid);
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.INVALID);
            }
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            pdCheck.dismiss();

            int default_string = 0;
            switch (checkResult.getType()) {
                case ERROR:
                    default_string = R.string.err_unknown;
                    break;
                case INVALID:
                    default_string = R.string.scan_result_invalid;
                    break;
                case UNPAID:
                    default_string = R.string.scan_result_unpaid;
                    break;
                case USED:
                    default_string = R.string.scan_result_used;
                    break;
                case VALID:
                    default_string = R.string.scan_result_redeemed;
                    break;
            }

            if (checkResult.getType() == TicketCheckProvider.CheckResult.Type.UNPAID && checkResult.isCheckinAllowed()) {
                UnpaidOrderDialogHelper.showDialog(SearchActivity.this, checkResult, secret, answers, new UnpaidOrderDialogHelper.RetryHandler() {
                    @Override
                    public void retry(String secret, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid) {
                        startRedeem(secret, answers, true);
                    }
                });
            } else if (checkResult.getType() == TicketCheckProvider.CheckResult.Type.ANSWERS_REQUIRED) {
                QuestionDialogHelper.showDialog(SearchActivity.this, checkResult, secret, new QuestionDialogHelper.RetryHandler() {
                    @Override
                    public void retry(String secret, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid) {
                        startRedeem(secret, answers, ignore_unpaid);
                    }
                }, ignore_unpaid);
            } else {
                new AlertDialog.Builder(SearchActivity.this)
                        .setMessage(getString(default_string))
                        .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();

                startSearch(etQuery.getText().toString());
                triggerSync();
            }
        }
    }

    private void triggerSync() {
        Intent i = new Intent(this, SyncService.class);
        startService(i);
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
