package com.mausampro.weather;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    TextView tvCity, tvTemp, tvCondition, tvFeels, tvHumidity, tvWind;
    CardView weatherCard;
    Switch themeSwitch;

    // Location
    FusedLocationProviderClient fusedLocationClient;

    // Theme
    SharedPreferences prefs;

    private static final int LOCATION_PERMISSION = 100;
    private static final String API_KEY = "c508a24de8eff60cdd1111a86a2886eb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔹 Load saved theme BEFORE setContentView
        prefs = getSharedPreferences("theme_pref", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        tvCity = findViewById(R.id.tvCity);
        tvTemp = findViewById(R.id.tvTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvFeels = findViewById(R.id.tvFeels);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        weatherCard = findViewById(R.id.weatherCard);
        themeSwitch = findViewById(R.id.themeSwitch);

        // Switch state
        themeSwitch.setChecked(isDark);

        themeSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();

            if (checked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();
    }

    // 📍 LOCATION
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION
            );
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getCityAndWeather(location);
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCityAndWeather(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                tvCity.setText(addresses.get(0).getLocality());
            }

            fetchWeather(location.getLatitude(), location.getLongitude());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ☁️ WEATHER API
    private void fetchWeather(double lat, double lon) {

        String url = "https://api.openweathermap.org/data/2.5/weather?lat="
                + lat + "&lon=" + lon + "&units=metric&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        JSONObject wind = response.getJSONObject("wind");

                        double temp = main.getDouble("temp");
                        double feels = main.getDouble("feels_like");
                        int humidity = main.getInt("humidity");
                        double windSpeed = wind.getDouble("speed");

                        String condition = response
                                .getJSONArray("weather")
                                .getJSONObject(0)
                                .getString("main");

                        tvTemp.setText(temp + "°C");
                        tvFeels.setText(feels + "°C");
                        tvHumidity.setText(humidity + "%");
                        tvWind.setText(windSpeed + " km/h");
                        tvCondition.setText(condition);

                        animateCard();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Weather fetch failed", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // ✨ ANIMATION
    private void animateCard() {
        weatherCard.setAlpha(0f);
        weatherCard.setScaleX(0.9f);
        weatherCard.setScaleY(0.9f);

        weatherCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .start();
    }

    // 🔐 PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }
    }
}
