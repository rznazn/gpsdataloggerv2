package com.babykangaroo.android.mylocationlibrary;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by sport on 7/12/2017.
 */

public abstract class LocationAccess extends Service implements SensorEventListener{

    public Context mContext;

    /**
     * location service variables
     */
    private FusedLocationProviderClient mLocationClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 9999;
    public static final String SERVICE_CHANNEL_ID = "GPSDataLogger";

    /**
     * variables for location aspects
     */
    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private long mGPSTime;
    private long mGPSTimeOffset;
    private double mAccuracy;
    private float mBearing;
    private double mSpeed;
    private LocationUpdateListener mLocationUpdateListener;

    /**
     * Variables for compass
     */
    private SensorManager mSensorManager;

    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];

    private GeomagneticField mGeoMagField;

    private Sensor mSensorGravity;
    private Sensor mSensorMagnetic;
    private double mBearingMagnetic = 0.0;


    /**
     * interface to perform action on location update
     */
    public interface LocationUpdateListener{
        void onLocationUpdate(Location location);
        void onAzimuthChange(double azimuth);
    }


    /**
     * public constructor. instantiate this object in the required activity
     * all location services are handled in this object and data can be called with the getter methods defined below
     * @param context of the instantiating activity
     */
    public LocationAccess(){}
    public LocationAccess(Context context, @Nullable LocationUpdateListener locationUpdateListener){
        mContext = context;
        if (locationUpdateListener != null) {
            this.mLocationUpdateListener = locationUpdateListener;
        }


        /**
         * initiate the FusedLocationProviderClient that will provide the last known locations.
         */
        mLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        /**
         * request location permission
         */
        ActivityCompat.requestPermissions((Activity) mContext,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);

        /**
         * initialize LocationCallback
         * define what to do when the locationrequest receives a reply
         */
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    long time = System.currentTimeMillis();
                    //call interface if established
                    if (mLocationUpdateListener != null){mLocationUpdateListener.onLocationUpdate(location);}
                    mLocation = location;
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    mAltitude = location.getAltitude();
                    mGPSTime = location.getTime();
                    mGPSTimeOffset = time - mGPSTime;
                    mAccuracy = location.getAccuracy();
                    mBearing = location.getBearing();
                    mSpeed = location.getSpeed();
                    mGeoMagField = new GeomagneticField(Double.valueOf(mLatitude).floatValue(),
                            Double.valueOf(mLongitude).floatValue(),
                            Double.valueOf(location.getAltitude()).floatValue(),time);
                }
            }
        };

        /**
         * create location request
         */

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /**
         * set the sensors and sensor manager
         */
        mSensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        else
        {
            Toast.makeText(mContext, "No Accelerometer in this Phone", Toast.LENGTH_LONG).show();
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        else {
            Toast.makeText(mContext, "No Magnometer in this Phone", Toast.LENGTH_LONG).show();
        }
        mSensorManager.registerListener(this, mSensorMagnetic,
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorGravity,
                SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * begin recieving location updates as defined by mLocationRequest
     */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission( mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
        }
    }

    /**
     * stop updates
     */

    protected void stopUpdates(){
        mLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * @param event from the sensor manager gets sorted by type and then recalculates bearing
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }

        SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic);
        SensorManager.getOrientation(rotation, orientation);
        double ori = 0;
        ori = orientation[0];
        double degrees = 0;
        degrees = Math.toDegrees(ori);
        double rounded = 0;
        rounded = Math.round(degrees);

        mBearingMagnetic = rounded;

        /**
         * adjust for declination
         */
        if (mGeoMagField != null) {
            mBearingMagnetic += mGeoMagField.getDeclination();
        }

        /**
         * set mBearing to be 0 - 360
         */
        if (mBearingMagnetic < 0) {
            mBearingMagnetic += 360;
        }
        mLocationUpdateListener.onAzimuthChange(mBearingMagnetic);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    /**
     * getter methods
     */
    public Location getLastKnownLocation(){return mLocation;}

    public double getmLatitude(){return mLatitude;}
    public double getmLongitude(){return mLongitude;}
    public double getmAltitude(){return mAltitude;}
    public long getmGPSTime(){return mGPSTime;} //of the last received location not current gps time

    /**GPSTimeOffset should be subtracted from actual time [System.getCurrentTimeMillis()] to correct difference
     * this correction might not be exact, but should be enough to correct differences greater than a second
     */
    public long getmGPSTimeOffset(){return mGPSTimeOffset;}
    public double getmAccuracy(){return mAccuracy;}
    public float getmBearing(){return mBearing;}
    public double getmBearingMagnetic(){return mBearingMagnetic;}
    public double getmSpeed(){return mSpeed;}
}
