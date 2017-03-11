package eu.pretix.pretixdroid.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.joshdholtz.sentry.Sentry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.BuildConfig;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.OnlineCheckProvider;
import eu.pretix.pretixdroid.check.TicketCheckProvider;
import eu.pretix.pretixdroid.net.api.PretixApi;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    public static final int PERMISSIONS_REQUEST_CAMERA = 10001;

    private ZXingScannerView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private State state = State.SCANNING;
    private Handler timeoutHandler;
    private MediaPlayer mediaPlayer;
    private TicketCheckProvider checkProvider;
    private AppConfig config;

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Intent receiver for LECOM-manufactured hardware scanners
            byte[] barcode = intent.getByteArrayExtra("barocode"); // sic!
            int barocodelen = intent.getIntExtra("length", 0);
            String barcodeStr = new String(barcode, 0, barocodelen);
            handleScan(barcodeStr);
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (BuildConfig.SENTRY_DSN != null) {
            Sentry.init(this, BuildConfig.SENTRY_DSN);
        }

        checkProvider = new OnlineCheckProvider(this);
        config = new AppConfig(this);

        setContentView(R.layout.activity_main);

        qrView = (ZXingScannerView) findViewById(R.id.qrdecoderview);
        qrView.setResultHandler(this);
        qrView.setAutoFocus(config.getAutofocus());
        qrView.setFlash(config.getFlashlight());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Sentry.addBreadcrumb("main.startup", "Permission request started");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
        }

        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);

        qrView.setFormats(formats);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = buildMediaPlayer(this);

        timeoutHandler = new Handler();

        resetView();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_logo);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Sentry.addBreadcrumb("main.startup", "Permission granted");
                    if (config.getCamera()) {
                        qrView.startCamera();
                    }
                } else {
                    Sentry.addBreadcrumb("main.startup", "Permission request denied");
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (config.getCamera()) {
            qrView.setResultHandler(this);
            qrView.startCamera();
            qrView.setAutoFocus(config.getAutofocus());
            resetView();
        } else {
            IntentFilter filter = new IntentFilter();
            // Broadcast sent by Lecom scanners
            filter.addAction("scan.rcv.message");
            registerReceiver(scanReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (config.getCamera()) {
            qrView.stopCamera();
        } else {
            unregisterReceiver(scanReceiver);
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        if (config.getCamera()) {
            qrView.resumeCameraPreview(this);
        }
        String s = rawResult.getText();
        if (s.equals(lastScanCode) && System.currentTimeMillis() - lastScanTime < 5000) {
            return;
        }
        lastScanTime = System.currentTimeMillis();
        lastScanCode = s;

        handleScan(s);
    }

    public void handleScan(String s) {
        if (config.getSoundEnabled()) mediaPlayer.start();
        resetView();

        if (config.isConfigured()) {
            handleTicketScanned(s);
        } else {
            handleConfigScanned(s);
        }
    }

    private void handleConfigScanned(String s) {
        Sentry.addBreadcrumb("main.scanned", "Config scanned");

        try {
            JSONObject jsonObject = new JSONObject(s);
            if (jsonObject.getInt("version") != PretixApi.API_VERSION) {
                displayScanResult(new TicketCheckProvider.CheckResult(
                        TicketCheckProvider.CheckResult.Type.ERROR,
                        getString(R.string.err_qr_version)));
            } else {
                config.setEventConfig(jsonObject.getString("url"), jsonObject.getString("key"));
                checkProvider = new OnlineCheckProvider(this);
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
        Sentry.addBreadcrumb("main.scanned", "Ticket scanned");

        state = State.LOADING;
        findViewById(R.id.tvScanResult).setVisibility(View.GONE);
        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        new CheckTask().execute(s);
    }

    private void resetView() {
        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
        timeoutHandler.removeCallbacksAndMessages(null);
        tvScanResult.setVisibility(View.VISIBLE);
        findViewById(R.id.tvTicketName).setVisibility(View.INVISIBLE);
        findViewById(R.id.tvAttendeeName).setVisibility(View.INVISIBLE);
        findViewById(R.id.tvOrderCode).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.tvTicketName)).setText("");
        ((TextView) findViewById(R.id.tvScanResult)).setText("");
        ((TextView) findViewById(R.id.tvAttendeeName)).setText("");
        ((TextView) findViewById(R.id.tvOrderCode)).setText("");
        findViewById(R.id.rlScanStatus).setBackgroundColor(
                ContextCompat.getColor(this, R.color.scan_result_unknown));

        if (config.isConfigured()) {
            tvScanResult.setText(R.string.hint_scan);
        } else {
            tvScanResult.setText(R.string.hint_config);
        }

        if (!config.getCamera()) {
            qrView.setVisibility(View.GONE);
        } else {
            qrView.setVisibility(View.VISIBLE);
        }
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            if (params[0].matches("[0-9A-Za-z-]+")) {
                return checkProvider.check(params[0]);
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.INVALID, getString(R.string.scan_result_invalid));
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
        TextView tvOrderCode = (TextView) findViewById(R.id.tvOrderCode);

        state = State.RESULT;
        findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
        tvScanResult.setVisibility(View.VISIBLE);

        if (checkResult.getTicket() != null) {
            tvTicketName.setVisibility(View.VISIBLE);
            if (checkResult.getVariation() != null && !checkResult.getVariation().equals("null")) {
                tvTicketName.setText(checkResult.getTicket() + " – " + checkResult.getVariation());
            } else {
                tvTicketName.setText(checkResult.getTicket());
            }
        }

        if (checkResult.getAttendee_name() != null && !checkResult.getAttendee_name().equals("null")) {
            tvAttendeeName.setVisibility(View.VISIBLE);
            tvAttendeeName.setText(checkResult.getAttendee_name());
        }

        if (checkResult.getOrderCode() != null && !checkResult.getOrderCode().equals("null")) {
            tvOrderCode.setVisibility(View.VISIBLE);
            tvOrderCode.setText(checkResult.getOrderCode());
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
            case UNPAID:
                col = R.color.scan_result_err;
                default_string = R.string.scan_result_unpaid;
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
        findViewById(R.id.rlScanStatus).setBackgroundColor(ContextCompat.getColor(this, col));

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

        MenuItem checkable = menu.findItem(R.id.action_flashlight);
        checkable.setChecked(config.getFlashlight());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_flashlight:
                config.setFlashlight(!item.isChecked());
                if (config.getCamera()) {
                    qrView.setFlash(!item.isChecked());
                }
                item.setChecked(!item.isChecked());
                return true;
            case R.id.action_preferences:
                Intent intent_settings = new Intent(this, SettingsActivity.class);
                startActivity(intent_settings);
                return true;
            case R.id.action_search:
                if (config.isConfigured()) {
                    Intent intent = new Intent(this, SearchActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.not_configured, Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
