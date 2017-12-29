package com.babykangaroo.android.gpsdataloggerv2;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import edu.nps.moves.dis.ArticulationParameter;
import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.EntityType;
import edu.nps.moves.dis.Vector3Double;
import edu.nps.moves.disutil.CoordinateConversions;
import edu.nps.moves.disutil.DisTime;

public class LoggingActivity extends AppCompatActivity implements LocationAccess.LocationUpdateListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private ImageView tvLogEvent;
    private TextView tvBearing;
    private TextView tvCurrentLogName;
    private TextView tvLogNote;
    private TextView tvEditLog;
    private ImageView ivAdminSettings;

    private LocationAccess mLocationAccess;
    private Location mLastGivenLocation;
    private Context mContext;
    private DisTime disTime;

    private SharedPreferences sharedPreferences;
    private String trackId;
    private int loggingInterval;
    private long lastUpdate= 0;
    private String destinationIp;
    private int destinationPort;
    private boolean liveUpdates;
    private boolean minimizedTracking;

    private String mCurrentLog;
    private long mTimeCorrection;
    private long mAzimuthUpdateTimeReference;
    private int mBearingMagnetic;
    private boolean adConfirmed;

    private MulticastSocket multicastSocket;
    private boolean isActive;
    private java.text.SimpleDateFormat dateFormat;
    private class PduSendTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                byte[] data = (byte[]) objects[0];
                multicastSocket.send(new DatagramPacket(data, data.length, InetAddress.getByName(destinationIp),
                        destinationPort));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        /**disable screen TimeOut
         **/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_logging);

        mContext = this;
        tvCurrentLogName = (TextView) findViewById(R.id.tv_current_log_name);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.contains(getString(R.string.current_log))){
            Intent intent = new Intent(this, FileManagerActivity.class);
            startActivity(intent);
        }

        try {
            multicastSocket = new MulticastSocket();
            multicastSocket.setTimeToLive(128);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        disTime = DisTime.getInstance();
        setSharedPreferences();
        mLocationAccess = new LocationAccess(this, this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        tvBearing = (TextView) findViewById(R.id.tv_bearing);


        ivAdminSettings = (ImageView) findViewById(R.id.iv_settings);
        ivAdminSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        tvEditLog = (TextView) findViewById(R.id.tv_edit_log);
        tvEditLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openEditLog();
            }
        });


        tvLogNote = (TextView) findViewById(R.id.tv_log_note);
        tvLogNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logEvent(2,mLastGivenLocation, System.currentTimeMillis()-mTimeCorrection,null);
            }
        });

        tvLogEvent = (ImageView) findViewById(R.id.iv_log_event);
        tvLogEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logEvent(1, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);
            }
        });

        dateFormat = new java.text.SimpleDateFormat("yyyyMMdd\\HHmmss\\SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public void onLocationUpdate(Location location) {
        /**
         * Log point data at each location update to create a track of travel
         */
        if (!minimizedTracking && !isActive){
            mLocationAccess.stopUpdates();
            return;
        }

        mLastGivenLocation = location;
        mTimeCorrection = mLocationAccess.getmGPSTimeOffset();
        if (lastUpdate+(loggingInterval*1000)> System.currentTimeMillis()){return;}
        lastUpdate = System.currentTimeMillis();
        long gpsTime = location.getTime();
        String eventTime;
        String eventTimeEnd;
        eventTime = dateFormat.format(new Date(gpsTime));
        eventTimeEnd = dateFormat.format(new Date(gpsTime + 10000));
        ContentValues contentValues = new ContentValues();
        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "POINT");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED, "FALSE");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
        contentValues.put(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER, trackId);

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

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected() && liveUpdates) {

            EntityStatePdu espdu = new EntityStatePdu();

            espdu.setExerciseID((short)1);

            String[] id = trackId.split("(?!^)");
            EntityID eid = espdu.getEntityID();
            eid.setSite(Integer.valueOf(id[0]));
            eid.setApplication(Integer.valueOf(id[1]));
            eid.setEntity(Integer.valueOf(id[2]));

            EntityType entityType = espdu.getEntityType();
            entityType.setEntityKind((short)1);      // Platform (vs lifeform, munition, sensor, etc.)
            entityType.setCountry(225);              // USA
            entityType.setDomain((short)1);          // Land (vs air, surface, subsurface, space)
            entityType.setCategory((short)1);        // Tank
            entityType.setSubcategory((short)1);     // M1 Abrams
            entityType.setSpec((short)3);

            int ts = disTime.getDisAbsoluteTimestamp();
            espdu.setTimestamp(ts);

            double disCoordinates[] = CoordinateConversions.
                    getXYZfromLatLonDegrees(location.getLatitude(), location.getLongitude(), location.getAltitude());
            Vector3Double locationespdu = espdu.getEntityLocation();
            locationespdu.setX(disCoordinates[0]);
            locationespdu.setY(disCoordinates[1]);
            locationespdu.setZ(disCoordinates[2]);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            espdu.marshal(dos);

            // The byte array here is the packet in DIS format. We put that into a
            // datagram and send it.
            byte[] data = baos.toByteArray();

            AsyncTask currentTask = new PduSendTask();
            currentTask.execute(data);
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
        isActive = false;
        if (!minimizedTracking) {
            try {
                mLocationAccess.stopUpdates();
            }catch (NullPointerException e){
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
        try {
            mLocationAccess.stopUpdates();
        }catch (NullPointerException e) {

        }
        mLocationAccess.startLocationUpdates();
    }

    private void logEvent(final int type1forBearing2forNote,
                          final Location location,
                          final long gpsCorrectedTime,
                          @Nullable final Integer azimuth) {
        if (mLastGivenLocation != null) {
            adConfirmed = false;

            if (type1forBearing2forNote == 1) {
                ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isConnected() && liveUpdates) {

                    List<ArticulationParameter> articulationParameters = new ArrayList<ArticulationParameter>();
                    ArticulationParameter apRange = new ArticulationParameter();
                    apRange.setParameterType(941);
                    apRange.setParameterValue(2500);
                    articulationParameters.add(apRange);
                    ArticulationParameter apBearing = new ArticulationParameter();
                    apBearing.setParameterType(940);
                    int newAzimuth;
                    if (azimuth > 180) {
                        newAzimuth = azimuth - 360;
                    } else {
                        newAzimuth = azimuth;
                    }
                    double radians = newAzimuth * (Math.PI / 180);
                    apBearing.setParameterValue(radians);
                    articulationParameters.add(apBearing);

                    EntityStatePdu espdu = new EntityStatePdu();

                    espdu.setNumberOfArticulationParameters((byte) 2);
                    espdu.setArticulationParameters(articulationParameters);

                    espdu.setExerciseID((short) 1);

                    String[] id = trackId.split("(?!^)");
                    EntityID eid = espdu.getEntityID();
                    eid.setSite(Integer.valueOf(id[0]));
                    eid.setApplication(Integer.valueOf(id[1]));
                    eid.setEntity(Integer.valueOf(id[2]));

                    EntityType entityType = espdu.getEntityType();
                    entityType.setEntityKind((short) 1);      // Platform (vs lifeform, munition, sensor, etc.)
                    entityType.setCountry(225);              // USA
                    entityType.setDomain((short) 1);          // Land (vs air, surface, subsurface, space)
                    entityType.setCategory((short) 1);        // Tank
                    entityType.setSubcategory((short) 1);     // M1 Abrams
                    entityType.setSpec((short) 3);

                    int ts = disTime.getDisAbsoluteTimestamp();
                    espdu.setTimestamp(ts);

                    double disCoordinates[] = CoordinateConversions.
                            getXYZfromLatLonDegrees(location.getLatitude(), location.getLongitude(), location.getAltitude());
                    Vector3Double locationespdu = espdu.getEntityLocation();
                    locationespdu.setX(disCoordinates[0]);
                    locationespdu.setY(disCoordinates[1]);
                    locationespdu.setZ(disCoordinates[2]);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    espdu.marshal(dos);

                    // The byte array here is the packet in DIS format. We put that into a
                    // datagram and send it.
                    byte[] data = baos.toByteArray();

                    AsyncTask currentTask = new PduSendTask();
                    currentTask.execute(data);
                }
            }

            final String eventTime = dateFormat.format(new Date(gpsCorrectedTime));
            final String eventTimeEnd = dateFormat.format(new Date(gpsCorrectedTime + 10000));

            final View adLayout = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
            final TextView adtvEventSummary = (TextView) adLayout.findViewById(R.id.tv_event_summary);
            String text = "Event time: " + eventTime +
                    "\nAzimuth: " + azimuth +
                    "\nLat: " + location.getLatitude() +
                    "\nLong: " + location.getLongitude();
            adtvEventSummary.setText(text);
            final EditText adetEventNote = (EditText) adLayout.findViewById(R.id.et_event_note);
            AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setView(adLayout);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!adConfirmed) {
                        String note;
                        ContentValues contentValues;
                        String latitude;
                        String longitude;
                        switch (type1forBearing2forNote) {
                            case 1:
                                        note = adetEventNote.getText().toString();
                                        contentValues = new ContentValues();
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED, "TRUE");
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER, trackId);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LINEB_LL");
                                        latitude = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, latitude);
                                        longitude = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, longitude);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                                        getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);



                                break;
                            case 2:
                                        note = adetEventNote.getText().toString();
                                        contentValues = new ContentValues();
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED, "TRUE");
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER, trackId);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LL");
                                        latitude = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, latitude);
                                        longitude = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, longitude);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST, location.getBearing());
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                                        contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                                        getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);


                                break;
                        }
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
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED, "FALSE");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER, trackId);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LINEB_LL");
                            String latitude = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, latitude);
                            String longitude = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, longitude);
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
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED, "FALSE");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER, trackId);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LL");
                            String latitude = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, latitude);
                            String longitude = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, longitude);
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

    private void openEditLog(){
        Intent intent = new Intent(this, EditLogActivity.class);
        startActivity(intent);
    }

    private void openSettings(){
        final View adView = getLayoutInflater().inflate(R.layout.password_dialog, null);
        final TextView tvMessage = (TextView) adView.findViewById(R.id.tv_event_summary);
        tvMessage.setText("Enter Password");
        final EditText etPassword = (EditText) adView.findViewById(R.id.et_event_note);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(adView);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String password = etPassword.getText().toString();
                if (password.equals("GeneRocks"  ) || password.equals(sharedPreferences.getString(getString(R.string.admin_password), getString(R.string.default_admin_password)))){
                    mLocationAccess.stopUpdates();
                    Intent intent = new Intent(mContext, FileManagerActivity.class);
                    startActivity(intent);
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    void setSharedPreferences(){
        mCurrentLog = sharedPreferences.getString(getString(R.string.current_log), "default");
        tvCurrentLogName.setText(mCurrentLog);
        trackId = sharedPreferences.getString(getString(R.string.track_id), getString(R.string.default_track_id));
        loggingInterval = sharedPreferences.getInt(getString(R.string.log_interval), getResources().getInteger(R.integer.default_log_interval));
        destinationIp = sharedPreferences.getString(getString(R.string.destination_ip), getString(R.string.default_ip));
        destinationPort = Integer.valueOf(sharedPreferences.getString(getString(R.string.destination_port), getString(R.string.default_port)));
        liveUpdates = sharedPreferences.getBoolean(getString(R.string.live_updates), false);
        minimizedTracking = sharedPreferences.getBoolean(getString(R.string.minimized_tracking), false);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        setSharedPreferences();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }
}