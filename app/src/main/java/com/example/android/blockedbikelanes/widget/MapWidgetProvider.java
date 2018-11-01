package com.example.android.blockedbikelanes.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.android.blockedbikelanes.R;
import com.example.android.blockedbikelanes.model.MarkerData;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;



public class MapWidgetProvider extends AppWidgetProvider {

    public static final String MAP_PATH = "/uploads/";

    private HashMap<String, Marker> markers = new HashMap<>();

    private static final String TAG = MapWidgetProvider.class.getSimpleName();


    private static void updateAppWidget(Context context, final AppWidgetManager appWidgetManager,
                                        final int appWidgetId) {

        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.map_widget_provider);

        FirebaseDatabase mFirebaseDatabase;
        DatabaseReference mFirebaseRef;

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseRef = mFirebaseDatabase.getReference(MAP_PATH);


        mFirebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                String address = dataSnapshot.getValue(MarkerData.class).getAddress();
                String date = dataSnapshot.getValue(MarkerData.class).getDate();
                String time = dataSnapshot.getValue(MarkerData.class).getTime();

                Log.d(TAG, "Widget debug:" + address);

                remoteViews.setTextViewText(R.id.address_widget, address);
                remoteViews.setTextViewText(R.id.date_widget, date);
                remoteViews.setTextViewText(R.id.time_widget, time);


                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public static void updateRecipeWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}

