package com.weshine.trackrecord;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;

public class TrackRecordService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "WS_TrackService";

    // Google API用戶端物件
    private GoogleApiClient googleApiClient;
    // Location請求物件
    private LocationRequest locationRequest;
    // 記錄目前最新的位置
    private Location currentLocation;

    private DatabaseReference databaseReference;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TrackRecordService建立");

        configGoogleApiClient();
        configLocationRequest();
        setFireBase();

        // 連線到Google API用戶端
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        setForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "LoactionHistoryService執行");

        // 連線到Google API用戶端
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 移除位置請求服務
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }

        // 移除 Google API用戶端連線
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 建立Google API用戶端物件
    private synchronized void configGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.i(TAG, "建立GoogleApiClient");
    }


    /**
     * 建立前台通知
     *
     * @return
     */
    private Notification setForegroundNotification() {
        return setForegroundNotification("偉迅科技");
    }

    /**
     * 建立前台通知
     *
     * @return
     */
    private Notification setForegroundNotification(String content) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.map_marker48)
                .setContentTitle("定位紀錄系統")
                .setContentText(content)
                .setContentInfo("點擊觀看")
                .setWhen(System.currentTimeMillis());

        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        return mBuilder.build();
    }

    private void setForegroundService() {
        startForeground(110, setForegroundNotification());
    }

    private void updateForegroundService(LocationInfo locationInfo) {
        String info = "經度: " + locationInfo.getLongitude() + "  緯度: " + locationInfo.getLatitude();
        startForeground(110, setForegroundNotification(info));
    }


    /**
     * 建立Location請求物件
     */
    private void configLocationRequest() {
        locationRequest = new LocationRequest();
        // 設定讀取位置資訊的間隔時間為10秒
        locationRequest.setInterval(10000);
        // 設定讀取位置資訊最快的間隔時間為10秒(因為其他程式可能也會取得更新位置，所以要限制)，預設是 interval frequency的6倍
        locationRequest.setFastestInterval(10000);
        // 設定讀取位置資訊的最小間隔距離為5米，可以用來過濾不必要的更新
//        locationRequest.setSmallestDisplacement(5);
        // 設定優先讀取高精確度的位置資訊（GPS）
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        Log.i(TAG, "建立LocationRequest");
    }

    private void setFireBase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();   // 從哪一層開始
    }

    @Override

    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "已經連線到 Google Services");
        // 啟動位置更新服務，位置資訊更新的時候，應用程式會自動呼叫 LocationListener.onLocationChanged
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        Log.i(TAG, "開始監聽目前位置");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Google Services連線中斷");
        // int參數是連線中斷的代號
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Google Services連線失敗");
        // ConnectionResult參數是連線失敗的資訊
        int errorCode = connectionResult.getErrorCode();
        if (errorCode == ConnectionResult.SERVICE_MISSING) {
            Toast.makeText(this, "裝置沒有Google Play服務", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "位置改變 : " + location.toString());
        // Location參數是目前的位置
        currentLocation = location;

        LocationInfo locationInfo = new LocationInfo();
        locationInfo.setLatitude(location.getLatitude());
        locationInfo.setLongitude(location.getLongitude());
        locationInfo.setAccuracy((int) location.getAccuracy());
        locationInfo.setDatetime(new Date().getTime());
        databaseReference.push().setValue(locationInfo);

        updateForegroundService(locationInfo);


    }


}
