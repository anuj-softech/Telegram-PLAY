package com.rock.tgplay.widget;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Objects;


public class LoaderDialog {

    private final Dialog loadingDialog;
    private boolean isLoading = false;
    private ObjectAnimator rotation;

    public LoaderDialog(@NonNull Context context, @DrawableRes int loaderImageRes) {
        // Initialize the dialog
        loadingDialog = new Dialog(context);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setCancelable(false);
        Objects.requireNonNull(loadingDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        loadingDialog.setContentView(createLoaderView(context, loaderImageRes));
    }

    private View createLoaderView(Context context, @DrawableRes int loaderImageRes) {
        LinearLayout loaderLayout = new LinearLayout(context);
        loaderLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        loaderLayout.setOrientation(LinearLayout.VERTICAL);

        // Create the ImageView for the loader
        ImageView loaderImageView = new ImageView(context);
        loaderImageView.setLayoutParams(new LinearLayout.LayoutParams(150, 150));
        loaderImageView.setImageResource(loaderImageRes);
        loaderImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        rotation = ObjectAnimator.ofFloat(loaderImageView, "rotation", 0f, 360f);
        rotation.setDuration(2000);
        rotation.setInterpolator(new LinearInterpolator());
        rotation.setRepeatCount(ObjectAnimator.INFINITE);
        loaderLayout.addView(loaderImageView);
        return loaderLayout;
    }

    public void startLoading() {
        if (!isLoading) {
            loadingDialog.show();
            if(rotation!=null) rotation.start();
            isLoading = true;
        }
    }

    public void stopLoading() {
        if (isLoading) {
            if(rotation!=null) rotation.cancel();
            loadingDialog.dismiss();
            isLoading = false;
        }
    }

    public boolean isLoading() {
        return isLoading;
    }
}
