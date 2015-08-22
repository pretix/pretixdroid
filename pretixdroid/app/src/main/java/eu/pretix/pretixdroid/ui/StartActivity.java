package eu.pretix.pretixdroid.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.ui.setup.SetupActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        final SharedPreferences settings = getSharedPreferences(PretixDroid.PREFS_NAME, 0);

        // Backwards-compatible button colors
        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.pretix_brand_light)});
        AppCompatButton btNewEvent = (AppCompatButton) findViewById(R.id.btNewEvent);
        btNewEvent.setSupportBackgroundTintList(csl);
        AppCompatButton btLastEvent = (AppCompatButton) findViewById(R.id.btLastEvent);
        btLastEvent.setSupportBackgroundTintList(csl);

        btLastEvent.setEnabled(settings.contains("multidevice"));

        btNewEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (settings.contains("multidevice")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                    builder.setMessage(R.string.warn_delete_current);
                    builder.setCancelable(true);
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    builder.setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(StartActivity.this, SetupActivity.class);
                            startActivity(intent);
                        }
                    });
                } else {
                    Intent intent = new Intent(StartActivity.this, SetupActivity.class);
                    startActivity(intent);
                }
            }
        });

        btLastEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
