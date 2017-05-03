package eu.pretix.pretixdroid.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.joshdholtz.sentry.Sentry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.db.QueuedCheckIn;

public class SettingsFragment extends PreferenceFragment {

    private void resetApp() {
//        DaoSession daoSession = ((PretixDroid) getActivity().getApplication()).getDaoSession();
//        daoSession.getQueuedCheckInDao().deleteAll();
//        daoSession.getTicketDao().deleteAll();

        AppConfig config = new AppConfig(getActivity());
        config.resetEventConfig();
        Toast.makeText(getActivity(), R.string.reset_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference reset = (Preference) findPreference("action_reset");
        reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                long cnt = ((PretixDroid) getActivity().getApplication()).getData().count(QueuedCheckIn.class).get().value();
                if (cnt > 0) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.pref_reset_warning)
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // do nothing
                                }
                            })
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    resetApp();
                                }
                            }).create().show();

                } else {
                    resetApp();
                }
                return true;
            }
        });

        final Preference about = findPreference("action_about");
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                asset_dialog(R.raw.about, R.string.about);
                return true;
            }
        });

        final CheckBoxPreference async = (CheckBoxPreference) findPreference("async");
        async.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final AppConfig config = new AppConfig(getActivity());
                if (newValue instanceof Boolean && ((Boolean) newValue) != config.getAsyncModeEnabled()) {
                    final boolean isEnabled = (Boolean) newValue;
                    if (isEnabled) {
                        if (config.getApiVersion() < 3) {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(R.string.pref_async_not_supported)
                                    .setPositiveButton(getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                        }
                                    }).create().show();
                            return false;
                        }

                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.pref_async)
                                .setMessage(R.string.pref_async_warning)
                                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        config.setAsyncModeEnabled(true);
                                        async.setChecked(true);
                                    }
                                }).create().show();

                        return false;
                    }
                }
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
            Sentry.captureException(e);
            e.printStackTrace();
        }

        textView.setText(Html.fromHtml(text));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        dialog.show();
    }
}