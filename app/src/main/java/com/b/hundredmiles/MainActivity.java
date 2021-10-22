package com.b.hundredmiles;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private LocationManager locationManager;
    private String location_provider;
    private Boolean keep_simulating;
    private Handler handler;
    private AMap aMap;
    private List<Marker> markers;
    private List<LatLng> latLngs;
    private int sum_marker;
    private LatLng latLng_onclick;
    private Marker marker_onclick;
    private Marker marker_choosen;
    private Polyline polyline;

    private Button button_simulate;
    private Button button_add_point;
    private Button button_delete_point;
    private Button button_clear_point;
    private Button button_goto_point;
    private EditText edittext_speed;
    private EditText edittext_latitude;
    private EditText edittext_longitude;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(new uncaught_exception_handler());

        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
        ActivityCompat.requestPermissions(this, permissions, 1);

        keep_simulating = true;
        handler = new Handler();
        markers = new ArrayList<>();
        latLngs = new ArrayList<LatLng>();
        sum_marker = 0;
        location_provider = LocationManager.GPS_PROVIDER;

        button_simulate = findViewById(R.id.button_simulate);
        button_add_point = findViewById(R.id.button_add_point);
        button_delete_point = findViewById(R.id.button_delete_point);
        button_clear_point = findViewById(R.id.button_clear_point);
        button_goto_point = findViewById(R.id.button_goto_point);
        edittext_speed = findViewById(R.id.edittext_speed);
        edittext_latitude = findViewById(R.id.edittext_latitude);
        edittext_longitude = findViewById(R.id.edittext_longitude);

        //获取地图控件引用
        mapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);

        if (aMap == null) {
            aMap = mapView.getMap();
        }

        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(1000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER));//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_CENTER);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.setOnMapClickListener(new map_onclick_handler());
        aMap.setOnInfoWindowClickListener(new infowindow_onclick_handler());
        aMap.setOnMarkerClickListener(new marker_onclick_handler());
        aMap.setOnMapTouchListener(new map_touch_handler());
        aMap.setOnMarkerDragListener(new marker_drag_handler());


//        try {
//            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//            if(locationManager.getProvider(location_provider)==null){
//                locationManager.addTestProvider(location_provider, false, false, false, false, true, true, true, 0, 5);
//            }
//            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
//        } catch (SecurityException e) {
//            print("请设置模拟位置信息应用");
//            button_simulate.setOnClickListener(this::do_nothing);
//        }

        try {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.getProvider(location_provider) == null) {
                locationManager.addTestProvider(location_provider, false, false, false, false, true, true, true, 0, 5);
            }
            locationManager.setTestProviderEnabled(location_provider, true);
        } catch (SecurityException e) {
            print("请设置模拟位置信息应用并重启应用");
            button_simulate.setOnClickListener(this::do_nothing);
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void start_simulation(View view) {
        if (sum_marker == 0) {
            print("请先设置路径点");
            return;
        }

        for (int i = 0; i < sum_marker; i++) {
            markers.get(i).setDraggable(false);
        }

        keep_simulating = true;
        button_simulate.setText(R.string.button_stop);
        button_simulate.setOnClickListener(this::stop_simulation);

        button_add_point.setOnClickListener(this::do_nothing);
        button_delete_point.setOnClickListener(this::do_nothing);
        button_clear_point.setOnClickListener(this::do_nothing);
        button_goto_point.setOnClickListener(this::do_nothing);

        Double speed = Double.valueOf(String.valueOf(edittext_speed.getText()));

        List<Double> point_latitudes = new ArrayList<>();
        List<Double> point_longitudes = new ArrayList<>();
        for (int i = 0; i < sum_marker - 1; i++) {
            Double distance = (double) AMapUtils.calculateLineDistance(latLngs.get(i), latLngs.get(i + 1)) / (double) 1000;
            int num_step = (int) (distance / speed);
            double tem_latitude = latLngs.get(i).latitude;
            double tem_longitude = latLngs.get(i).longitude;
            double d_latitude = (latLngs.get(i + 1).latitude - latLngs.get(i).latitude) / (double) num_step;
            double d_longitude = (latLngs.get(i + 1).longitude - latLngs.get(i).longitude) / (double) num_step;
            for (int j = 0; j < num_step; j++) {
                point_latitudes.add(tem_latitude);
                point_longitudes.add(tem_longitude);
                tem_latitude += d_latitude;
                tem_longitude += d_longitude;
            }
        }
        point_latitudes.add(latLngs.get(latLngs.size() - 1).latitude);
        point_longitudes.add(latLngs.get(latLngs.size() - 1).longitude);
        int sum_point = point_latitudes.size();

        int[] num_point = new int[]{0};
        int[] direction = new int[]{1};
        Thread simulate_thread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void run() {
                while (keep_simulating) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if ((num_point[0] == sum_point - 1 && direction[0] == 1) || (num_point[0] == 0 && direction[0] == -1)) {
                        direction[0] = -direction[0];
                    } else {
                        num_point[0] += direction[0];
                    }
                    set_location(point_latitudes.get(num_point[0]), point_longitudes.get(num_point[0]), 0, 5);
                }
            }
        });
        simulate_thread.start();
    }

    public void stop_simulation(View view) {
        for (int i = 0; i < sum_marker; i++) {
            markers.get(i).setDraggable(true);
        }

        keep_simulating = false;
        button_simulate.setText(R.string.button_start);
        button_simulate.setOnClickListener(this::start_simulation);

        button_add_point.setOnClickListener(this::add_point);
        button_delete_point.setOnClickListener(this::delete_point);
        button_clear_point.setOnClickListener(this::clear_point);
        button_goto_point.setOnClickListener(this::goto_point);
    }

    public void delete_point(View view) {
        if (marker_choosen == null) {
            return;
        }
        int point_index = Integer.parseInt(marker_choosen.getSnippet());
        latLngs.remove(point_index - 1);
        for (int i = point_index; i < sum_marker; i++) {
            markers.get(i).setSnippet(String.valueOf(i));
        }
        marker_choosen.remove();
        markers.remove(point_index - 1);
        draw_polyline();
        sum_marker--;
    }

    public void clear_point(View view) {
        if (sum_marker == 0) {
            return;
        }
        latLngs.clear();
        for (int i = 0; i < sum_marker; i++) {
            markers.get(i).remove();
        }
        markers.clear();
        marker_choosen = null;
        polyline.remove();
        sum_marker = 0;
    }

    public void goto_point(View view) {
        latLng_onclick = new LatLng(Double.parseDouble(String.valueOf(edittext_latitude.getText())), Double.parseDouble(String.valueOf(edittext_longitude.getText())));
        if (marker_onclick == null) {
            marker_onclick = aMap.addMarker(new MarkerOptions().position(latLng_onclick));
        } else {
            marker_onclick.setPosition(latLng_onclick);
        }
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(latLng_onclick, aMap.getCameraPosition().zoom, 0, 0));
        aMap.moveCamera(cameraUpdate);
    }

    public void help(View view) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public void do_nothing(View view) {
        //do nothing
    }

    class map_touch_handler implements AMap.OnMapTouchListener {
        @Override
        public void onTouch(MotionEvent motionEvent) {
            if (marker_choosen != null) {
                marker_choosen.hideInfoWindow();
            }
            marker_choosen = null;
        }
    }

    class map_onclick_handler implements AMap.OnMapClickListener {
        @Override
        public void onMapClick(LatLng latLng) {
            latLng_onclick = latLng;
            set_position_edittext(latLng);
            if (marker_onclick == null) {
                marker_onclick = aMap.addMarker(new MarkerOptions().position(latLng_onclick));
            } else {
                marker_onclick.setPosition(latLng_onclick);
            }
        }
    }

    class infowindow_onclick_handler implements AMap.OnInfoWindowClickListener {
        @Override
        public void onInfoWindowClick(Marker marker) {
            marker.hideInfoWindow();
        }
    }

    class marker_onclick_handler implements AMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker.getSnippet() != null) {
                marker_choosen = marker;
                if (!marker.isInfoWindowShown()) {
                    marker.showInfoWindow();
                } else {
                    marker.hideInfoWindow();
                }
            }
            return false;
        }
    }

    class marker_drag_handler implements AMap.OnMarkerDragListener {

        @Override
        public void onMarkerDragStart(Marker marker) {

        }

        @Override
        public void onMarkerDrag(Marker marker) {
            LatLng latLng = marker.getPosition();
            int index = Integer.valueOf(marker.getSnippet()) - 1;
            markers.set(index, marker);
            latLngs.set(index, latLng);
            draw_polyline();
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {

        }
    }

    private void draw_polyline() {
        if (polyline != null) {
            polyline.remove();
        }
        polyline = aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(5).setDottedLine(true));
    }

    private void set_position_edittext(LatLng latLng) {
        edittext_latitude.setText(String.valueOf(latLng.latitude));
        edittext_longitude.setText(String.valueOf(latLng.longitude));
    }

    public void add_point(View view) {
        Marker marker = aMap.addMarker(new MarkerOptions().position(latLng_onclick).title("路径点").snippet(String.valueOf(++sum_marker)));
        marker.setDraggable(true);
        markers.add(marker);

        latLngs.add(latLng_onclick);
        draw_polyline();
    }

    private void print(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void set_location(double latitude, double longitude, double altitude, float accuracy) {
        Location location = new Location(location_provider);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude + 0.0020);
        location.setLongitude(longitude - 0.0055);
        location.setAltitude(altitude);
        location.setAccuracy(accuracy);
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        locationManager.setTestProviderLocation(location_provider, location);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    private void send_to_clipboard(String message) {
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("message", message);
        clipboardManager.setPrimaryClip(clipData);
    }

    class uncaught_exception_handler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            send_to_clipboard(e.toString());
            finish();
        }
    }
}