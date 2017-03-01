package eu.pretix.pretixdroid.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference reset = (Preference) findPreference("action_reset");
        reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AppConfig config = new AppConfig(getActivity());
                config.resetEventConfig();
                Toast.makeText(getActivity(), R.string.reset_success, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Preference about = findPreference("action_about");
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                asset_dialog(R.raw.about, R.string.about);
                return true;
            }
        });
    }

    private void asset_dialog(@RawRes int htmlRes, @StringRes int title) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_about, null, false);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.dismiss, null)
                .create();

        TextView textView = (TextView) view.findViewById(R.id.aboutText);

        String text = "";

        StringBuilder builder = new StringBuilder();
        InputStream fis;
        try {
            fis = getResources().openRawResource(htmlRes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            text = builder.toString();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        textView.setText(Html.fromHtml(text));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        dialog.show();
    }
}