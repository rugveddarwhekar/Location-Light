package com.example.locationlight;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import github.nisrulz.easydeviceinfo.base.EasyConfigMod;


public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 2*1000; //Delay for 2 seconds.  One second = 1000 milliseconds.
    private static final String TAG="MainActivity";
    //Sensors
    Sensor light;
    SensorManager sensorManager;
    float light_value;
    String s_latitude, s_longitude, s_date, s_time, s_light;
    private Location location;
    private TextView locationTv, lightTv;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 1000, FASTEST_INTERVAL = 1000; // = 5 seconds
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    String shadowType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationTv = findViewById(R.id.location);
        lightTv = findViewById(R.id.light);

        // we add permissions we need to request location of the users
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
//        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
//        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }
        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
    }

//    RadioGroup rg = (RadioGroup) findViewById(R.id.radio_group);
//
//    rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
//        public void onCheckedChanged(RadioGroup group, int checkedId) {
//        switch(checkedId){
//            case R.id.radio_tree:
//                shadowType = "Tree";
//                // do operations specific to this selection
//                break;
//            case R.id.radio_building:
//                shadowType = "Building";
//                // do operations specific to this selection
//                break;
//            case R.id.radio_other:
//                shadowType = "Other";
//                // do operations specific to this selection
//                break;
//            case R.id.radio_no_shadow:
//                shadowType = "No";
//                // do operations specific to this selection
//                break;
//        }
//    }
//    });

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();
        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    public void getData() throws IOException {
        EasyConfigMod easyConfigMod = new EasyConfigMod(this);
//        tv_date.setText(String.valueOf(easyConfigMod.getCurrentDate()));
        s_date = String.valueOf(easyConfigMod.getCurrentDate());
        Log.d(TAG, "getData: Date: " + s_date);
//        tv_time.setText(String.valueOf(easyConfigMod.getFormattedTime()));
        s_time = easyConfigMod.getFormattedTime();
        Log.d(TAG, "getData: Time: " + s_time);
        Log.d(TAG, "getData: Latitude: " + s_latitude);
        Log.d(TAG, "getData: Longitude: " + s_longitude);
        Log.d(TAG, "getData: Light: " + s_light);

        //---Adding data to CSV File----
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "dataForProject.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File("/"+filePath);
        CSVWriter writer;
        // File exist
        if(f.exists()&&!f.isDirectory())
        {
            FileWriter mFileWriter = new FileWriter(filePath, true);
            writer = new CSVWriter(mFileWriter);
        }
        else
        {
            String[] header = {"Date", "Time", "Latitude", "Longitude", "Luminosity"};
            writer = new CSVWriter(new FileWriter(filePath));
            writer.writeNext(header);
        }
        //Date, Time, Latitude, Longitude, Light
        String[] data = {s_date, s_time, s_latitude, s_longitude, s_light};
        writer.writeNext(data);
        writer.close();
        //-------End of CSV Code------Rugved--------
    }

    @Override
    protected void onResume() {
        handler.postDelayed( runnable = new Runnable() {
            public void run() {
                //do something
                try {
                    getData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(runnable, delay);
            }
        }, delay);

        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        if (!checkPlayServices()) {
            locationTv.setText("You need to install Google Play Services to use the App properly");
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        sensorManager.unregisterListener(this);
        super.onPause();
        // stop location updates
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {
            locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            s_latitude = "" + location.getLatitude();
            // Log.d(TAG, "onConnected: Latitude: " + s_latitude);
            s_longitude = "" + location.getLongitude();
            //  Log.d(TAG, "onConnected: Longitude: " + s_longitude);
            locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();
                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            light_value = event.values[0];
            s_light = String.valueOf(light_value);
            // Log.d(TAG, "onConnected: Light: " + s_light);
            lightTv.setText("Light Sensor value: " + light_value + " lux");
           // Log.d(TAG, "onSensorChanged: " + light_value);
            //Toast.makeText(this, "" + event.values[0], Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}