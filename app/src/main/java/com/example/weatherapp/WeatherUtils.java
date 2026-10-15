package com.example.weatherapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherUtils {

    /** Maps an OpenWeather condition id to a Lottie raw resource. */
    public static int animationFor(int id, String icon) {
        boolean night = icon != null && icon.endsWith("n");
        if (id >= 200 && id < 300) return R.raw.anim_thunder;
        if (id >= 300 && id < 600) return R.raw.anim_rain;
        if (id >= 600 && id < 700) return R.raw.anim_snow;
        if (id >= 700 && id < 800) return R.raw.anim_mist;
        if (id == 800) return night ? R.raw.anim_clear_night : R.raw.anim_sunny;
        return R.raw.anim_cloudy;
    }

    /** Top/bottom background gradient colors for a condition. */
    public static int[] gradientFor(int id, String icon) {
        boolean night = icon != null && icon.endsWith("n");
        if (id >= 200 && id < 300) return new int[]{0xFF1C1C2E, 0xFF414563};
        if (id >= 300 && id < 600) return new int[]{0xFF37474F, 0xFF607D8B};
        if (id >= 600 && id < 700) return new int[]{0xFF78909C, 0xFFCFD8DC};
        if (id >= 700 && id < 800) return new int[]{0xFF78909C, 0xFFB0BEC5};
        if (id == 800) return night ? new int[]{0xFF0B1026, 0xFF283B6B} : new int[]{0xFF1E88E5, 0xFF64B5F6};
        return night ? new int[]{0xFF263238, 0xFF455A64} : new int[]{0xFF546E7A, 0xFF90A4AE};
    }

    public static String iconUrl(String icon) {
        return "https://openweathermap.org/img/wn/" + icon + "@2x.png";
    }

    public static String windDir(int deg) {
        String[] d = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return d[(int) Math.round(deg / 45.0) % 8];
    }

    /** Formats a unix timestamp in the city's local time (tzOffset in seconds). */
    public static String time(long unix, int tzOffset, String pattern) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.getDefault());
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date((unix + tzOffset) * 1000L));
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
