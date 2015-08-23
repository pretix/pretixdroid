package eu.pretix.pretixdroid.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.net.api.PretixApi;

public class MainActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new MainFragmentPagerAdapter(getSupportFragmentManager(),
                MainActivity.this));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.update_data:
                SharedPreferences settings = getSharedPreferences(PretixApi.PREFS_NAME, 0);
                progressDialog = ProgressDialog.show(this, getString(R.string.progress_init),
                        getString(R.string.progress_downloading), true, false);
                new DownloadPretixDataTask().execute(this, settings.getString("url", ""), settings.getString("key", ""));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class MainFragmentPagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 3;
        private String tabTitles[] = new String[]{
                getString(R.string.tab_scan),
                getString(R.string.tab_list),
                getString(R.string.tab_devices),
        };
        private Context context;

        public MainFragmentPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new ScanFragment();
            } else {
                return new BlankFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return tabTitles[position];
        }
    }


    public class DownloadPretixDataTask extends AsyncTask<Object, Integer, Boolean> {
        private Exception exception;

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                return PretixApi.downloadPretixData((Context) params[0],
                        (String) params[1], (String) params[2], false);
            } catch (Exception e) {
                exception = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            progressDialog.dismiss();
            if (exception != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(exception.getMessage()).setTitle(R.string.err_title);
                builder.setCancelable(true);
                builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else if(!success) {
                Toast.makeText(MainActivity.this, getString(R.string.err_unknown), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
