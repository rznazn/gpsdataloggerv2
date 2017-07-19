package com.babykangaroo.android.gpsdataloggerv2;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;
import com.babykangaroo.android.mylocationlibrary.LocationAccess;
import com.babykangaroo.android.myudpdatagramlib.UdpDatagram;
import com.example.WamFormater;

import java.util.Date;

public class LoggingActivity extends AppCompatActivity implements LocationAccess.LocationUpdateListener {

    private ImageView tvLogEvent;
    private TextView tvBearing;
    private TextView tvCurrentLogName;
    private TextView tvLogNote;
    private LocationAccess mLocationAccess;
    private Location mLastGivenLocation;
    private Context mContext;

    private String mCurrentLog;
    private long mTimeCorrection;
    private long mAzimuthUpdateTimeReference;
    private int mBearingMagnetic;
    private boolean adWasDismissed;

    private UdpDatagram mDatagram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        /**
         * disable screen TimeOut
         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_logging);

        mContext = this;
        mDatagram = new UdpDatagram(this, "192.168.1.8", 50000);
        mLocationAccess = new LocationAccess(this, this);

        tvCurrentLogName = (TextView) findViewById(R.id.tv_current_log_name);
        tvBearing = (TextView) findViewById(R.id.tv_bearing);
        tvLogNote = (TextView) findViewById(R.id.tv_log_note);
        tvLogNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logEvent(2,mLastGivenLocation, System.currentTimeMillis()-mTimeCorrection,null);
            }
        });
        Intent intent = getIntent();
        mCurrentLog = intent.getStringExtra("log name");
        tvCurrentLogName.setText(mCurrentLog);


        tvLogEvent = (ImageView) findViewById(R.id.iv_log_event);
        tvLogEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logEvent(1, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);
            }
        });

    }

    @Override
    public void onLocationUpdate(Location location) {
        /**
         * Log point data at each location update to create a track of travel
         */
        mLastGivenLocation = location;
        ContentValues contentValues = new ContentValues();
        mTimeCorrection = mLocationAccess.getmGPSTimeOffset();
        long gpsCorrectedTime = System.currentTimeMillis()- mTimeCorrection;
        String eventTime;
        String eventTimeEnd;
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd\\HHmmss\\SSS");
        eventTime = dateFormat.format(new Date(gpsCorrectedTime));
        eventTimeEnd = dateFormat.format(new Date(gpsCorrectedTime + 10000));
        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "POINT");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
        contentValues.put(ListContract.ListContractEntry.COlUMN_TRACK_NUMBER, "001");

        String latitude = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, latitude);
        String longitude = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, longitude);
        String altitude = String.valueOf(location.getAltitude());
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, altitude);

        contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST, location.getBearing());
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
        contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
        Uri uri = getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
        String wamDataPack = WamFormater.formatPoint(eventTime,"001",latitude,
                longitude,altitude);
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            mDatagram.sendPacket(wamDataPack);
        }
    }

    @Override
    public void onAzimuthChange(double azimuth) {
        mBearingMagnetic = (int) azimuth;
        long time = System.currentTimeMillis();
        if (mAzimuthUpdateTimeReference == 0 || mAzimuthUpdateTimeReference < time) {
            tvBearing.setText(String.valueOf(mBearingMagnetic));
            mAzimuthUpdateTimeReference = System.currentTimeMillis() + 150;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationAccess.stopUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationAccess.startLocationUpdates();
    }

    private void logEvent(int type1forBearing2forNote,
                          final Location location,
                          final long gpsCorrectedTime,
                          @Nullable final Integer azimuth) {
        if (mLastGivenLocation != null) {
            adWasDismissed = false;
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd\\HHmmss\\SSS");
            final String eventTime = dateFormat.format(new Date(gpsCorrectedTime));
            final String eventTimeEnd = dateFormat.format(new Date(gpsCorrectedTime + 10000));

            final View adLayout = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
            final TextView adtvEventSummary = (TextView) adLayout.findViewById(R.id.tv_event_summary);
            adtvEventSummary.setText("Event time: " + eventTime +
                    "\nAzimuth: " + azimuth +
                    "\nLat: " + location.getLatitude() +
                    "\nLong: " + location.getLongitude()
            );
            final EditText adetEventNote = (EditText) adLayout.findViewById(R.id.et_event_note);
            AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setView(adLayout);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!adWasDismissed) {
                        /**
                         * TODO implement later
                          */
                    }
                }
            });
            builder.setNeutralButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            });
            switch (type1forBearing2forNote) {
                case 1:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adWasDismissed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "Action");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                            contentValues.put(ListContract.ListContractEntry.COlUMN_TRACK_NUMBER, "001");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LINEB_LL");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, location.getLatitude());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, location.getLongitude());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                            getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
                        }
                    });
                    break;
                case 2:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adWasDismissed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "Action");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                            contentValues.put(ListContract.ListContractEntry.COlUMN_TRACK_NUMBER, "001");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LL");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, location.getLatitude());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, location.getLongitude());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST, location.getBearing());
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                            getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
                        }
                    });

                    break;
            }
            AlertDialog ad = builder.create();
            ad.show();
        } else {
            Toast.makeText(mContext, "GPS not acquired", Toast.LENGTH_LONG).show();
        }
    }
}
