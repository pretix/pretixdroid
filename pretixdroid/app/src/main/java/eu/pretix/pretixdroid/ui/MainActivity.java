package eu.pretix.pretixdroid.ui;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.PointF;
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
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.io.IOException;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

public class MainActivity extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    private QRCodeReaderView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private State state = State.SCANNING;
    private Handler timeoutHandler;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        qrView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        qrView.setOnQRCodeReadListener(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = buildMediaPlayer(this);
        timeoutHandler = new Handler();
        resetView();
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

    @Override
    public void onQRCodeRead(String s, PointF[] pointFs) {
        if (s.equals(lastScanCode) && System.currentTimeMillis() - lastScanTime < 5000) {
            return;
        }
        lastScanTime = System.currentTimeMillis();
        lastScanCode = s;

        mediaPlayer.start();
        state = State.LOADING;
        resetView();
        findViewById(R.id.rlScanStatus).setVisibility(View.VISIBLE);
        new CheckTask().execute(s);
    }

    @Override
    public void cameraNotFound() {
        Toast.makeText(this, getString(R.string.err_no_camera), Toast.LENGTH_LONG).show();
    }

    @Override
    public void QRCodeNotFoundOnCamImage() {
    }

    private void resetView() {
        timeoutHandler.removeCallbacksAndMessages(null);
        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        findViewById(R.id.tvScanResult).setVisibility(View.INVISIBLE);
        findViewById(R.id.tvTicketName).setVisibility(View.INVISIBLE);
        findViewById(R.id.tvAttendeeName).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.tvTicketName)).setText("");
        ((TextView) findViewById(R.id.tvScanResult)).setText("");
        ((TextView) findViewById(R.id.tvAttendeeName)).setText("");
        findViewById(R.id.rlScanStatus).setVisibility(View.INVISIBLE);
        findViewById(R.id.rlScanStatus).setBackgroundColor(
                getResources().getColor(R.color.scan_result_unknown));
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            if (params[0].matches("[0-9A-Za-z-]+")) {
                //return checkProvider.check(params[0]);
                return null;
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.INVALID);
            }
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
            TextView tvTicketName = (TextView) findViewById(R.id.tvTicketName);
            TextView tvAttendeeName = (TextView) findViewById(R.id.tvAttendeeName);

            findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
            tvScanResult.setVisibility(View.VISIBLE);
            if (checkResult.getType() == TicketCheckProvider.CheckResult.Type.VALID
                    || checkResult.getType() == TicketCheckProvider.CheckResult.Type.USED) {
                tvTicketName.setVisibility(View.VISIBLE);
                tvAttendeeName.setVisibility(View.VISIBLE);
                if (checkResult.getVariation() != null && !checkResult.getVariation().equals("null")) {
                    tvTicketName.setText(checkResult.getTicket() + " – " + checkResult.getVariation());
                } else {
                    tvTicketName.setText(checkResult.getTicket());
                }
                if (checkResult.getAttendee_name() != null) {
                    tvAttendeeName.setText(checkResult.getAttendee_name());
                }
            }

            switch (checkResult.getType()) {
                case ERROR:
                    tvScanResult.setText(checkResult.getMessage());
                    findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_err));
                    break;
                case INVALID:
                    tvScanResult.setText(getString(R.string.scan_result_invalid));
                    findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_err));
                    break;
                case USED:
                    tvScanResult.setText(getString(R.string.scan_result_used));
                    findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_warn));
                    break;
                case VALID:
                    tvScanResult.setText(getString(R.string.scan_result_valid));
                    findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_ok));
                    break;
            }

            timeoutHandler.postDelayed(new Runnable() {
                public void run() {
                    resetView();
                }
            }, 10000);
        }
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
