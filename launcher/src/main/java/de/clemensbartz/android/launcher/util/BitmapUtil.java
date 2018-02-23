/*
 * Copyright (C) 2018  Clemens Bartz
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.clemensbartz.android.launcher.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;

/**
 * Util used to scale bitmaps.
 * @author Clemens Bartz
 * @since 1.4
 */
public final class BitmapUtil {

    /** The default separator. */
    private static final String SEPARATOR = "&";

    /** The default dp. */
    private static final int DEFAULT_DP = 60;

    /**
     * Hidden constructor.
     */
    private BitmapUtil() {
    }

    /**
     * Create a key for a package name and a class name.
     * @param packname name of the package
     * @param classname name of the class
     * @return the key
     */
    public static String createKey(final String packname, final String classname) {
        return packname + SEPARATOR + classname;
    }

    /**
     * Resize a drawable to the default dp.
     * @param res the resources
     * @param drawable the drawable
     * @return a bitmap drawable
     */
    public static BitmapDrawable resizeDrawable(final Resources res, final Drawable drawable) {
        final int px = pxFrom60dp(res.getDisplayMetrics());

        final Bitmap bitmap = convertToBitmap(drawable);
        final Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, px, px, false);

        return new BitmapDrawable(res, resizedBitmap);
    }

    /**
     * Convert display metrix to px for the default dp.
     * @param metrics the display metrics
     * @return the px
     */
    private static int pxFrom60dp(final DisplayMetrics metrics) {
        return pxFromDp(metrics, DEFAULT_DP).intValue();
    }

    /**
     * Convert dp to px.
     * @param metrics the metrics of the screen
     * @param dp the dp
     * @return px
     */
    private static Float pxFromDp(final DisplayMetrics metrics, final float dp) {
        return dp * metrics.density;
    }

    /**
     * Convert any drawable in a bitmap.
     * @param drawable the drawable
     * @return a bitmap
     */
    private static Bitmap convertToBitmap(final Drawable drawable) {
        // Check if there is a shortcut
        if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        // Get the foreground drawable
        Drawable foregroundDrawable = drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
            foregroundDrawable = ((AdaptiveIconDrawable) drawable).getForeground();
        }

        // Check the size
        final Rect bounds = foregroundDrawable.getBounds();
        final int boundsWidth = bounds.isEmpty() ? drawable.getIntrinsicWidth() : bounds.width();
        final int boundsHeight = bounds.isEmpty() ? drawable.getIntrinsicHeight() : bounds.height();

        final int bitmapWidth = (boundsWidth > 0) ? boundsWidth : 1;
        final int bitmapHeight = (boundsHeight > 0) ? boundsHeight : 1;

        // Create the bitmap
        final Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);

        // Draw the picture on the canvas
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
