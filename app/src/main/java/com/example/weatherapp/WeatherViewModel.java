package com.example.weatherapp;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/** Shared between the Weather and Map tabs. */
public class WeatherViewModel extends ViewModel {
    public final MutableLiveData<WeatherModels.CurrentWeather> current = new MutableLiveData<>();
}
