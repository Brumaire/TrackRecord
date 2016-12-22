package com.weshine.trackrecord;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "WS_MainActivity";

    // 定位設備授權請求代碼
    private static final int REQUEST_LOCATION_PERMISSION = 102;

    Button bt_map;
    Button bt_sql;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        checkLocationPermission();

        intView();
        setData();
    }


    private void intView() {
        bt_map = (Button) findViewById(R.id.bt_map);
        bt_sql = (Button) findViewById(R.id.bt_sql);

        bt_map.setOnClickListener(this);
        bt_sql.setOnClickListener(this);
    }

    private void setData() {
    }

    private void checkLocationPermission() {
        // 裝置的版本為android 6.0後續的版本才需要進行後續權限的處理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startAndBindService();
            } else {
                requestLocationPermission();
            }
        } else {
            startAndBindService();
        }
    }

    private void requestLocationPermission() {
        Log.i(TAG, "請求定位權限");

        /**
         * shouldShowRequestPermissionRationale 回傳的規則如下：
         * 1.true:用戶在先前權限詢問的選項中，選擇了「拒絕」，但沒有勾選「不再詢問」
         * 2.false:第一次安裝APP後進入該功能
         * 3.false:用戶在先前權限詢問選項中，選擇了「允許」
         * 4.false:用戶在先前權限詢問的選項中，選擇了「拒絕」，但勾選「不再詢問」
         * 5.false:用戶在「設定」裡頭選擇「允許或拒絕」
         *
         * 呼叫requestPermissions後，是否會跳出詢問視窗，規則如下：
         * 1.不會：用戶在先前權限詢問選項中，選擇了「允許」
         * 2.不會：用戶在先前權限詢問的選項中，選擇了「拒絕」，但勾選「不再詢問」，在後續onRequestPermissionsResult當中一律回傳PERMISSION_DENIED
         * 3.會：第一次安裝APP後進入該功能(第一次跳出不會有「不再詢問」的選項可以勾選)
         * 4.會：用戶在先前權限詢問的選項中，選擇了「拒絕」
         * 5.會：用戶在「設定」裡頭選擇「允許或拒絕」
         *
         * 當呼叫requestPermissions後，不管用戶選擇允許或拒絕，又或者沒有跳出詢問視窗，後續都會進入onRequestPermissionsResult執行後續動作，規則如下：
         * 1.PERMISSION_GRANTED(允許)：用戶選擇「允許」選項、設定中直接選擇「允許」
         * 2.PERMISSION_DENIED(拒絕)：用戶選擇「拒絕」選項、設定中直接選擇「拒絕」
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i(TAG, "Displaying camera permission rationale to provide additional context.");

            // 說明提醒使用戶該功能必須允許該權限才可以使用
            Snackbar.make(findViewById(android.R.id.content), "必須允許權限", Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                }
            }).show();
        } else {
            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    /**
     * 啟動 Service
     */
    private void startAndBindService() {
        Intent intent = new Intent(MainActivity.this, TrackRecordService.class);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_map:
                startActivity(new Intent(this, MapsActivity.class));
                break;
            case R.id.bt_sql:
                break;
        }
    }
}
