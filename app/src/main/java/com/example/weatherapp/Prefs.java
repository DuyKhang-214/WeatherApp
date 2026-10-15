package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public class Prefs {
    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE);
    }

    public String getUnits() { return sp.getString("units", "metric"); }
    public void setUnits(String u) { sp.edit().putString("units", u).apply(); }
    public boolean isMetric() { return "metric".equals(getUnits()); }

    public String getLastCity() { return sp.getString("last_city", "London"); }
    public void setLastCity(String c) { sp.edit().putString("last_city", c).apply(); }

    public String getDefaultCity() { return sp.getString("default_city", getLastCity()); }
    public void setDefaultCity(String c) { sp.edit().putString("default_city", c).apply(); }
    public boolean hasDefaultCity() { return sp.contains("default_city"); }

    public List<String> getFavorites() {
        List<String> l = gson.fromJson(sp.getString("favorites", "[]"),
                new TypeToken<List<String>>() {}.getType());
        return l == null ? new ArrayList<>() : l;
    }
    public boolean addFavorite(String name) {
        List<String> l = getFavorites();
        if (l.contains(name)) return false;
        l.add(name);
        sp.edit().putString("favorites", gson.toJson(l)).apply();
        return true;
    }
    public void removeFavorite(String name) {
        List<String> l = getFavorites();
        l.remove(name);
        sp.edit().putString("favorites", gson.toJson(l)).apply();
    }
}
