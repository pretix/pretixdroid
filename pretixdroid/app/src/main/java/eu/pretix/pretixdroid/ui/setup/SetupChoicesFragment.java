package eu.pretix.pretixdroid.ui.setup;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;

public class SetupChoicesFragment extends Fragment {
    private Callbacks callbacks;
    private View view;

    public SetupChoicesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setup_choices, container, false);

        View.OnClickListener listener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean multi = ((RadioButton) view.findViewById(R.id.rbMultiDevice)).isChecked();
                view.findViewById(R.id.rbSloppy).setEnabled(multi);
                view.findViewById(R.id.rbStrict).setEnabled(multi);

                view.findViewById(R.id.btContinue).setEnabled(
                        ((RadioButton) view.findViewById(R.id.rbOneDevice)).isChecked()
                                || (
                                ((RadioButton) view.findViewById(R.id.rbMultiDevice)).isChecked() && (
                                        ((RadioButton) view.findViewById(R.id.rbStrict)).isChecked()
                                                || ((RadioButton) view.findViewById(R.id.rbSloppy)).isChecked()
                                )
                        ));
            }
        };


        view.findViewById(R.id.rbSloppy).setEnabled(false);
        view.findViewById(R.id.rbStrict).setEnabled(false);
        view.findViewById(R.id.rbSloppy).setOnClickListener(listener);
        view.findViewById(R.id.rbStrict).setOnClickListener(listener);
        view.findViewById(R.id.rbMultiDevice).setOnClickListener(listener);
        view.findViewById(R.id.rbOneDevice).setOnClickListener(listener);

        view.findViewById(R.id.btContinue).setEnabled(false);
        view.findViewById(R.id.btContinue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = getActivity().getSharedPreferences(PretixDroid.PREFS_NAME, 0);
                settings.edit()
                        .putBoolean("multidevice", ((RadioButton) view.findViewById(R.id.rbMultiDevice)).isChecked())
                        .putBoolean("strict", ((RadioButton) view.findViewById(R.id.rbStrict)).isChecked())
                        .apply();
                if (callbacks != null) {
                    callbacks.choicesDone();
                }

            }
        });

        return view;
    }

    public interface Callbacks {
        public void choicesDone();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        callbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

}
