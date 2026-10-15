package com.example.weatherapp;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("data/2.5/weather")
    Call<WeatherModels.CurrentWeather> currentByCity(@Query("q") String city,
                                                     @Query("units") String units,
                                                     @Query("appid") String key);

    @GET("data/2.5/weather")
    Call<WeatherModels.CurrentWeather> currentByCoord(@Query("lat") double lat,
                                                      @Query("lon") double lon,
                                                      @Query("units") String units,
                                                      @Query("appid") String key);

    @GET("data/2.5/forecast")
    Call<WeatherModels.ForecastResponse> forecast(@Query("lat") double lat,
                                                  @Query("lon") double lon,
                                                  @Query("units") String units,
                                                  @Query("appid") String key);

    @GET("geo/1.0/direct")
    Call<List<WeatherModels.GeoResult>> geo(@Query("q") String query,
                                            @Query("limit") int limit,
                                            @Query("appid") String key);
}
