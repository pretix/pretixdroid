package eu.pretix.pretixdroid.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.io.IOException;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScanFragment extends Fragment implements QRCodeReaderView.OnQRCodeReadListener, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    private QRCodeReaderView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private View view;
    private State state = State.SCANNING;
    private TicketCheckProvider checkProvider;
    private Handler timeoutHandler;
    private MediaPlayer mediaPlayer;

    public ScanFragment() {
        timeoutHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_scan, container, false);
        qrView = (QRCodeReaderView) view.findViewById(R.id.qrdecoderview);
        qrView.setOnQRCodeReadListener(this);
        resetView();
        return view;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        checkProvider = ((PretixDroid) getActivity().getApplication()).getCheckProvider(getActivity());
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = buildMediaPlayer(getActivity());
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
        view.findViewById(R.id.rlScanStatus).setVisibility(View.VISIBLE);
        new CheckTask().execute(s);
    }

    @Override
    public void cameraNotFound() {
        Toast.makeText(getActivity(), getString(R.string.err_no_camera), Toast.LENGTH_LONG).show();
    }

    @Override
    public void QRCodeNotFoundOnCamImage() {
    }

    private void resetView() {
        timeoutHandler.removeCallbacksAndMessages(null);
        view.findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        view.findViewById(R.id.tvScanResult).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.tvTicketName).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.tvAttendeeName).setVisibility(View.INVISIBLE);
        ((TextView) view.findViewById(R.id.tvTicketName)).setText("");
        ((TextView) view.findViewById(R.id.tvScanResult)).setText("");
        ((TextView) view.findViewById(R.id.tvAttendeeName)).setText("");
        view.findViewById(R.id.rlScanStatus).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                getResources().getColor(R.color.scan_result_unknown));
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

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
            TextView tvScanResult = (TextView) view.findViewById(R.id.tvScanResult);
            TextView tvTicketName = (TextView) view.findViewById(R.id.tvTicketName);
            TextView tvAttendeeName = (TextView) view.findViewById(R.id.tvAttendeeName);

            view.findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
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
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_err));
                    break;
                case INVALID:
                    tvScanResult.setText(getString(R.string.scan_result_invalid));
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_err));
                    break;
                case USED:
                    tvScanResult.setText(getString(R.string.scan_result_used));
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_warn));
                    break;
                case VALID:
                    tvScanResult.setText(getString(R.string.scan_result_valid));
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
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
}
