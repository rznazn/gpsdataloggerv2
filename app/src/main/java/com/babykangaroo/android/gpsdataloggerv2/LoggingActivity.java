package com.babykangaroo.android.gpsdataloggerv2;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.babykangaroo.android.mydatabaselibrary.ListContract;
import com.babykangaroo.android.mylocationlibrary.LocationAccess;

public class LoggingActivity extends AppCompatActivity implements LocationAccess.LocationUpdateListener{

    private TextView tvLogEvent;
    private TextView tvCurrentLogName;
    private LocationAccess mLocationAccess;
    private ContentValues mContentValues = new ContentValues();
    private Context context;

    private String mCurrentLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = this;
        mLocationAccess = new LocationAccess(this,this);

        tvCurrentLogName = (TextView) findViewById(R.id.tv_current_log_name);
        Intent intent = getIntent();
        mCurrentLog = intent.getStringExtra("log name");
        tvCurrentLogName.setText(mCurrentLog);


        tvLogEvent = (TextView) findViewById(R.id.tv_log_event);
        tvLogEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mContentValues !=null) {
                    Uri uri = getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, mContentValues);
                    Toast.makeText(context, uri.toString(), Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(context, "content values still null", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onLocationUpdate(Location location) {
        mContentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, System.currentTimeMillis());
        mContentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
        mContentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, location.getLatitude());
        mContentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, location.getLongitude());
        mContentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
        mContentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST, location.getBearing());
        mContentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());

    }
}
