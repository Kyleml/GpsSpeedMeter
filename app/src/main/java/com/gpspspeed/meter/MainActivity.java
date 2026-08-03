package com.gpspspeed.meter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvSpeed;
    private TextView tvGpsStatus;
    private LocationManager locationManager;
    private GpsStatus.Listener gpsStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSpeed = findViewById(R.id.tv_speed);
        tvGpsStatus = findViewById(R.id.tv_gps_status);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                return;
            }
        }
        startGps();
    }

    private void startGps() {
        try {
            // 每秒请求一次GPS位置更新
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,   // 1秒更新一次
                    0,      // 最小距离0米
                    locationListener
            );

            // 监听GPS状态（卫星数量）
            gpsStatusListener = new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    updateGpsStatus();
                }
            };
            locationManager.addGpsStatusListener(gpsStatusListener);

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                // speed 单位是 m/s，转换为 km/h
                float speedMs = location.getSpeed();
                float speedKmh = speedMs * 3.6f;
                tvSpeed.setText(String.format("%.0f", speedKmh));
            }
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "请开启GPS", Toast.LENGTH_SHORT).show();
        }
    };

    private void updateGpsStatus() {
        try {
            GpsStatus gpsStatus = locationManager.getGpsStatus(null);
            int satellites = 0;
            int usedInFix = 0;
            for (GpsSatellite sat : gpsStatus.getSatellites()) {
                satellites++;
                if (sat.usedInFix()) usedInFix++;
            }
            final int finalUsed = usedInFix;
            final int finalTotal = satellites;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvGpsStatus.setText(String.format("GPS信号: %d / %d 颗卫星", finalUsed, finalTotal));
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGps();
        } else {
            Toast.makeText(this, "需要GPS权限才能使用", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            if (gpsStatusListener != null) {
                locationManager.removeGpsStatusListener(gpsStatusListener);
            }
        }
    }
}
