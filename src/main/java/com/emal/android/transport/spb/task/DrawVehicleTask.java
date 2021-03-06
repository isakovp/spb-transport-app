package com.emal.android.transport.spb.task;

import android.graphics.*;
import android.os.AsyncTask;
import android.util.Log;
import com.emal.android.transport.spb.VehicleSyncAdapter;
import com.emal.android.transport.spb.VehicleType;
import com.emal.android.transport.spb.portal.*;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;

import java.util.*;

/**
 * @author alexey.emelyanenko@gmail.com
 * @since: 1.5
 */
public class DrawVehicleTask extends AsyncTask<Object, Void, List<Vehicle>> {
    private static final String TAG = DrawVehicleTask.class.getName();
    private Route route;
    private PortalClient portalClient;
    private GoogleMap mMap;
    private Map<String, Marker> markers;
    private VehicleSyncAdapter vehicleSyncAdapter;

    public DrawVehicleTask(Route route, PortalClient portalClient, GoogleMap mMap, Map<String, Marker> markers, VehicleSyncAdapter vehicleSyncAdapter) {
        this.route = route;
        this.portalClient = portalClient;
        this.mMap = mMap;
        this.markers = markers;
        this.vehicleSyncAdapter = vehicleSyncAdapter;
    }

    @Override
    protected void onPreExecute() {
        vehicleSyncAdapter.beforeSync(false);
    }

    @Override
    protected List<Vehicle> doInBackground(Object... params) {
        List<Vehicle> list = null;
        try {
            Log.d(TAG, "Get vehicles fro route: " + route);
            VehicleCollection routeData = portalClient.getRouteData(route.getId(), PortalClient.BBOX);
            list = routeData.getList();
            Log.d(TAG, "Found vehicles size: " + list.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list != null ? list : Collections.EMPTY_LIST;
    }

    @Override
    protected void onPostExecute(List<Vehicle> vehicles) {
        Log.d(TAG, "Found new vehciles size: " + vehicles.size());
        Collection<Marker> routeMarkers = new ArrayList<Marker>(markers.values());
        Log.d(TAG, "Found markers " + routeMarkers.size() + " for route " + route);

        for (Vehicle v : vehicles) {
            Double latitude = v.getGeometry().getLatitude();
            Double longtitude = v.getGeometry().getLongtitude();

            LatLng homePoint = new LatLng(latitude, longtitude);

            VehicleProps properties = v.getProperties();
            String stateNumber = properties.getStateNumber() + " @ " + properties.getVelocity() + " km/h";

            String routeLetter = "S";
            int routeColor = Color.YELLOW;
            VehicleType transportType = route.getTransportType();
            if (VehicleType.TROLLEY.equals(transportType)) {
                routeLetter = "U";
                routeColor = Color.GREEN;
            } else if (VehicleType.BUS.equals(transportType)) {
                routeLetter = "A";
                routeColor = Color.BLUE;
            } else if (VehicleType.TRAM.equals(transportType)) {
                routeLetter = "T";
                routeColor = Color.RED;
            }

            Bitmap bitmap = getVehicleBitmap(route.getRouteNumber(), properties, routeLetter, routeColor);

            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            MarkerOptions title = new MarkerOptions()
                    .position(homePoint)
                    .anchor(0.5f, 0.5f)
                    .icon(bitmapDescriptor)
                    .title(stateNumber);

            String vehId = v.getId();
            Log.d(TAG, "Add new marker for vehicle: " + vehId);
            Marker newMarker = mMap.addMarker(title);
            Marker oldMarker = markers.get(vehId);
            if (oldMarker != null) {
                Log.d(TAG, "Found old marker for vehicle: " + vehId);
                oldMarker.remove();
                routeMarkers.remove(oldMarker);
            }
            markers.put(vehId, newMarker);
        }

        Log.d(TAG, "Remainig markers " + routeMarkers.size() + " for route " + route);
        for (Marker routeMarker : routeMarkers) {
            routeMarker.remove();
        }

        vehicleSyncAdapter.afterSync(true);
    }

    private static Bitmap getVehicleBitmap(String routeNumber, VehicleProps properties, String routeLetter, int routeColor) {
        int bHeigth = 80;
        int bWidth = 80;
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(bWidth, bHeigth, conf);

        Paint vehiclePaint = new Paint();
        vehiclePaint.setTextSize(30);
        vehiclePaint.setColor(Color.WHITE);
        vehiclePaint.setTypeface(Typeface.DEFAULT_BOLD);
        vehiclePaint.setTextAlign(Paint.Align.CENTER);

        Paint vehicleNumberPaint = new Paint();
        vehicleNumberPaint.setTextSize(25);
        vehicleNumberPaint.setColor(Color.BLACK);
        vehicleNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
        vehicleNumberPaint.setTextAlign(Paint.Align.CENTER);

        Paint rectPaint = new Paint();
        rectPaint.setColor(routeColor);
        rectPaint.setStyle(Paint.Style.FILL);

        Canvas canvas = new Canvas(bitmap);
        int x = canvas.getClipBounds().centerX();
        int y = canvas.getClipBounds().centerY();

        canvas.drawText(routeNumber, 20, 20, vehicleNumberPaint);
        canvas.save();
        canvas.rotate(properties.getDirection(), x, y);
        canvas.drawRect(x - 15, y + 20, x + 15, y - 20, rectPaint);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((vehiclePaint.descent() + vehiclePaint.ascent()) / 2)) ;

        canvas.drawText(routeLetter, xPos, yPos, vehiclePaint);
        canvas.restore();
        return bitmap;
    }
}
