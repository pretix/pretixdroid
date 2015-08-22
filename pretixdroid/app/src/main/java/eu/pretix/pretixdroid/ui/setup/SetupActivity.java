package eu.pretix.pretixdroid.ui.setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.Set;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;

public class SetupActivity extends AppCompatActivity implements SetupPretixInitFragment.Callbacks,
        SetupChoicesFragment.Callbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        SetupPretixInitFragment initFragment = new SetupPretixInitFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, initFragment).commit();
    }

    @Override
    public void onDataDownloaded() {
        SetupChoicesFragment choicesFragment = new SetupChoicesFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, choicesFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void choicesDone() {
        SharedPreferences settings = getSharedPreferences(PretixDroid.PREFS_NAME, 0);
        if (settings.getBoolean("multidevice")) {
            Toast.makeText(this, "Not yet implemented.", Toast.LENGTH_SHORT);
        } else {
            Intent intent = new Intent(SetupActivity.this, SetupActivity.class);
            startActivity(intent);
        }
    }
}
