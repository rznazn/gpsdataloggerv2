package com.babykangaroo.android.gpsdataloggerv2;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;
import com.babykangaroo.android.mylocationlibrary.LocationAccess;

import java.util.Date;

public class LoggingActivity extends AppCompatActivity implements LocationAccess.LocationUpdateListener{

    private TextView tvLogEvent;
    private TextView tvCurrentLogName;
    private TextView tvTestFunction;
    private LocationAccess mLocationAccess;
    private Location mLastGivenLocation;
    private Context context;

    private String mCurrentLog;
    private long mTimeCorrection;

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

        context = this;
        mLocationAccess = new LocationAccess(this,this);

        tvCurrentLogName = (TextView) findViewById(R.id.tv_current_log_name);
        tvTestFunction = (TextView) findViewById(R.id.tv_measure_log);
        tvTestFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] args = new String[1];
                args[0] = mCurrentLog;
                Cursor cursor = getContentResolver().query(ListContract.ListContractEntry.ITEMS_CONTENT_URI,
                        null,
                        ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST + " = ? ",
                        args,
                        ListContract.ListContractEntry.COLUMN_EVENT_TIME);
                Log.v("LOGGING ACTIVITY", String.valueOf(cursor.getCount()));
            }
        });
        Intent intent = getIntent();
        mCurrentLog = intent.getStringExtra("log name");
        tvCurrentLogName.setText(mCurrentLog);


        tvLogEvent = (TextView) findViewById(R.id.tv_log_event);
        tvLogEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLastGivenLocation != null) {
                    Location location = mLastGivenLocation;
                    long time = System.currentTimeMillis();
                    double azimuth = mLocationAccess.getmBearingMagnetic();
                    ContentValues contentValues = new ContentValues();
                    mTimeCorrection = mLocationAccess.getmGPSTimeOffset();
                    String eventTime;
                    String eventTimeEnd;
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd\\HHmmss\\SSS");
                        eventTime = dateFormat.format(new Date(time - mTimeCorrection));
                        eventTimeEnd = dateFormat.format(new Date(time + 10000 - mTimeCorrection));
                    contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "Action");
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                    contentValues.put(ListContract.ListContractEntry.COlUMN_TRACK_NUMBER, "001");
                    contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, "Test Note");
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, location.getLatitude());
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, location.getLongitude());
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
                    contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                    contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                    contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                    Uri uri = getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
                    Log.v("LOGGING ACTIVITY", String.valueOf(azimuth));
                }else {
                    Toast.makeText(context, "content values still null", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onLocationUpdate(Location location){
        long time = System.currentTimeMillis();
        mLastGivenLocation = location;
        ContentValues contentValues = new ContentValues();
        mTimeCorrection = mLocationAccess.getmGPSTimeOffset();
        String eventTime;
        String eventTimeEnd;
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd\\HHmmss\\SSS");
        eventTime = dateFormat.format(new Date(time - mTimeCorrection));
        eventTimeEnd = dateFormat.format(new Date(time + 10000 - mTimeCorrection));
//        public static final String COLUMN_ITEM_PARENT_LIST = "parent_list";
//        public static final String COLUMN_EVENT_KEYWORD = "keyword";
//        public static final String COLUMN_EVENT_TIME = "event_time";
//        public static final String COlUMN_TRACK_NUMBER = "track_number";
//        public static final String COLUMN_EVENT_LATITUDE = "event_latitude";
//        public static final String COLUMN_EVENT_LONGITUDE = "event_longitude";
//        public static final String COLUMN_EVENT_ALTITUDE = "event_altitude";
//        public static final String COLUMN_FIGURE_COLOR = "figure_color";
//        public static final String COLUMN_EVENT_BEARING_MAG = "bearing_magnetic";
//        public static final String COLUMN_EVENT_BEARING_FROM_LAST = "bearing_from_last";
//        public static final String COLUMN_EVENT_RANGE = "event_range";
//        public static final String COLUMN_EVENT_END_TIME = "event_end_time";
//        public static final String COLUMN_GPS_ACCURACY = "gps_accuracy";
//        public static final String COLUMN_SPEED_FROM_LAST = "gps_speed";
//        public static final String COLUMN_ITEM_NOTE = "note";
        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "POINT");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
        contentValues.put(ListContract.ListContractEntry.COlUMN_TRACK_NUMBER, "001");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, location.getLatitude());
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, location.getLongitude());
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
        contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST, location.getBearing());
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
        contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
        Uri uri = getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
        Log.v("LOGGING ACTIVITY", eventTime + "__" + String.valueOf(mTimeCorrection));
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
}
