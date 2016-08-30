package eu.pretix.pretixdroid.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.OnlineCheckProvider;
import eu.pretix.pretixdroid.check.TicketCheckProvider;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class MainActivity extends AppCompatActivity implements ZBarScannerView.ResultHandler, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    private ZBarScannerView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private State state = State.SCANNING;
    private Handler timeoutHandler;
    private MediaPlayer mediaPlayer;
    private TicketCheckProvider checkProvider;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrView = (ZBarScannerView) findViewById(R.id.qrdecoderview);
        qrView.setResultHandler(this);
        qrView.setAutoFocus(true);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = buildMediaPlayer(this);

        timeoutHandler = new Handler();

        checkProvider = new OnlineCheckProvider(this);
        prefs = getSharedPreferences(PretixDroid.PREFS_NAME, MODE_PRIVATE);

        resetView();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_logo);
    }

    @Override
    public void onResume() {
        super.onResume();
        qrView.setResultHandler(this);
        qrView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        qrView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        qrView.resumeCameraPreview(this);
        String s = rawResult.getContents();
        if (s.equals(lastScanCode) && System.currentTimeMillis() - lastScanTime < 5000) {
            return;
        }
        lastScanTime = System.currentTimeMillis();
        lastScanCode = s;

        mediaPlayer.start();
        state = State.LOADING;
        resetView();

        if (prefs.contains(PretixDroid.PREFS_KEY_API_URL)) {
            handleTicketScanned(s);
        } else {
            handleConfigScanned(s);
        }
    }

    private void handleConfigScanned(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            if (jsonObject.optInt("version", 0) != 2) {
                displayScanResult(new TicketCheckProvider.CheckResult(
                        TicketCheckProvider.CheckResult.Type.ERROR,
                        getString(R.string.err_qr_version)));
            } else {
                prefs.edit()
                        .putString(PretixDroid.PREFS_KEY_API_URL, jsonObject.getString("url"))
                        .putString(PretixDroid.PREFS_KEY_API_KEY, jsonObject.getString("key"))
                        .apply();

                displayScanResult(new TicketCheckProvider.CheckResult(
                        TicketCheckProvider.CheckResult.Type.VALID,
                        getString(R.string.config_done)));
            }
        } catch (JSONException e) {
            displayScanResult(new TicketCheckProvider.CheckResult(
                    TicketCheckProvider.CheckResult.Type.ERROR,
                    getString(R.string.err_qr_invalid)));
        }
    }

    private void handleTicketScanned(String s) {
        new CheckTask().execute(s);
    }

    private void resetView() {
        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
        timeoutHandler.removeCallbacksAndMessages(null);
        tvScanResult.setVisibility(View.VISIBLE);
        findViewById(R.id.tvTicketName).setVisibility(View.INVISIBLE);
        findViewById(R.id.tvAttendeeName).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.tvTicketName)).setText("");
        ((TextView) findViewById(R.id.tvScanResult)).setText("");
        ((TextView) findViewById(R.id.tvAttendeeName)).setText("");
        findViewById(R.id.rlScanStatus).setBackgroundColor(
                getResources().getColor(R.color.scan_result_unknown));

        if (prefs.contains(PretixDroid.PREFS_KEY_API_URL)) {
            tvScanResult.setText(R.string.hint_scan);
        } else {
            tvScanResult.setText(R.string.hint_config);
        }
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
        }

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            if (params[0].matches("[0-9A-Za-z-]+")) {
                return checkProvider.check(params[0]);
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.INVALID);
            }
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            displayScanResult(checkResult);
        }
    }

    private void displayScanResult(TicketCheckProvider.CheckResult checkResult) {
        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
        TextView tvTicketName = (TextView) findViewById(R.id.tvTicketName);
        TextView tvAttendeeName = (TextView) findViewById(R.id.tvAttendeeName);

        findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
        tvScanResult.setVisibility(View.VISIBLE);
        if (checkResult.getType() == TicketCheckProvider.CheckResult.Type.VALID
                || checkResult.getType() == TicketCheckProvider.CheckResult.Type.USED) {
            if (checkResult.getTicket() != null) {
                tvTicketName.setVisibility(View.VISIBLE);
                if (checkResult.getVariation() != null && !checkResult.getVariation().equals("null")) {
                    tvTicketName.setText(checkResult.getTicket() + " – " + checkResult.getVariation());
                } else {
                    tvTicketName.setText(checkResult.getTicket());
                }
            }
            if (checkResult.getAttendee_name() != null) {
                tvAttendeeName.setVisibility(View.VISIBLE);
                tvAttendeeName.setText(checkResult.getAttendee_name());
            }
        }

        int col = R.color.scan_result_unknown;
        int default_string = R.string.err_unknown;

        switch (checkResult.getType()) {
            case ERROR:
                col = R.color.scan_result_err;
                default_string = R.string.err_unknown;
                break;
            case INVALID:
                col = R.color.scan_result_err;
                default_string = R.string.scan_result_invalid;
                break;
            case USED:
                col = R.color.scan_result_warn;
                default_string = R.string.scan_result_used;
                break;
            case VALID:
                col = R.color.scan_result_ok;
                default_string = R.string.scan_result_valid;
                break;
        }

        if (checkResult.getMessage() != null) {
            tvScanResult.setText(checkResult.getMessage());
        } else {
            tvScanResult.setText(getString(default_string));
        }
        findViewById(R.id.rlScanStatus).setBackgroundColor(getResources().getColor(col));

        timeoutHandler.postDelayed(new Runnable() {
            public void run() {
                resetView();
            }
        }, 10000);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0);
    }

    private MediaPlayer buildMediaPlayer(Context activity) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        // mediaPlayer.setOnErrorListener(this);
        try {
            AssetFileDescriptor file = activity.getResources()
                    .openRawResourceFd(R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mediaPlayer.setVolume(0.10f, 0.10f);
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException ioe) {
            mediaPlayer.release();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
