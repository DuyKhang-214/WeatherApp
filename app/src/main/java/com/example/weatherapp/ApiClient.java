package com.example.weatherapp;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static WeatherApi api;

    public static synchronized WeatherApi get() {
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherApi.class);
        }
        return api;
    }
}
