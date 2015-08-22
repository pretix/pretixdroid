package eu.pretix.pretixdroid.ui;

import android.app.Activity;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScanFragment extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    private QRCodeReaderView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private View view;
    private State state = State.SCANNING;
    private TicketCheckProvider checkProvider;

    public ScanFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_scan, container, false);
        qrView = (QRCodeReaderView) view.findViewById(R.id.qrdecoderview);
        qrView.setOnQRCodeReadListener(this);
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
    }

    @Override
    public void onQRCodeRead(String s, PointF[] pointFs) {
        if (s.equals(lastScanCode) && System.currentTimeMillis() - lastScanTime < 5000) {
            return;
        }
        lastScanTime = System.currentTimeMillis();
        lastScanCode = s;

        state = State.LOADING;
        view.findViewById(R.id.rlScanStatus).setVisibility(View.VISIBLE);
        view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                getResources().getColor(R.color.scan_result_unknown));
        view.findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        new CheckTask().execute(s);
    }

    @Override
    public void cameraNotFound() {
        Toast.makeText(getActivity(), getString(R.string.err_no_camera), Toast.LENGTH_LONG).show();
    }

    @Override
    public void QRCodeNotFoundOnCamImage() {
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            return checkProvider.check(params[0]);
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            switch (checkResult.getType()) {
                case ERROR:
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_err));
                    break;
                case INVALID:
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_err));
                    break;
                case USED:
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_warn));
                    break;
                case VALID:
                    view.findViewById(R.id.rlScanStatus).setBackgroundColor(
                            getResources().getColor(R.color.scan_result_ok));
                    break;
            }
        }
    }
}
