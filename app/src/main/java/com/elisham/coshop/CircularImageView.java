package com.elisham.coshop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

// Custom ImageView that displays images in a circular shape
public class CircularImageView extends AppCompatImageView {

    // Constructor for CircularImageView with context
    public CircularImageView(Context context) {
        super(context);
    }

    // Constructor for CircularImageView with context and attributes
    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Constructor for CircularImageView with context, attributes, and style
    public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // Draws the circular image on the canvas
    @Override
    protected void onDraw(Canvas canvas) {
        float radius = Math.min(getWidth(), getHeight()) / 2.0f;
        Path path = new Path();
        path.addCircle(getWidth() / 2.0f, getHeight() / 2.0f, radius, Path.Direction.CW);
        canvas.clipPath(path);
        super.onDraw(canvas);
    }
}
