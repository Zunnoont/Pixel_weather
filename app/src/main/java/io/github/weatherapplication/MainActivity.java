package io.github.weatherapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.chrono.MinguoChronology;
import java.util.List;
import java.util.Locale;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homepage;
    private ProgressBar loading_page;
    private TextView temperature, city_and_country, weather_status;
    private EditText enter_city;

    private ImageView icon, background, weather_image;
    private pl.droidsonroids.gif.GifImageView gif;
    private FusedLocationProviderClient fusedLocationClient;
    String user_city = "";
    String latitude;
    String longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        homepage = findViewById(R.id.home_page);
        loading_page = findViewById(R.id.loading);
        temperature = findViewById(R.id.temperature);
        city_and_country = findViewById(R.id.city);
        weather_status = findViewById(R.id.weather_status);
        weather_image = findViewById(R.id.weather_image);
        enter_city = findViewById(R.id.edit_city);
        background = findViewById(R.id.wallpaper);
        gif = findViewById(R.id.gif);

        background.setScaleType(ImageView.ScaleType.FIT_XY);
        gif.setScaleType(ImageView.ScaleType.FIT_XY);

        enter_city.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    JSONObject json = parse_json();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                if(imm.isAcceptingText()) { // verify if the soft keyboard is open
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                get_longitude_and_latitude(enter_city.getText().toString());
                get_weather_info(latitude, longitude);
                return true;
            }
            return false;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            get_location(fusedLocationClient);
            try {
                JSONObject json_data = parse_australian_suburbs();
                if(json_data.has(user_city)) {
                    get_longitude_and_latitude(enter_city.getText().toString());
                    get_weather_info(latitude, longitude);
                }
                else {
                    System.out.println("whats up");
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IO Exception");
                System.out.println(new File(".").getAbsoluteFile());
            } catch (ParseException e) {
                System.out.println("ParseException");

                e.printStackTrace();
            }
            // get_weather_info("Perth");
        }
        else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    22);
        }


    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
    public JSONObject parse_json() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader("countries_to_cities.json");
        Object object = parser.parse(reader);
        JSONObject json_data = (JSONObject)object;
        return json_data;
    }
    public JSONObject parse_australian_suburbs() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader("australian_suburbs.json");
        Object object = parser.parse(reader);
        JSONObject json_data = (JSONObject)object;
        return json_data;

    }
    public void get_location(FusedLocationProviderClient fusedLocationClient) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 22);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        city_and_country.setText(address.get(0).getAddressLine(0) + ", " + address.get(0).getCountryName());
                        user_city = address.get(0).getLocality();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(e.toString());
                    }
                }
            }
        });
    }
    public void update_weather_image(String weather_text) {
        if(weather_text.equals("Partly cloudy")) {
            weather_image.setImageResource(R.drawable.cloud_icon_cloudy_and_sunny);
            gif.setVisibility(View.GONE);
            background.setVisibility(View.VISIBLE);
            temperature.setTextColor(Color.parseColor("#202020"));
            city_and_country.setTextColor(Color.parseColor("#202020"));
            weather_status.setTextColor(Color.parseColor("#202020"));

        }
        else if(weather_text.contains("cloud") == true || weather_text.equals("Overcast")) {
            weather_image.setImageResource(R.drawable.cloud_icon_cloudy);
            gif.setVisibility(View.GONE);
            background.setVisibility(View.VISIBLE);
        }
        else if(weather_text.contains("clear") || weather_text.equals("Sunny")) {
            weather_image.setImageResource(R.drawable.sunny);
            gif.setVisibility(View.VISIBLE);
            background.setVisibility(View.GONE);
            temperature.setTextColor(Color.WHITE);
            city_and_country.setTextColor(Color.WHITE);
            weather_status.setTextColor(Color.WHITE);
            gif.setImageResource(R.drawable.wallpaper_clear_night);
        }
        else if(weather_text.contains("rain")) {
            weather_image.setImageResource(R.drawable.cloud_icon_rainy);
            gif.setVisibility(View.VISIBLE);
            background.setVisibility(View.GONE);
            gif.setImageResource(R.drawable.wallpaper_rainy);
            temperature.setTextColor(Color.parseColor("#202020"));
            city_and_country.setTextColor(Color.parseColor("#202020"));
            weather_status.setTextColor(Color.parseColor("#202020"));
        }
    }
    private void get_longitude_and_latitude(String city) {
        String url = "http://api.openweathermap.org/geo/1.0/direct?q=" + city + "n&limit=5&appid=d90f2f3bc5c53cf687458bd3f68904bb";
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        System.out.println("Hello here!!");

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // do something
                        try {
                            latitude = response.getJSONObject(0).getString("lat");
                            longitude = response.getJSONObject(0).getString("lon");
                        }
                        catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("something went wrong ;(, Error:" + error.toString());
                Toast.makeText(MainActivity.this, "Error occurred", Toast.LENGTH_SHORT).show();

            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);

    }
    private void get_weather_info(String lat, String lon) {
        System.out.println("Hello!");
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon +" &appid=d90f2f3bc5c53cf687458bd3f68904bb";
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        System.out.println("Hello here!!");
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do something
                        try {
                            String curr_temp = response.getJSONObject("main").getString("temp");
                            String temp_degrees = curr_temp + "°C";
                            String weather_status_in_city = response.getJSONObject("weather").getString("description");
                            String locality = response.getJSONObject("location").getString("name") + ", " + response.getJSONObject("location").getString("country");
                            temperature.setText(temp_degrees);
                            weather_status.setText(weather_status_in_city);
                            city_and_country.setText(locality);
                            update_weather_image(weather_status_in_city);

                        }
                        catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("something went wrong ;(, Error:" + error.toString());
                Toast.makeText(MainActivity.this, "Error occurred", Toast.LENGTH_SHORT).show();

            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                    if(imm.isAcceptingText()) { // verify if the soft keyboard is open
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }
}