package eu.pretix.pretixdroid.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.View;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.ui.setup.SetupActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Backwards-compatible button colors
        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.pretix_brand_light)});
        AppCompatButton btNewEvent = (AppCompatButton) findViewById(R.id.btNewEvent);
        btNewEvent.setSupportBackgroundTintList(csl);
        AppCompatButton btLastEvent = (AppCompatButton) findViewById(R.id.btLastEvent);
        btLastEvent.setSupportBackgroundTintList(csl);

        btLastEvent.setEnabled(false);

        btNewEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, SetupActivity.class);
                startActivity(intent);
            }
        });
    }
}
