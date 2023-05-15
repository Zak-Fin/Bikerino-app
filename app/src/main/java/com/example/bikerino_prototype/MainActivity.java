package com.example.bikerino_prototype;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity  implements SensorEventListener{
    protected SensorManager sensorManager;

    protected Sensor accelerometerSensor;
    protected Sensor gyroscopeSensor;
    protected Sensor pressureSensor;
    protected TextView accelerometerValuesTextView;
    protected TextView gyroscopeValuesTextView;
    protected TextView pressureValuesTextView;
    protected String fileName;
    protected Boolean isCapturing = false;
    protected Button cycleButton;
    protected int light_purple;
    protected int dark_purple;
    protected TextView crash_message;
    protected TextView timerTextView;
    protected TextView EmergencyCall;
    protected Button crash_button;

    private CountDownTimer countDownTimer;
    private static final int REQUEST_SEND_SMS = 1;

    protected Timer timer;
    protected String currentLat;
    protected String currentLon;
    FusedLocationProviderClient mFusedLocationClient;

    // Initializing other items
    // from layout file
    int PERMISSION_ID = 44;
    String apiSample ="http://finmead.pythonanywhere.com/predict";
    protected EditText phoneNumber;
    protected EditText contactName;
    private static final String USER_AGENT = "Mozilla/5.0";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Initialize sensors
        // Check if accelerometer sensor is available
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor == null) {
            // Handle case when accelerometer sensor is not available
            Toast.makeText(this, "Accelerometer sensor not available", Toast.LENGTH_SHORT).show();
        }

        // Check if gyroscope sensor is available
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscopeSensor == null) {
            // Handle case when gyroscope sensor is not available
            Toast.makeText(this, "Gyroscope sensor not available", Toast.LENGTH_SHORT).show();
        }

        // Check if pressure sensor is available
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            // Handle case when pressure sensor is not available
            Toast.makeText(this, "Pressure sensor not available", Toast.LENGTH_SHORT).show();
        }

        cycleButton = findViewById(R.id.cycleButton);
        crash_message = findViewById(R.id.crash_message);
        timerTextView = findViewById(R.id.timerTextView);
        crash_button = findViewById(R.id.crash_button);
        EmergencyCall = findViewById(R.id.EmergencyCall);

        countDownTimer = new CountDownTimer(20000, 1000) {
            public void onTick(long millisUntilFinished) {
                // Update the UI to show the remaining time
                timerTextView.setText("Time remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                EmergencyCall.setVisibility(View.VISIBLE);
                crash_message.setVisibility(View.GONE);
                crash_button.setVisibility(View.GONE);
                timerTextView.setVisibility(View.GONE);
                getLastLocation();
                String msgText = String.format("BIKERINO - CRASH DETECTED %s has been injured: current location: https://maps.google.com/?q=%s,%s", contactName.getText().toString(), currentLat, currentLon);
                sendSms(phoneNumber.getText().toString(), msgText);
            }
        };
        // Initialize TextViews
        accelerometerValuesTextView = findViewById(R.id.accelerometerValuesTextView);
        light_purple = Color.rgb(187,134, 252);
        dark_purple = Color.rgb(120,86, 162);

        phoneNumber=findViewById(R.id.contactID);
        contactName=findViewById(R.id.userName);

// Check if we have permission to send SMS messages
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, ask the user for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQUEST_SEND_SMS);
        }
        getSensorData();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
    }

//    Code for location data from https://www.geeksforgeeks.org/how-to-get-user-location-in-android/
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (checkEnable()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        }
                        else{
                            System.out.println(location.getLatitude());
                            currentLat = location.getLatitude()+"";
                            currentLon = location.getLongitude()+"";
                        }

                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {

            requestPermissions();
        }
    }


    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
        }
    };
    private boolean checkEnable() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, send the SMS message
//                sendSms(phoneNumber, message);
            } else {
                // Permission denied, show an error message
                Toast.makeText(this, "Permission denied to send SMS messages", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (checkPermissions()) {
            getLastLocation();
        }
    }
    private void sendSms(String phone,String message) {
        System.out.println(message);
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the SensorEventListener to conserve resources
        sensorManager.unregisterListener(this);
    }

    public void okayFunc(View view){
        // Cancel the countdown timer
        countDownTimer.cancel();
        // Hide the elements
        crash_message.setVisibility(View.GONE);
        crash_button.setVisibility(View.GONE);
        timerTextView.setVisibility(View.GONE);
        accelerometerValuesTextView.setVisibility(View.VISIBLE);
        cycleButton.setVisibility(View.VISIBLE);
        phoneNumber.setVisibility(View.VISIBLE);
        contactName.setVisibility(View.VISIBLE);

    }
    public void onCycleClick(View view){
        System.out.println("something happening2");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        fileName = "cycle_" + timestamp + ".txt";

        String regEx = "^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$";
        Pattern pattern = Pattern.compile(regEx,Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(phoneNumber.getText().toString().trim());

        if(!matcher.matches() ){
            System.out.println(phoneNumber.getText().toString().trim());
            return ;
        }
        isCapturing = !isCapturing;
        if(isCapturing) {

            System.out.println("something happening");
            cycleButton.setText("Recording cycle...");
            cycleButton.setBackgroundColor(dark_purple);
            phoneNumber.setVisibility(View.GONE);
            contactName.setVisibility(View.GONE);
        }
        else{
            cycleButton.setText("Record cycle");
            cycleButton.setBackgroundColor(light_purple);
            phoneNumber.setVisibility(View.VISIBLE);
            contactName.setVisibility(View.VISIBLE);
            accelerometerValuesTextView.setVisibility(View.VISIBLE);}
    }



    public void doClassifier(ArrayList<String> accelerometerDataList,ArrayList<String> gyroscopeDataList,ArrayList<String> barometerDataList) throws IOException, JSONException {
        final boolean[] hasCrash = {false};

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        JSONObject test = new JSONObject();
        test.put("data", accelerometerDataList.toString());
//        new JSONObjectRequest requestBody = accelerometerDataList.toString();
        final String requestBody = test.toString();
        System.out.println(requestBody);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, apiSample,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {;
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String result = jsonObject.getString("result");
                            if (result.equals("['cycle']")) {
                                System.out.println("continue cycling");
                            } else if (result.equals("['crash']")) {
                                handleCrash();
                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error){
                System.out.println("Error");
            }
        }){
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
//                    System.out.println(response.data);
                    responseString = new String(response.data);
                    System.out.println(responseString);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        queue.add(stringRequest);


    }
    public void handleCrash(){
        isCapturing = !isCapturing;
        cycleButton.setText("Record cycle");
        cycleButton.setBackgroundColor(light_purple);
        crash_message.setVisibility(View.VISIBLE);
        crash_button.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);

        accelerometerValuesTextView.setVisibility(View.GONE);
        cycleButton.setVisibility(View.GONE);
        countDownTimer.start();
    }
    public void getSensorData(){


        sensorManager.registerListener(new SensorEventListener() {

            private ArrayList<String> accelerometerDataList = new ArrayList<>();
            private ArrayList<String> gyroscopeDataList = new ArrayList<>();
            private ArrayList<String> barometerDataList = new ArrayList<>();
            protected long startTime = 0;


            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(isCapturing){
                if (startTime == 0) {
                    startTime = event.timestamp;
                }

                long elapsedTime = event.timestamp - startTime;
                if (elapsedTime >= 4.9 * 1000000000L) { // Check if 5 seconds have elapsed
                    try {
                        doClassifier(accelerometerDataList,gyroscopeDataList,barometerDataList);
                    } catch (IOException | JSONException e) {
                        throw new RuntimeException(e);
                    }
                    startTime = event.timestamp;
                    accelerometerDataList.clear();
                    gyroscopeDataList.clear();
                    barometerDataList.clear();
                }
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    // Handle accelerometer values here
                    float ax = event.values[0]; // Acceleration along x-axis
                    float ay = event.values[1]; // Acceleration along y-axis
                    float az = event.values[2]; // Acceleration along z-axis
                    String accelerometerValuesString = "X" + ax + "Y" + ay + "Z" + az;
                    accelerometerDataList.add(accelerometerValuesString);
                }

                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    float gx = event.values[0]; // Rotation rate around x-axis
                    float gy = event.values[1]; // Rotation rate around y-axis
                    float gz = event.values[2]; // Rotation rate around z-axis
                    String gyroscopeValuesString = "X" + gx + "Y" + gy + "Z" + gz;
                    gyroscopeDataList.add(gyroscopeValuesString);

                }
                // Handle pressure sensor value
                if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                    float pressure = event.values[0]; // Atmospheric pressure
                    String pressureValueString = "P" + String.valueOf(pressure);
                    barometerDataList.add(pressureValueString);
                }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSensorChanged(SensorEvent event) {
    }
}