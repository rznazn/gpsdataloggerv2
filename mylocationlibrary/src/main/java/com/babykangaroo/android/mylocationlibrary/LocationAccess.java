package com.babykangaroo.android.mylocationlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Created by sport on 7/12/2017.
 */

public class LocationAccess {
    private FusedLocationProviderClient mLocationClient;
    private Context mContext;
    private Location mLocation;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 9999;

    /**
     * @param context of the instantiating activity
     */
    public LocationAccess(Context context){
        mContext = context;
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

        if (ActivityCompat.checkSelfPermission( mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.getLastLocation()
                    .addOnSuccessListener((Activity) mContext, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mLocation = location;
                            }
                        }
                    });
        }

    }

    public Location getLastKnownLocation(){

        return mLocation;
    }
}
