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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
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
    protected Button crashButton;
    protected int light_purple;
    protected int dark_purple;

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

        // Initialize TextViews
        accelerometerValuesTextView = findViewById(R.id.accelerometerValuesTextView);
        gyroscopeValuesTextView = findViewById(R.id.gyroscopeValuesTextView2);
        pressureValuesTextView = findViewById(R.id.pressureValuesTextView3);

        cycleButton = findViewById(R.id.cycleButton);
        crashButton = findViewById(R.id.crashButton);

        light_purple = Color.rgb(187,134, 252);
        dark_purple = Color.rgb(120,86, 162);
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

    public void onCycleClick(View view){
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        fileName = "cycle_" + timestamp + ".txt";
        isCapturing = !isCapturing;
        if(isCapturing) {
            cycleButton.setText("Recording cycle...");
            cycleButton.setBackgroundColor(dark_purple);
        }
        else{
            cycleButton.setText("Record cycle");
            cycleButton.setBackgroundColor(light_purple);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onCrashClick(View view){
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        fileName = "crash_" + timestamp + ".txt";
        isCapturing = !isCapturing;
        if(isCapturing) {
            crashButton.setText("Recording crash...");
            crashButton.setBackgroundColor(dark_purple);
        }
        else{
            crashButton.setText("Record crash");
            crashButton.setBackgroundColor(light_purple);
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(isCapturing) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Handle accelerometer values here
                float ax = event.values[0]; // Acceleration along x-axis
                float ay = event.values[1]; // Acceleration along y-axis
                float az = event.values[2]; // Acceleration along z-axis
                String accelerometerValuesString = "\nX" + ax + "Y" + ay + "Z" + az;
                String accelerometerDisplayValuesString = "\nX" + ax + "\nY" + ay + "\nZ" + az;
                accelerometerValuesTextView.setText("Accelerometer Values: " + accelerometerDisplayValuesString);
                // Save accelerometer data to file
                saveSensorDataToFile("/accelerometer_data_"+fileName, accelerometerValuesString);
            }
            // Handle gyroscope values
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float gx = event.values[0]; // Rotation rate around x-axis
                float gy = event.values[1]; // Rotation rate around y-axis
                float gz = event.values[2]; // Rotation rate around z-axis
                String gyroscopeValuesString = "\nX" + gx + "Y" + gy + "Z" + gz;
                String gyroscopeDisplayValuesString = "\nX" + gx + "\nY " + gy + "\nZ" + gz;
                gyroscopeValuesTextView.setText("Gyroscope Values: " + gyroscopeDisplayValuesString);
                // Save gyroscope data to file
                saveSensorDataToFile("/gyroscope_data_"+fileName, gyroscopeValuesString);
            }
            // Handle pressure sensor value
            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                float pressure = event.values[0]; // Atmospheric pressure
                String pressureValueString = "\nP" + String.valueOf(pressure);
                String pressureDisplayValueString = "\n" + String.valueOf(pressure);
                pressureValuesTextView.setText("Pressure Value: \n" + pressureDisplayValueString);
                // Save pressure data to file
                saveSensorDataToFile("/pressure_data_"+fileName, pressureValueString);
            }
        }
    }
}