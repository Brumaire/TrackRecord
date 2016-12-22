package com.weshine.trackrecord;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "WS_MapsActivity";

    private GoogleMap mMap;

    private List<LocationInfo> locationInfoList = new ArrayList<>();

    // 因為每次畫折線，都至少需要兩個點，所以必須儲存上次的最後一點，作為這次的起始點
    private LatLng startLatLng;

    // 是否把鏡頭移至目前位置
    private boolean isGoCurrent = false;

    // 顯示目前與儲存位置的標記物件
    private Marker currentMarker, itemMarker;

    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setFireBase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        databaseReference.removeEventListener(childEventListener);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void setFireBase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();   // 從哪一層開始

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                LocationInfo locationInfo = dataSnapshot.getValue(LocationInfo.class);
                locationInfo.setId(dataSnapshot.getKey());
                Log.i(TAG, "取得一筆新資料" + locationInfo.getId() + ", " + locationInfo.getDatetime());

                locationInfoList.add(locationInfo);

                LatLng latLng = new LatLng(locationInfo.getLatitude(), locationInfo.getLongitude());

                if (!isGoCurrent) {
                    moveMap(latLng, 20, false);
                    isGoCurrent = true;
                }

                if (startLatLng != null) {
                    drawPolyline(Color.BLUE, 5, startLatLng, latLng);
                }
                startLatLng = latLng;
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
        };

        databaseReference.addChildEventListener(childEventListener);

        /*
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locationInfoList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    LocationInfo locationInfo = ds.getValue(LocationInfo.class);
                    locationInfo.setId(ds.getKey());
                    locationInfoList.add(locationInfo);
                }

                Collections.sort(locationInfoList, new Comparator<LocationInfo>() {
                    @Override
                    public int compare(LocationInfo o1, LocationInfo o2) {
                        return (int) (o1.getDatetime() - o2.getDatetime());
                    }
                });


                if (!locationInfoList.isEmpty()) {
                    // 取得最後一筆資料，表示目前的位置
                    LocationInfo lastLocationInfo = locationInfoList.get(locationInfoList.size() - 1);
                    LatLng currentLatLng = new LatLng(lastLocationInfo.getLatitude(), lastLocationInfo.getLongitude());
                    moveMap(currentLatLng, 20, false);
                    isGoCurrent = true;
                    startLatLng = currentLatLng;

                    // 取得所有經緯度
                    List<LatLng> latLngList = new ArrayList<>();
                    for (LocationInfo locationInfo : locationInfoList) {
                        latLngList.add(new LatLng(locationInfo.getLatitude(), locationInfo.getLongitude()));
                    }
                    drawPolyline(Color.BLUE, 5, latLngList);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
        */


    }

    /**
     * 移動地圖到參數指定的位置
     *
     * @param place   位置
     * @param zoom    縮放(1:最遠，21:最近)
     * @param animate 是否使用動畫
     */
    private void moveMap(LatLng place, float zoom, boolean animate) {
        // 建立地圖攝影機的位置物件
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(place)
                .zoom(zoom)
                .build();

        if (animate) {
            // 使用動畫的效果移動地圖
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    /**
     * 在地圖上畫折線
     */
    private void drawPolyline(int color, float width, LatLng... latLngs) {
        drawPolyline(color, width, Arrays.asList(latLngs));
    }

    /**
     * 在地圖上畫折線
     *
     * @param color    折線顏色
     * @param width    折線寬度
     * @param iterable 容器
     */
    private void drawPolyline(int color, float width, Iterable<LatLng> iterable) {
        // 建立折線
        PolylineOptions polylineOptions = new PolylineOptions().geodesic(true)
                .color(color)
                .width(width)
                .addAll(iterable);

        // 繪製折線
        Polyline polyline = mMap.addPolyline(polylineOptions);
    }

    // 在地圖加入指定位置與標題的標記
    private void addMarker(LatLng place, String title, String context) {
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.map_marker48);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(place)
                .title(title)
                .snippet(context)
                .icon(icon);

        mMap.addMarker(markerOptions);
    }
}
