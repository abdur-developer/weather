package com.tufancoder.smartfermer.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.tufancoder.smartfermer.R;
import com.tufancoder.smartfermer.model.DatabaseHelper;
import com.tufancoder.smartfermer.model.JelaModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class WeatherActivity extends AppCompatActivity {
    TextView tv_location, condition, tv_condition, tv_temp, tv_temp_max, tv_temp_min,
            tv_pressure, tv_humidity, tv_wind_speed, tv_sunrise, tv_sunset, tv_date, tv_day;
    ConstraintLayout layout;
    LottieAnimationView lottieAnimationView;
    DatabaseHelper dbHelper;
    ArrayList<JelaModel> jelaModels;
    AutoCompleteTextView autoTextView;
    ImageView img_clear;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        initVariable();
        getAllJela();


        Now();

        searchJelaByAPI(sharedPreferences.getString("query", "dhaka"));




        onClick();

    }
    private void onClick() {
        img_clear.setOnClickListener(v -> autoTextView.setText(""));
    }

    private void getAllJela() {
        Cursor cursor = dbHelper.getAllData(dbHelper.JELA_TABLE);
        while (cursor.moveToNext()){
            String bn_text, en_text;
            int id;
            id = cursor.getInt(0);
            en_text = cursor.getString(1);
            bn_text = cursor.getString(2);

            jelaModels.add(new JelaModel(id,bn_text, en_text));
        }
        addJelaToAutocomplete();

    }

    private void addJelaToAutocomplete() {
        List<String> jelaList = new ArrayList<>();
        for (JelaModel model: jelaModels) {
            jelaList.add(model.getEnJela());
            jelaList.add(model.getBnJela());
        }
        String[] jelaArray = jelaList.toArray(new String[0]);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                jelaArray);
        autoTextView.setAdapter(adapter);
        autoTextView.setOnItemClickListener((adapterView, view, i, l) -> {
            String query = autoTextView.getText().toString().trim();
            searchJelaByAPI(query);
        });
    }



    private void searchJelaByAPI(String query) {

        String link = "https://api.openweathermap.org/data/2.5/weather?q="
                +dbHelper.getEnJela(query)+",bd&appid=eba19e891d356107f0340a1ccfe39efd";
        @SuppressLint("SetTextI18n") JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, link, null,
                response -> {
                    try {
                        // JSON ডেটা পার্স করে এখানে ব্যবহার করুন
                        String city = response.getString("name");
                        JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
                        String description = weather.getString("description");
                        String main = weather.getString("main");

                        JSONObject mainData = response.getJSONObject("main");
                        double temp = mainData.getDouble("temp");
                        double tempMax = mainData.getDouble("temp_max");
                        double tempMin = mainData.getDouble("temp_min");
                        int pressure = mainData.getInt("pressure");
                        int humidity = mainData.getInt("humidity");

                        JSONObject wind = response.getJSONObject("wind");
                        double speed = wind.getDouble("speed");

                        JSONObject sys = response.getJSONObject("sys");
                        long sunrise = sys.getLong("sunrise");
                        long sunset = sys.getLong("sunset");

                        tv_location.setText(dbHelper.getBnJela(city));
                        tv_condition.setText(translateToBengali(main));

                        condition.setText(translateToBengali(main));
                        tv_humidity.setText(convertNumberToBengali(humidity)+" %");

                        tv_sunrise.setText(convertUnixToTime(sunrise));
                        tv_sunset.setText(convertUnixToTime(sunset));

                        tv_temp_max.setText("সর্বোচ্চ " +kelToCel(tempMax));
                        tv_temp_min.setText("সর্বনিম্ন " +kelToCel(tempMin));

                        tv_wind_speed.setText( convertNumberToBengali(speed)+" m/s");
                        tv_pressure.setText( convertNumberToBengali(pressure)+" hPa");

                        tv_temp.setText(kelToCel(temp));

                        changeTheme(main.toLowerCase());
                        updateSaveJela(query);
                        autoTextView.setText("");
                    } catch (JSONException e) {
                        errorAlert("json");
                    }
                }, error -> {
            errorAlert("api");
        });
        RequestQueue mQueue = Volley.newRequestQueue(WeatherActivity.this);
        mQueue.add(request);

    }

    private void errorAlert(String note) {
        View customDialog = LayoutInflater.from(WeatherActivity.this).inflate(R.layout.error_alert, null);
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(WeatherActivity.this);


        alertdialog.setView(customDialog);
        ImageView cancel = customDialog.findViewById(R.id.imageView);

        final AlertDialog dialog = alertdialog.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        cancel.setOnClickListener(view -> dialog.cancel());


    }

    private void updateSaveJela(String query) {
        editor.putString("query",dbHelper.getEnJela(query));
        editor.apply();
    }

    private void initVariable() {
        tv_location = findViewById(R.id.location);
        tv_condition = findViewById(R.id.tv_conditions);
        condition = findViewById(R.id.conditions);
        tv_temp = findViewById(R.id.temp);
        tv_temp_max = findViewById(R.id.max_temp);
        tv_temp_min = findViewById(R.id.min_temp);
        tv_pressure = findViewById(R.id.tv_sea);
        tv_humidity = findViewById(R.id.tv_humidity);
        tv_wind_speed = findViewById(R.id.tv_wind);
        tv_sunrise = findViewById(R.id.tv_sunrise);
        tv_sunset = findViewById(R.id.tv_sunset);
        tv_date = findViewById(R.id.date);
        tv_day = findViewById(R.id.day);
        img_clear = findViewById(R.id.img_clear);
        autoTextView = findViewById(R.id.autoTextView);

        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        layout = findViewById(R.id.main);

        dbHelper = new DatabaseHelper(WeatherActivity.this);
        jelaModels = new ArrayList<>();

        sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    private String kelToCel(double number){
        // Kelvin - 273.15 ----> কেলভিন থেকে সেলসিয়াস
        return convertNumberToBengali( (int) Math.round( number - 273.15))+"°C";
    }
    private String convertUnixToTime(long unixTimestamp) {
        Locale locale = new Locale("bn", "BD");
        Date date = new Date(unixTimestamp * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", locale);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }
    private void Now(){
        Date currentDate = new Date();

        Locale locale = new Locale("bn", "BD");

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", locale);
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", locale);
        String formattedDay = dayFormat.format(currentDate);
        String formattedDate = dateFormat.format(currentDate);

        tv_day.setText(formattedDay);
        tv_date.setText(formattedDate);

    }
    private String translateToBengali(String text) {
        switch (text.toLowerCase()) {
            case "rain":
                return "বৃষ্টি";
            case "light rain":
                return "হালকা বৃষ্টি";
            case "clear":
                return "পরিষ্কার";
            case "clouds":
                return "মেঘ";
            case "haze":
                return "কুয়াশা";
            case "heavy rain":
                return "ভারী বৃষ্টি";
            case "showers":
                return "বর্ষণ";
            case "moderate rain":
                return "মাঝারি বৃষ্টি";
            case "drizzle":
                return "গুঁড়ি গুঁড়ি";
            case "heavy snow":
                return "ভারী তুষার";
            case "moderate snow":
                return "মাঝারি তুষার";
            case "blizzard":
                return "তুষারঝড়";
            case "light snow":
                return "হালকা তুষার";
            case "foggy":
                return "কুয়াশাচ্ছন্ন";
            case "mist":
                return "কুয়াশা";
            case "overcast":
                return "মেঘাচ্ছন্ন";
            case "partly clouds":
                return "আংশিক মেঘ";
            case "sunny":
                return "রৌদ্রোজ্জ্বল";
            case "clear sky":
                return "স্বচ্ছ আকাশ";
            default:
                return text;
        }
    }
    private void changeTheme(String text){
        if (checkTrue(new String[]{"clear sky", "sunny", "clear"}, text)) setTheme(R.raw.sun, R.drawable.sunny_background);
        else if (checkTrue(new String[]{"partly clouds", "clouds", "overcast", "mist", "foggy"}, text)) setTheme(R.raw.cloud, R.drawable.colud_background);
        else if (checkTrue(new String[]{"light snow", "moderate snow", "heavy snow", "blizzard"}, text)) setTheme(R.raw.snow, R.drawable.snow_background);
        else setTheme(R.raw.rain, R.drawable.rain_background); //light rain, drizzle, moderate rain, showers, heavy rain
    }
    private void setTheme(int lottie, int img){
        lottieAnimationView.setAnimation(lottie);
        lottieAnimationView.playAnimation();
        layout.setBackgroundResource(img);
    }
    private boolean checkTrue(String[] array, String text){
        for (String s : array) {
            if (Objects.equals(text, s)) return true;
        }
        return false;
    }

    private String convertNumberToBengali(double number) {
        String[] bengaliDigits = {"০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯"};
        String numberStr = String.valueOf(number);
        StringBuilder bengaliNumber = new StringBuilder();

        for (char digit : numberStr.toCharArray()) {
            if (Character.isDigit(digit)) {
                int index = Character.getNumericValue(digit);
                bengaliNumber.append(bengaliDigits[index]);
            } else {
                bengaliNumber.append(digit);
            }
        }

        return bengaliNumber.toString();
    }

}
