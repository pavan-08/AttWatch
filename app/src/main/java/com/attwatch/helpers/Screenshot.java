package com.attwatch.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

/**
 * Created by Pavan on 6/3/2016.
 */
public class Screenshot {
    private final View view;
    /** Create snapshots based on the view and its children. */
    public Screenshot(View root) {
        this.view = root;
    }
    /** Create snapshot handler that captures the root of the whole activity. */
    public Screenshot(Activity activity) {
        final View contentView = activity.findViewById(android.R.id.content);
        this.view = contentView.getRootView();
    }
    /** Take a snapshot of the view. */
    public Bitmap snap() {
        Bitmap bitmap = Bitmap.createBitmap(this.view.getWidth(), this.view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
}
