package eu.pretix.pretixdroid.ui;

import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import eu.pretix.pretixdroid.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScanFragment extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {
    public QRCodeReaderView qrView = null;

    public ScanFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
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
    public void onQRCodeRead(String s, PointF[] pointFs) {

    }

    @Override
    public void cameraNotFound() {

    }

    @Override
    public void QRCodeNotFoundOnCamImage() {

    }
}
