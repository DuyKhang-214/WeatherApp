package com.example.weatherapp;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Gson models for the OpenWeather responses. */
public class WeatherModels {
    public static class Coord { public double lat, lon; }
    public static class Condition { public int id; public String main, description, icon; }
    public static class Main { public double temp, feels_like, temp_min, temp_max; public int pressure, humidity; }
    public static class Wind { public double speed; public int deg; }
    public static class Clouds { public int all; }
    public static class Rain { @SerializedName("1h") public double oneHour; }
    public static class Sys { public String country; public long sunrise, sunset; }

    public static class CurrentWeather {
        public Coord coord;
        public List<Condition> weather;
        public Main main;
        public Wind wind;
        public Clouds clouds;
        public Rain rain;
        public Sys sys;
        public int visibility;
        public int timezone;
        public long dt;
        public String name;
    }

    public static class ForecastItem {
        public long dt;
        public Main main;
        public List<Condition> weather;
        public double pop;
    }
    public static class City { public int timezone; }
    public static class ForecastResponse { public List<ForecastItem> list; public City city; }

    public static class GeoResult {
        public String name, country, state;
        public double lat, lon;
        public String label() {
            StringBuilder sb = new StringBuilder(name == null ? "" : name);
            if (state != null && !state.isEmpty()) sb.append(", ").append(state);
            if (country != null && !country.isEmpty()) sb.append(", ").append(country);
            return sb.toString();
        }
    }
}
