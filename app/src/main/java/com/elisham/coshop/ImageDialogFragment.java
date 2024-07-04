package com.elisham.coshop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImageDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE_URL = "image_url";
    private boolean isFullScreen = false;

    public static ImageDialogFragment newInstance(String imageUrl) {
        ImageDialogFragment fragment = new ImageDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_image, container, false);

        ImageView dialogImageView = view.findViewById(R.id.dialogImageView);
        String imageUrl = getArguments().getString(ARG_IMAGE_URL);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            int size = (int) (getResources().getDisplayMetrics().widthPixels * 0.4);
            Glide.with(this)
                    .load(imageUrl)
                    .override(size, size)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.star)
                    .fitCenter()
                    .into(dialogImageView);
        } else {
            dialogImageView.setImageResource(R.drawable.star);
        }

        view.setOnClickListener(v -> dismiss());

        dialogImageView.setOnClickListener(v -> {
            if (isFullScreen) {
                dismiss();
            } else {
                toggleFullScreen(dialogImageView);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View dialogImageView = getView().findViewById(R.id.dialogImageView);
        updateDialogSize(dialogImageView);
    }

    private void toggleFullScreen(View dialogImageView) {
        isFullScreen = !isFullScreen;
        updateDialogSize(dialogImageView);
    }

    private void updateDialogSize(View dialogImageView) {
        ViewGroup.LayoutParams params = dialogImageView.getLayoutParams();
        if (isFullScreen) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            int size = (int) (getResources().getDisplayMetrics().widthPixels * 0.4);
            params.width = size;
            params.height = size;
        }
        dialogImageView.setLayoutParams(params);
        dialogImageView.requestLayout();
    }
}
