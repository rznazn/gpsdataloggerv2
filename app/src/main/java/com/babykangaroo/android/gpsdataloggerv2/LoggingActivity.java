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
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.PendingIntent;
import android.app.Notification;

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

    //private ImageView tvLogEvent;
    private TextView tvBearing;
    private TextView tvCurrentLogName;
    private TextView tvLogNote;
    private TextView tvEditLog;
    private ImageView ivAdminSettings;
    private Button tgtButton;
    private Button engageButton;
    private Button oneShotButton;
    private Button msnSuccess;
    private Button msnFail;

    private LocationService mLocationAccess;
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
    private boolean enemyEngaged;   //to keep track of state of ENGAGE button, either ENGAGE START or ENGAGE STOP

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
        mLocationAccess = new LocationService(this, this);
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
                // Removing mTimeCorrection. UTC given with System.currentTimeMillis() call
                // logEvent(1,mLastGivenLocation, System.currentTimeMillis()-mTimeCorrection,null);
                logEvent(1,mLastGivenLocation, System.currentTimeMillis(),null);
            }
        });

        tgtButton = (Button) findViewById(R.id.btnTgtId);
        tgtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removing mTimeCorrection. UTC given with System.currentTimeMillis() call
                // logEvent(2, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);
                logEvent(2, mLastGivenLocation, System.currentTimeMillis(), mBearingMagnetic);
            }
        });

        engageButton = (Button) findViewById(R.id.btnEngage);

        engageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //logEvent(3, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);

                // Attempting to correct gpsTime issue using device system time. - Carte July 2020
                // long gpsTime = lastUpdate - mTimeCorrection;
                // if (gpsTime > 0 && gpsTime < 1546300800000L) //there is a bug in the gps date where the week beyond April 6, 2019 is not recognized; this code attempts to correct that issue
                // {
                //    gpsTime += 619315200000L;
                // }

                // Use current system time (based off epoch).
                // As long as good gps signal (required for events), current system time should be good.
                // NOTE: Initial test give correct UTC on WAM Output - Carte Jul 2020
                // TODO - Extra testing required
                long gpsTime = System.currentTimeMillis();

                final String eventTime = dateFormat.format(new Date(gpsTime));
                final String eventTimeEnd = dateFormat.format(new Date((gpsTime) + 10000));



                if(mLastGivenLocation != null) {
                    Location location;
                    location = mLastGivenLocation;

                    if (!enemyEngaged) {
                        enemyEngaged = true;

                        
                        ivAdminSettings.setClickable(false);
                        engageButton.setText(R.string.engage_stop);
                        engageButton.setBackgroundColor(getResources().getColor(R.color.colorAccentEngStop));

                        String note = "";
                        ContentValues contentValues = new ContentValues();

                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "ENGAGE STARTED");
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
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, mBearingMagnetic);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                        getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
                    } // end if (!enemyEngaged)
                    else if (enemyEngaged) {
                        enemyEngaged = false;
                        ivAdminSettings.setClickable(true);
                        engageButton.setText(R.string.engage_start);
                        engageButton.setBackgroundColor(getResources().getColor(R.color.colorAccentEngStrt));

                        String note = "";
                        ContentValues contentValues = new ContentValues();

                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "ENGAGE STOPPED");
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_WAS_CANCELLED, "FALSE");
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TIME, eventTime);   //was eventTime
                        contentValues.put(ListContract.ListContractEntry.COLUMN_TRACK_NUMBER, trackId);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_NOTE, note);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE, "TEXT_LINEB_LL");
                        String latitude = Location.convert(location.getLatitude(), Location.FORMAT_MINUTES);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE, latitude);
                        String longitude = Location.convert(location.getLongitude(), Location.FORMAT_MINUTES);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE, longitude);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE, location.getAltitude());
                        contentValues.put(ListContract.ListContractEntry.COLUMN_FIGURE_COLOR, "11");
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, mBearingMagnetic);
                        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);   //was eventTimeEnd
                        contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                        getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
                    } // end else if (enemyEngaged)
                } // end if(mLastGivenLocation != null)
                else { //location is invalid
                Toast.makeText(mContext, "GPS not acquired", Toast.LENGTH_LONG).show();
                }
            } // end onClick
        });

        oneShotButton = (Button) findViewById(R.id.btnOneShot);
        oneShotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removing mTimeCorrection. UTC given with System.currentTimeMillis() call
                // logEvent(3, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);
                logEvent(3, mLastGivenLocation, System.currentTimeMillis(), mBearingMagnetic);
            }
        });

        msnSuccess = (Button) findViewById((R.id.success));
        msnSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removing mTimeCorrection. UTC given with System.currentTimeMillis() call
                // logEvent(4, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);
                logEvent(4, mLastGivenLocation, System.currentTimeMillis(), mBearingMagnetic);
            }
        });

        msnFail = (Button) findViewById((R.id.failure));
        msnFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removing mTimeCorrection. UTC given with System.currentTimeMillis() call
                // logEvent(5, mLastGivenLocation, System.currentTimeMillis() - mTimeCorrection, mBearingMagnetic);
                logEvent(5, mLastGivenLocation, System.currentTimeMillis(), mBearingMagnetic);
            }
        });

        dateFormat = new java.text.SimpleDateFormat("yyyyMMdd\\HHmmss\\SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }//end onCreate method

    @Override
    public void onLocationUpdate(Location location) {
        /**
         * Log point data at each location update to create a track of travel
         */
        if (!minimizedTracking && !isActive){
//            mLocationAccess.stopForeground(true);
            mLocationAccess.stopUpdates();
            return;
        }

        mLastGivenLocation = location;
        mTimeCorrection = mLocationAccess.getmGPSTimeOffset();
        if (lastUpdate+(loggingInterval*1000)> System.currentTimeMillis()){return;}
        lastUpdate = System.currentTimeMillis();

        // Attempting to correct gpsTime issue using device system time. - Carte July 2020
        //long gpsTime = lastUpdate - mTimeCorrection;   //was just location.getTime(); so the point records were not getting millis
        //if (gpsTime > 0 && gpsTime < 1546300800000L)
        //{
        //    gpsTime += 619315200000L;
        //}


        String eventTime;
        String eventTimeEnd;
        // eventTime = dateFormat.format(new Date(gpsTime));
        // eventTimeEnd = dateFormat.format(new Date(gpsTime + 10000));
        eventTime = dateFormat.format(new Date(lastUpdate));
        eventTimeEnd = dateFormat.format(new Date(lastUpdate + 10000));
        ContentValues contentValues = new ContentValues();
        contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "POINT");
        contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "LOCATION UPDATE");
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
    }//end onLocationUpdate

    @Override
    public void onAzimuthChange(double azimuth) {
        mBearingMagnetic = (int) azimuth;
        long gpsTime = System.currentTimeMillis();
        if (gpsTime > 0 && gpsTime < 1546300800000L)
        {
            gpsTime += 619315200000L;
        }
        if (mAzimuthUpdateTimeReference == 0 || mAzimuthUpdateTimeReference < gpsTime) {
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

    private void logEvent(final int typeOfEvent,
                          final Location location,
                          final long gpsCorrectedTime,
                          @Nullable final Integer azimuth) {
        if (mLastGivenLocation != null) {         //if not an invalid location
            adConfirmed = false;                  //reset the alert dialog  boolean to false

            if (typeOfEvent == 2 || typeOfEvent == 3) {
                ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isConnected() && liveUpdates) { //if there is network connectivity and the user wants continuous updates
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
                    entityType.setCountry(225);               // USA
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
                } //end liveUpdates
            }

            long gpsTime = gpsCorrectedTime;

            if (gpsTime > 0 && gpsTime < 1546300800000L) {
                gpsTime += 619315200000L;
            }

            final String eventTime = dateFormat.format(new Date(gpsTime)); //Gene corrected time because GPS time to account for the difference between the time the message was received and GPS time
            final String eventTimeEnd = dateFormat.format(new Date(gpsTime + 10000));
            final View adLayout = getLayoutInflater().inflate(R.layout.log_event_alert_dialog, null);
            final TextView adtvEventSummary = (TextView) adLayout.findViewById(R.id.tv_event_summary);
            String text = "Event time: " + eventTime +
                    "\nAzimuth: " + azimuth +
                    "\nLat: " + location.getLatitude() +
                    "\nLong: " + location.getLongitude();
            adtvEventSummary.setText(text);
            final EditText adetEventNote = (EditText) adLayout.findViewById(R.id.et_event_note);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(adLayout);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {   //this listens to the user to determine if they press cancel or confirm
                    if (!adConfirmed) {                 //upon cancel, the following code executes:
                        String note;
                        ContentValues contentValues;
                        String latitude;
                        String longitude;

                        switch (typeOfEvent) {
                            case 1:
                                note = adetEventNote.getText().toString();
                                contentValues = new ContentValues();
                                //could make the KEYWORD something else in the cancelled events. they won't show in the log, but could still see them in the CSV/WAM files
                                //need to ask Jason/Jake if they would be ok with that
                                //do not want to send all point records out to the log because of the periodic location updates, would flood the log
                                contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "NOTE ADDED");
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
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "TARGET IDENTIFIED");
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
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                                getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);

                                break;

                                case 3:
                                note = adetEventNote.getText().toString();
                                contentValues = new ContentValues();

                                contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "SINGLE SHOT FIRED");
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
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                                getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);

                                break;

                            case 4:
                                note = adetEventNote.getText().toString();
                                contentValues = new ContentValues();

                                contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "MISSION SUCCESSFUL");
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
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                                getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);

                                break;

                            case 5:
                                note = adetEventNote.getText().toString();
                                contentValues = new ContentValues();

                                contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "MISSION FAILURE");
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
                                contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
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
            switch (typeOfEvent) {
                case 1:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "NOTE ADDED");
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

                case 2:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "TARGET IDENTIFIED");
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
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                            getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);
                        }
                    });

                    break;

                case 3:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "SINGLE SHOT FIRED");
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
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                            getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);

                        }
                    });

                    break;

                case 4:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "MISSION SUCCESSFUL");
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
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                            getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);

                        }
                    });

                    break;

                case 5:
                    builder.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adConfirmed = true;
                            String note = adetEventNote.getText().toString();
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST, mCurrentLog);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD, "ACTION");
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_TYPE, "MISSION FAILED");
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
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG, azimuth);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_EVENT_END_TIME, eventTimeEnd);
                            contentValues.put(ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST, location.getSpeed());
                            getContentResolver().insert(ListContract.ListContractEntry.ITEMS_CONTENT_URI, contentValues);

                        }
                    });

                    break;

            } //end switch(typeOfEvent)

            AlertDialog ad = builder.create(); //i didn't understand why these were down here and not with the rest of the button stuff at the start of
            ad.show();                         //this function, but upon moving them learned that it is because you need to define the buttons and behavior first

        } else { //location is invalid
            Toast.makeText(mContext, "GPS not acquired", Toast.LENGTH_LONG).show();
        }

    }//end logEvent


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
                // update to shared password validation method
                // if (password.equals("GeneRocks"  ) || password.equals(sharedPreferences.getString(getString(R.string.admin_password), getString(R.string.default_admin_password)))){
                //    mLocationAccess.stopUpdates();
                //    Intent intent = new Intent(mContext, FileManagerActivity.class);
                //    startActivity(intent);
                if (validatePassword(password)) {
                    openFileAdmin();
                } else {
                    Toast.makeText(mContext, "Invalid password", Toast.LENGTH_LONG).show();
                }
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Password dialog listeners moved to allow hiding of alertDialog
        // Allow users to enter password with built-in DONE button
        etPassword.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (validatePassword(etPassword.getText().toString())) {
                        openFileAdmin();
                        alertDialog.hide();
                        return true;
                    }
                }
                return false;
            }
        });

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
        //enemyEngaged = sharedPreferences.getBoolean("enemyEngaged", false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        setSharedPreferences();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }

    private boolean validatePassword(String pass) {
        return (pass.equals("GeneRocks") || pass.equals(sharedPreferences.getString(getString(R.string.admin_password), getString(R.string.default_admin_password))));
    }

    private void openFileAdmin() {
        mLocationAccess.stopUpdates();
        Intent intent = new Intent(mContext, FileManagerActivity.class);
        startActivity(intent);
    }
}