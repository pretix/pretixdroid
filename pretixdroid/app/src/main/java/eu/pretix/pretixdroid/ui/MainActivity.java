package eu.pretix.pretixdroid.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.joshdholtz.sentry.Sentry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import eu.pretix.libpretixsync.api.PretixApi;
import eu.pretix.libpretixsync.check.TicketCheckProvider;
import eu.pretix.libpretixsync.db.QueuedCheckIn;
import eu.pretix.pretixdroid.AppConfig;
import eu.pretix.pretixdroid.BuildConfig;
import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.async.SyncService;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    public static final int PERMISSIONS_REQUEST_CAMERA = 10001;

    private CustomizedScannerView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private State state = State.SCANNING;
    private Handler timeoutHandler;
    private Runnable blinkExecute;
    private boolean blinkDark = true;
    private Handler blinkHandler;
    private MediaPlayer mediaPlayer;
    private TicketCheckProvider checkProvider;
    private AppConfig config;
    private Timer timer;
    private Dialog questionsDialog;
    private Dialog unpaidDialog;

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("com.symbol.datawedge.data_string")) {
                // Zebra DataWedge
                handleScan(intent.getStringExtra("com.symbol.datawedge.data_string"));
            } else if (intent.hasExtra("barocode")) {
                // Intent receiver for LECOM-manufactured hardware scanners
                byte[] barcode = intent.getByteArrayExtra("barocode"); // sic!
                int barocodelen = intent.getIntExtra("length", 0);
                String barcodeStr = new String(barcode, 0, barocodelen);
                handleScan(barcodeStr);
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (BuildConfig.SENTRY_DSN != null) {
            Sentry.init(this, BuildConfig.SENTRY_DSN);
        }

        checkProvider = ((PretixDroid) getApplication()).getNewCheckProvider();
        config = new AppConfig(this);

        setContentView(R.layout.activity_main);

        qrView = (CustomizedScannerView) findViewById(R.id.qrdecoderview);
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
        blinkHandler = new Handler();

        findViewById(R.id.rlSyncStatus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSyncStatusDetails();
            }
        });

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

    private class SyncTriggerTask extends TimerTask {

        @Override
        public void run() {
            triggerSync();
        }
    }

    private class UpdateSyncStatusTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateSyncStatus();
                }
            });
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
            // Broadcast sent by Lecom or Zebra scanners
            IntentFilter filter = new IntentFilter();
            filter.addAction("scan.rcv.message");
            filter.addAction("eu.pretix.SCAN");
            registerReceiver(scanReceiver, filter);
        }

        timer = new Timer();
        timer.schedule(new SyncTriggerTask(), 1000, 10000);
        timer.schedule(new UpdateSyncStatusTask(), 500, 500);
        updateSyncStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (config.getCamera()) {
            qrView.stopCamera();
        } else {
            unregisterReceiver(scanReceiver);
        }
        timer.cancel();
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
        if (questionsDialog != null && questionsDialog.isShowing()) {
            // Do not scan while asking questions
            return;
        }
        if (unpaidDialog != null && unpaidDialog.isShowing()) {
            // Do not scan while asking questions
            return;
        }

        if (config.getSoundEnabled()) mediaPlayer.start();
        resetView();

        if (config.isConfigured()) {
            handleTicketScanned(s, new ArrayList<TicketCheckProvider.Answer>(), false);
        } else {
            handleConfigScanned(s);
        }
    }

    private void handleConfigScanned(String s) {
        Sentry.addBreadcrumb("main.scanned", "Config scanned");

        try {
            JSONObject jsonObject = new JSONObject(s);
            if (jsonObject.getInt("version") > PretixApi.SUPPORTED_API_VERSION) {
                displayScanResult(new TicketCheckProvider.CheckResult(
                        TicketCheckProvider.CheckResult.Type.ERROR,
                        getString(R.string.err_qr_version)), null, false);
            } else {
                if (jsonObject.getInt("version") < 3) {
                    config.setAsyncModeEnabled(false);
                }
                config.setEventConfig(jsonObject.getString("url"), jsonObject.getString("key"),
                        jsonObject.getInt("version"), jsonObject.optBoolean("show_info", true),
                        jsonObject.optBoolean("allow_search", true));
                checkProvider = ((PretixDroid) getApplication()).getNewCheckProvider();
                displayScanResult(new TicketCheckProvider.CheckResult(
                        TicketCheckProvider.CheckResult.Type.VALID,
                        getString(R.string.config_done)), null, false);

                triggerSync();
            }
        } catch (JSONException e) {
            displayScanResult(new TicketCheckProvider.CheckResult(
                    TicketCheckProvider.CheckResult.Type.ERROR,
                    getString(R.string.err_qr_invalid)), null, false);
        }
    }

    private void triggerSync() {
        Intent i = new Intent(this, SyncService.class);
        startService(i);
    }

    private void handleTicketScanned(String s, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid) {
        Sentry.addBreadcrumb("main.scanned", "Ticket scanned");

        state = State.LOADING;
        findViewById(R.id.tvScanResult).setVisibility(View.GONE);
        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        new CheckTask().execute(s, answers, ignore_unpaid);
    }

    private void updateSyncStatus() {
        if (config.getAsyncModeEnabled()) {
            findViewById(R.id.rlSyncStatus).setVisibility(View.VISIBLE);

            if (config.getLastFailedSync() > config.getLastSync() || System.currentTimeMillis() - config.getLastDownload() > 5 * 60 * 1000) {
                findViewById(R.id.rlSyncStatus).setBackgroundColor(ContextCompat.getColor(this, R.color.scan_result_err));
            } else {
                findViewById(R.id.rlSyncStatus).setBackgroundColor(ContextCompat.getColor(this, R.color.scan_result_ok));
            }
            String text;
            long diff = System.currentTimeMillis() - config.getLastDownload();
            if (config.getLastDownload() == 0) {
                text = getString(R.string.sync_status_never);
            } else if (diff > 24 * 3600 * 1000) {
                int days = (int) (diff / (24 * 3600 * 1000));
                text = getResources().getQuantityString(R.plurals.time_days, days, days);
            } else if (diff > 3600 * 1000) {
                int hours = (int) (diff / (3600 * 1000));
                text = getResources().getQuantityString(R.plurals.time_hours, hours, hours);
            } else if (diff > 60 * 1000) {
                int mins = (int) (diff / (60 * 1000));
                text = getResources().getQuantityString(R.plurals.time_minutes, mins, mins);
            } else {
                text = getString(R.string.sync_status_now);
            }

            ((TextView) findViewById(R.id.tvSyncStatus)).setText(text);
        } else {
            findViewById(R.id.rlSyncStatus).setVisibility(View.GONE);
        }
    }

    public void showSyncStatusDetails() {
        Calendar lastSync = Calendar.getInstance();
        lastSync.setTimeInMillis(config.getLastSync());
        Calendar lastSyncFailed = Calendar.getInstance();
        lastSyncFailed.setTimeInMillis(config.getLastFailedSync());
        long cnt = ((PretixDroid) getApplication()).getData().count(QueuedCheckIn.class).get().value();

        SimpleDateFormat formatter = new SimpleDateFormat(getString(R.string.sync_status_date_format));
        new AlertDialog.Builder(this)
                .setTitle(R.string.sync_status)
                .setMessage(
                        getString(R.string.sync_status_last) + "\n" +
                                formatter.format(lastSync.getTime()) + "\n\n" +
                                getString(R.string.sync_status_local) + cnt +
                                (config.getLastFailedSync() > 0 ? (
                                        "\n\n" +
                                                getString(R.string.sync_status_last_failed) + "\n" +
                                                formatter.format(lastSyncFailed.getTime()) +
                                                "\n" + config.getLastFailedSyncMsg()
                                ) : "")

                )
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();

    }

    private void resetView() {
        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
        timeoutHandler.removeCallbacksAndMessages(null);
        blinkHandler.removeCallbacksAndMessages(null);
        tvScanResult.setVisibility(View.VISIBLE);
        findViewById(R.id.rlWarning).setVisibility(View.GONE);
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

    public class CheckTask extends AsyncTask<Object, Integer, TicketCheckProvider.CheckResult> {
        List<TicketCheckProvider.Answer> answers;
        boolean ignore_unpaid;

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(Object... params) {
            String secret = (String) params[0];
            answers = (List<TicketCheckProvider.Answer>) params[1];
            ignore_unpaid = (boolean) params[2];
            if (secret.matches("[0-9A-Za-z-]+")) {
                return checkProvider.check(secret, answers, ignore_unpaid);
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.INVALID, getString(R.string.scan_result_invalid));
            }
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            displayScanResult(checkResult, answers, ignore_unpaid);
            triggerSync();
        }
    }

    private void displayScanResult(TicketCheckProvider.CheckResult checkResult, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid) {
        if (checkResult.getType() == TicketCheckProvider.CheckResult.Type.ANSWERS_REQUIRED) {
            questionsDialog = QuestionDialogHelper.showDialog(this, checkResult, lastScanCode, new QuestionDialogHelper.RetryHandler() {
                @Override
                public void retry(String secret, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid) {
                    handleTicketScanned(secret, answers, ignore_unpaid);
                }
            }, ignore_unpaid);
        }
        if (checkResult.getType() == TicketCheckProvider.CheckResult.Type.UNPAID && checkResult.isCheckinAllowed()) {
            unpaidDialog = UnpaidOrderDialogHelper.showDialog(this, checkResult, lastScanCode, answers, new UnpaidOrderDialogHelper.RetryHandler() {
                @Override
                public void retry(String secret, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid) {
                    handleTicketScanned(secret, answers, ignore_unpaid);
                }
            });
        }

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
            case PRODUCT:
                col = R.color.scan_result_err;
                default_string = R.string.scan_result_product;
                break;
            case ANSWERS_REQUIRED:
                col = R.color.scan_result_warn;
                default_string = R.string.scan_result_questions;
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

        findViewById(R.id.rlWarning).setVisibility(
                checkResult.isRequireAttention() ? View.VISIBLE : View.GONE
        );

        if (checkResult.isRequireAttention()) {
            blinkExecute = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (blinkDark) {
                            findViewById(R.id.rlWarning).setBackgroundColor(getResources().getColor(R.color.scan_result_attention_alternate));
                            ((TextView) findViewById(R.id.tvWarning)).setTextColor(getResources().getColor(R.color.pretix_brand_dark));
                            ((ImageView) findViewById(R.id.ivWarning)).setImageResource(R.drawable.ic_warning_dark_24dp);
                            blinkDark = false;
                        } else {
                            findViewById(R.id.rlWarning).setBackgroundColor(getResources().getColor(R.color.scan_result_attention));
                            ((TextView) findViewById(R.id.tvWarning)).setTextColor(getResources().getColor(R.color.white));
                            ((ImageView) findViewById(R.id.ivWarning)).setImageResource(R.drawable.ic_warning_white_24dp);
                            blinkDark = true;
                        }
                    } finally {
                        blinkHandler.postDelayed(blinkExecute, 200);
                    }
                }
            };
            blinkExecute.run();
        }

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
            case R.id.action_eventinfo:
                if (config.isConfigured()) {
                    Intent intent = new Intent(this, EventinfoActivity.class);
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
