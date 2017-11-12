package eu.pretix.pretixdroid.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class CustomizedScannerView extends ZXingScannerView {
    private Rect mFramingRectInPreview;

    public CustomizedScannerView(Context context) {
        super(context);
    }

    public CustomizedScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public synchronized Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if(this.mFramingRectInPreview == null) {
            Rect rect = new Rect();
            rect.left = 0;
            rect.top = 0;
            rect.right = previewWidth;
            rect.bottom = previewHeight;

            this.mFramingRectInPreview = rect;
        }

        return this.mFramingRectInPreview;
    }
}
