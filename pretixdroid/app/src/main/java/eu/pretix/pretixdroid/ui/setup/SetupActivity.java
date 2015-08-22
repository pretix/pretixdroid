package eu.pretix.pretixdroid.ui.setup;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import eu.pretix.pretixdroid.R;

public class SetupActivity extends AppCompatActivity implements SetupPretixInitFragment.Callbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
    }
}
