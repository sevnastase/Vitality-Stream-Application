package com.videostreamtest.ui.phone.helpers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.config.application.PraxtourApplication;
import com.videostreamtest.utils.ApplicationSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ViewHelper {
    final static String TAG = ViewHelper.class.getSimpleName();

    public static void loadImage(Context context, final ImageButton imageButton, final String imageUrl, final int defaultWidth, final int defaultHeight){
        int dpi = context.getResources().getDisplayMetrics().densityDpi;
        float dpiRatio = dpi / ApplicationSettings.RECOMMENDED_DENSITY_DPI;
        float pixelWidth = defaultWidth * dpiRatio;
        float pixelHeight = defaultHeight * dpiRatio;

        Log.d(TAG, "DPI: "+dpi+", pixelWidth: "+pixelWidth+", pixelHeight: "+pixelHeight);

        //Set product image in button
        Picasso.get()
                .load(imageUrl)
                .resize((int)pixelWidth, (int)pixelHeight)
                .placeholder(R.drawable.placeholder_button)
                .error(R.drawable.placeholder_button)
                .into(imageButton);
    }

    public static void loadImage(Context context, final ImageButton imageButton, final String imageUrl){
        List<Integer> dimensions = getExternalImageSize(imageUrl);
        if (dimensions != null && dimensions.size() == 2) {
            loadImage(context, imageButton, imageUrl, dimensions.get(1), dimensions.get(0));
        } else {
            Log.e(TAG, "ERROR RETRIEVING IMAGE INFORMATION");
        }
    }

    public static boolean isTouchScreen(final Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    private static List<Integer> getExternalImageSize(final String url) {
        List<Integer> dimensions = new ArrayList<>();
        Uri fileUri = Uri.parse(url);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(fileUri.getPath()).getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        dimensions.add(imageHeight);
        int imageWidth = options.outWidth;
        dimensions.add(imageWidth);

        return dimensions;
    }

    public static void loadImageFile(Context context, final ImageButton imageButton, final String imageUrl, final int height, final int width) {
        //CHECK IF FILE IS ON DISK

    }

    /**
     * Sets the color of ALL texts under (and including) {@param view} to {@param color}.
     * For EditTexts, it sets to hint color to a more opaque color.
     *
     * @param colorId must be given in hexadecimal
     */
    public static void setTextColorToWhiteInViewAndChildren(@NonNull View view, int colorId) {
        int color = ContextCompat.getColor(PraxtourApplication.getAppContext(), colorId);
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
        if (view instanceof Button) {
            ((Button) view).setTextColor(color);
        }

        /* Explanation: #AARRGGBB is the representation in hexadecimal
         * of the color with Alpha value AA, RGB values RR, GG, BB.
         * So by appending CA in the beginning, we can make whatever color
         * we were given semi-transparent.
         */
        if (view instanceof EditText) {
            String hexColorOpaque = String.format("#CA%06X", 0xFFFFFF & color);
            ((EditText) view).setHintTextColor(Color.parseColor(hexColorOpaque));
            ((EditText) view).setTextColor(color);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setTextColorToWhiteInViewAndChildren(viewGroup.getChildAt(i), colorId);
            }
        }
    }
}
