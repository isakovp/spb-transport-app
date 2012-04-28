package com.emal.android.transport.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import com.emal.android.*;
import com.emal.android.transport.map.ExtendedMapView;
import com.emal.android.transport.map.MapOverlay;
import com.emal.android.transport.utils.Constants;
import com.emal.android.transport.utils.GeoConverter;
import com.emal.android.transport.map.MapUtils;
import com.google.android.maps.*;

import java.io.IOException;
import java.util.*;

/**
 * User: alexey.emelyanenko@gmail.com
 * Date: 4/15/12 9:34 PM
 */
public class MainActivity extends MapActivity {
    private static final String TAG = "MapActivity";
    private boolean showBus = false;
    private boolean showTrolley = false;
    private boolean showTram = false;
    private boolean satView = true;
    private int syncTime = Constants.DEFAULT_SYNC_MS;
    private int zoomSize;
    private GeoPoint homeLocation;
    private GeoPoint longPressedLocation;
    private ExtendedMapView mapView;
    private SharedPreferences sharedPreferences;
    private Handler mHandler = new Handler();
    private TimerTask timerTask = new MapUpdateTimerTask();
    private VehicleTracker vehicleTracker;
    private MyLocationOverlay mylocationOverlay;
    private LocationManager locationManager;
    private AlertDialog alert;
    private Address myPlace;

    public class MapUpdateTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "START Timer Update " + Thread.currentThread().getName() + " with time " + syncTime);
            vehicleTracker.syncAll();
            mHandler.postDelayed(this, syncTime);
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mapView = (ExtendedMapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.displayZoomControls(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mylocationOverlay = new MyLocationOverlay(mapView.getContext(), mapView);
        mylocationOverlay.enableMyLocation();
        mapView.getOverlays().add(mylocationOverlay);

        initParams();
        alert = MapUtils.createMyPlaceDialog(mapView,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        homeLocation = longPressedLocation;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        float latitude = (float) (homeLocation.getLatitudeE6() / 1E6);
                        float longtitude = (float) (homeLocation.getLongitudeE6() / 1E6);
                        editor.putFloat(Constants.HOME_LOC_LAT_FLAG, latitude);
                        editor.putFloat(Constants.HOME_LOC_LONG_FLAG, longtitude);
                        editor.commit();
                        MapUtils.redrawMyPlace(mapView, homeLocation);
                        dialog.cancel();

                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }
        );
        vehicleTracker = new VehicleTracker(mapView);
        mapView.setVehicleTracker(vehicleTracker);
        mapView.getController().setZoom(zoomSize);
        mapView.setOnLongpressListener(createLongPressListener());
        moveToCurrentLocation();
        MapUtils.addMyPlace(mapView, homeLocation);
    }

    private ExtendedMapView.OnLongpressListener createLongPressListener() {
        return new ExtendedMapView.OnLongpressListener() {
            public void onLongpress(final MapView view, final GeoPoint geoPoint) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "Long press. Point " + geoPoint);
                        longPressedLocation = geoPoint;
                        float latitude = (float) (longPressedLocation.getLatitudeE6() / 1E6);
                        float longtitude = (float) (longPressedLocation.getLongitudeE6() / 1E6);

                        Geocoder geo = new Geocoder(view.getContext());
                        try {
                            List<Address> myAddrs = geo.getFromLocation(latitude, longtitude, 1);
                            if (myAddrs.size() > 0) {
                                myPlace = myAddrs.get(0);
                                Log.d(TAG, "My Place selected: " + GeoConverter.convert(myPlace));

                                String msg = getResources().getString(R.string.addmyplace, GeoConverter.convert(myPlace));
                                alert.setMessage(msg);
                                alert.show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

    private void initParams() {
        sharedPreferences = getSharedPreferences(Constants.APP_SHARED_SOURCE, 0);
        showBus = sharedPreferences.getBoolean(Constants.SHOW_BUS_FLAG, true);
        showTrolley = sharedPreferences.getBoolean(Constants.SHOW_TROLLEY_FLAG, true);
        showTram = sharedPreferences.getBoolean(Constants.SHOW_TRAM_FLAG, true);
        syncTime = sharedPreferences.getInt(Constants.SYNC_TIME_FLAG, Constants.DEFAULT_SYNC_MS);

        Float homeLat = sharedPreferences.getFloat(Constants.HOME_LOC_LAT_FLAG, 59.95f);
        Float homeLong = sharedPreferences.getFloat(Constants.HOME_LOC_LONG_FLAG, 30.316667f);
        homeLocation = new GeoPoint((int) (homeLat * 1E6), (int) (homeLong * 1E6));

        satView = sharedPreferences.getBoolean(Constants.SAT_VIEW_FLAG, false);
        zoomSize = sharedPreferences.getInt(Constants.ZOOM_FLAG, Constants.DEFAULT_ZOOM_LEVEL);
    }

    private void restoreApplicationState() {
        if (showBus) {
            trackVehicle(Vehicle.BUS);
        } else {
            untrackVehicle(Vehicle.BUS);
        }
        if (showTrolley) {
            trackVehicle(Vehicle.TROLLEY);
        } else {
            untrackVehicle(Vehicle.TROLLEY);
        }
        if (showTram) {
            trackVehicle(Vehicle.TRAM);
        } else {
            untrackVehicle(Vehicle.TRAM);
        }
        mapView.setSatellite(satView);
        mHandler.postDelayed(timerTask, 0);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, mylocationOverlay);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        initParams();
        restoreApplicationState();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(timerTask);
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHandler.removeCallbacks(timerTask);
        locationManager.removeUpdates(mylocationOverlay);
        super.onStop();
    }

    private void moveToCurrentLocation() {
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        GeoPoint currentPoint = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
        MapController controller = mapView.getController();
        controller.animateTo(currentPoint, new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(timerTask, 0);
            }
        });
    }

    private void moveToHomeLocation() {
        MapController controller = mapView.getController();
        controller.animateTo(homeLocation, new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(timerTask, 0);
            }
        });
        controller.setZoom(zoomSize);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    public void trackVehicle(Vehicle vehicle) {
        Log.d(TAG, "Track " + vehicle);
        vehicleTracker.track(vehicle);
        MapOverlay mapOverlay = new MapOverlay(vehicle, vehicleTracker);
        List<Overlay> overlays = mapView.getOverlays();
        for (Overlay overlay : overlays) {
            if (overlay instanceof MapOverlay) {
                Vehicle v = ((MapOverlay) overlay).getVehicle();
                if (v.equals(vehicle)) {
                    Log.d(TAG, "vehicle exist " + vehicle);
                    return;
                }
            }
        }
        overlays.add(mapOverlay);
        mapView.invalidate();
    }

    public void untrackVehicle(Vehicle vehicle) {
        Log.d(TAG, "Untrack " + vehicle);
        vehicleTracker.untrack(vehicle);

        List<Overlay> mapOverlays = mapView.getOverlays();
        Iterator<Overlay> iterator = mapOverlays.iterator();
        while (iterator.hasNext()) {
            Overlay overlay = iterator.next();
            if (overlay instanceof MapOverlay) {
                Vehicle vehicle1 = ((MapOverlay) overlay).getVehicle();
                if (vehicle1.equals(vehicle)) {
                    iterator.remove();
                    mapView.invalidate();
                    //break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cur_location: {
                moveToCurrentLocation();
                break;
            }
            case R.id.home_location: {
                moveToHomeLocation();
                break;
            }
            case R.id.settings: {
                startActivity(new Intent(this, TransportSettings.class));
                break;
            }
        }
        return true;
    }
}