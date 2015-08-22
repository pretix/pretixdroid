package eu.pretix.pretixdroid.ui.setup;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.json.JSONException;
import org.json.JSONObject;

import eu.pretix.pretixdroid.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SetupPretixInitFragment extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {

    Callbacks callbacks = null;
    QRCodeReaderView qrView = null;

    public SetupPretixInitFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_pretix_init, container, false);
        qrView = (QRCodeReaderView) view.findViewById(R.id.qrdecoderview);
        return view;
    }

    public interface Callbacks {

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
        try {
            JSONObject jsonObject = new JSONObject(s);
            if (jsonObject.getInt("version") != 1) {
                Toast.makeText(getActivity(), getString(R.string.err_qr_version), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Gotta do something!", Toast.LENGTH_LONG).show();
                // Do something
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getString(R.string.err_qr_invalid), Toast.LENGTH_SHORT).show();
        }
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
}
