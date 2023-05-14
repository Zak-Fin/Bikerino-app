package com.example.bikerino_prototype;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.net.URL;
import java.util.Scanner;

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

    protected Timer timer;
    String apiSample ="http://192.168.0.20:5000";

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
            }
        }.start();


        // Initialize TextViews
        accelerometerValuesTextView = findViewById(R.id.accelerometerValuesTextView);


        light_purple = Color.rgb(187,134, 252);
        dark_purple = Color.rgb(120,86, 162);
        getSensorData();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the SensorEventListener
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
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

    }
    public void onCycleClick(View view){
        System.out.println("something happening2");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        fileName = "cycle_" + timestamp + ".txt";
        isCapturing = !isCapturing;
        if(isCapturing) {

            System.out.println("something happening");
            cycleButton.setText("Recording cycle...");
            cycleButton.setBackgroundColor(dark_purple);
        }
        else{
            cycleButton.setText("Record cycle");
            cycleButton.setBackgroundColor(light_purple);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveSensorDataToFile(String fileName, String data) {
        try {

            // Get the current date and time
            LocalDateTime now = LocalDateTime.now();

            // Format the date and time as a string
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);

            // Create a file in external storage directory
            File file = new File(getFilesDir(), fileName);

            // Create a FileOutputStream to write to the file
            FileOutputStream fos = new FileOutputStream(file, true); // 'true' to append data to file

            // Create an OutputStreamWriter to write data to FileOutputStream
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            // Write data to the file
            osw.write(data);

            // Close the OutputStreamWriter and FileOutputStream
            osw.close();
            fos.close();

            Toast.makeText(this,  formattedDateTime+ "formattedDateTimeSensor data saved to file :" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doClassifier(ArrayList<String> accelerometerDataList,ArrayList<String> gyroscopeDataList,ArrayList<String> barometerDataList) throws IOException {

//        URL url = new URL("https://postman-echo.com/get/");
//        RequestQueue queue = Volley.newRequestQueue(this);
//        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiSample,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        System.out.println(response);
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error){
//                System.out.println(error);
//            }
//        });
//        queue.add(stringRequest);
        String sample ="http://192.168.0.20:5000/predict";
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
//        JSONObject jsonBody = new JSONObject();
//        try {
//            jsonBody.put("Title", "Android Volley Demo");
//            jsonBody.put("Author", "BNK");
//        } catch (JSONException e) {
//            throw new RuntimeException(e);
//        }
//        JSONObject obj = new JSONObject();
        final String requestBody = accelerometerDataList.toString();

//        StringRequest stringRequest = new StringRequest(Request.Method.POST, sample,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        Log.i("VOLLEY", response);
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error){
//                System.out.println("Error");
//            }
//        }){
//            @Override
//            public String getBodyContentType() {
//                return "application/json; charset=utf-8";
//            }
//            @Override
//            public byte[] getBody() throws AuthFailureError {
//                try {
//                    return requestBody == null ? null : requestBody.getBytes("utf-8");
//                } catch (UnsupportedEncodingException uee) {
//                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
//                    return null;
//                }
//            }
//            @Override
//            protected Response<String> parseNetworkResponse(NetworkResponse response) {
//                String responseString = "";
//                if (response != null) {
////                    responseString = String.valueOf(response.statusCode);
//                    // can get more details such as response.headers
//
//                    responseString = new String(response.data);
//                }
//                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
//            }
//        };
//        queue.add(stringRequest);

        boolean yes= true;

        if(yes==true){
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
        else{
            accelerometerValuesTextView.setText("Have you crashed: \n" + "No");
        }

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
                    } catch (IOException e) {
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