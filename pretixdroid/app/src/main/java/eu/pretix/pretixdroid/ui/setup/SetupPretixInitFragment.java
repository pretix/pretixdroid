package eu.pretix.pretixdroid.ui.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.api.PretixApi;

/**
 * A placeholder fragment containing a simple view.
 */
public class SetupPretixInitFragment extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {

    private Callbacks callbacks = null;
    private QRCodeReaderView qrView = null;
    private boolean working = false;
    private ProgressDialog progressDialog;
    private long lastScan;

    public SetupPretixInitFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_pretix_init, container, false);
        qrView = (QRCodeReaderView) view.findViewById(R.id.qrdecoderview);
        qrView.setOnQRCodeReadListener(this);
        return view;
    }

    public interface Callbacks {
        public void onDataDownloaded();
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
    public void onQRCodeRead(String s, PointF[] pointFs) {
        if (working || System.currentTimeMillis() - lastScan < 2000) {
            return;
        }
        lastScan = System.currentTimeMillis();
        try {
            JSONObject jsonObject = new JSONObject(s);
            if (jsonObject.getInt("version") != 1) {
                Toast.makeText(getActivity(), getString(R.string.err_qr_version), Toast.LENGTH_LONG).show();
            } else {
                pretixInit(jsonObject.getString("url"), jsonObject.getString("key"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getString(R.string.err_qr_invalid), Toast.LENGTH_SHORT).show();
        }
    }

    public void pretixInit(String url, String key) {
        working = true;
        SharedPreferences settings = getActivity().getSharedPreferences(PretixApi.PREFS_NAME, 0);
        settings.edit().putString("url", url).putString("key", key).apply();
        progressDialog = ProgressDialog.show(getActivity(), getString(R.string.progress_init),
                getString(R.string.progress_downloading), true, false);
        new DownloadPretixDataTask().execute(getActivity(), url, key);
    }

    @Override
    public void cameraNotFound() {
        Toast.makeText(getActivity(), getString(R.string.err_no_camera), Toast.LENGTH_LONG).show();
    }

    @Override
    public void QRCodeNotFoundOnCamImage() {
    }

    @Override
    public void onResume() {
        super.onResume();
        qrView.getCameraManager().startPreview();
    }

    @Override
    public void onPause() {
        super.onPause();
        qrView.getCameraManager().stopPreview();
    }

    public class DownloadPretixDataTask extends AsyncTask<Object, Integer, Boolean> {
        private Exception exception;

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                return PretixApi.downloadPretixData((Context) params[0],
                        (String) params[1], (String) params[2]);
            } catch (Exception e) {
                exception = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            progressDialog.dismiss();
            working = false;
            if (success) {
                callbacks.onDataDownloaded();
            } else if (exception != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(exception.getMessage()).setTitle(R.string.err_title);
                builder.setCancelable(true);
                builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.err_unknown), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
