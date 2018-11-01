package com.example.android.blockedbikelanes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;


class InfoWindowInfo implements InfoWindowAdapter {

    private static final String TAG = InfoWindowInfo.class.getSimpleName();

    private View infoWindow = null;
    private LayoutInflater inflater;
    private HashMap<String, Uri> markers;
    private Context context;
    private Marker lastMarker = null;

    InfoWindowInfo(Context ctxt, LayoutInflater inflater, HashMap<String, Uri> markers) {
        this.context = ctxt;
        this.inflater = inflater;
        this.markers = markers;

    }

    @Override
    public View getInfoWindow(Marker marker) {
        return (null);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getInfoContents(Marker marker) {
        if (infoWindow == null) {
            infoWindow = inflater.inflate(R.layout.info_window, null);
        }

        if (lastMarker == null
                || !lastMarker.getId().equals(marker.getId())) {
            lastMarker = marker;

            TextView snippett_text_view = infoWindow.findViewById(R.id.snippet);
            snippett_text_view.setText(marker.getSnippet());

            Uri imageUri = markers.get(marker.getId());

            Log.d(TAG, "URI TESTER" + imageUri);

            ImageView image = infoWindow.findViewById(R.id.image);

            if (imageUri == null) {
                image.setVisibility(View.GONE);
            } else {
                Glide.with(context).load(imageUri).placeholder(R.drawable.placeholder).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate().into(image);
            }


        }

        return (infoWindow);

    }


}

