package com.example.weatherapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap map;
    private TileOverlay overlay;
    private Marker marker;
    private String layer = "precipitation_new";
    private Prefs prefs;
    private WeatherViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_map, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        prefs = new Prefs(requireContext());
        vm = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        MaterialButtonToggleGroup g = v.findViewById(R.id.layerToggle);
        g.addOnButtonCheckedListener((grp, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btnRain) layer = "precipitation_new";
            else if (id == R.id.btnClouds) layer = "clouds_new";
            else if (id == R.id.btnTemp) layer = "temp_new";
            else layer = "wind_new";
            showLayer();
        });

        SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentByTag("gmap");
        if (smf == null) {
            smf = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.mapContainer, smf, "gmap").commitNow();
        }
        smf.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap gm) {
        map = gm;
        map.getUiSettings().setZoomControlsEnabled(true);
        showLayer();
        map.setOnMapClickListener(this::onMapTap);
        vm.current.observe(getViewLifecycleOwner(), w -> {
            if (w == null || w.coord == null || map == null) return;
            LatLng p = new LatLng(w.coord.lat, w.coord.lon);
            if (marker != null) marker.remove();
            marker = map.addMarker(new MarkerOptions().position(p).title(w.name).snippet(summary(w)));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 6f));
        });
    }

    /** Overlays an OpenWeather tile layer on top of the base map. */
    private void showLayer() {
        if (map == null) return;
        if (overlay != null) overlay.remove();
        final String l = layer;
        TileProvider tp = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                try {
                    return new URL("https://tile.openweathermap.org/map/" + l + "/" + zoom + "/"
                            + x + "/" + y + ".png?appid=" + BuildConfig.OWM_KEY);
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        };
        overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tp).transparency(0.25f));
    }

    /** Tap anywhere on the map to see the weather at that point. */
    private void onMapTap(LatLng p) {
        if (BuildConfig.OWM_KEY.isEmpty()) return;
        if (marker != null) marker.remove();
        final Marker m = map.addMarker(new MarkerOptions().position(p).title("Loading…"));
        marker = m;
        ApiClient.get().currentByCoord(p.latitude, p.longitude, prefs.getUnits(), BuildConfig.OWM_KEY)
                .enqueue(new Callback<WeatherModels.CurrentWeather>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherModels.CurrentWeather> call,
                                           @NonNull Response<WeatherModels.CurrentWeather> r) {
                        if (!isAdded() || !r.isSuccessful() || r.body() == null || r.body().weather == null) return;
                        WeatherModels.CurrentWeather w = r.body();
                        m.setTitle(w.name == null || w.name.isEmpty() ? "Selected point" : w.name);
                        m.setSnippet(summary(w));
                        m.showInfoWindow();
                    }
                    @Override
                    public void onFailure(@NonNull Call<WeatherModels.CurrentWeather> call, @NonNull Throwable t) {
                        if (isAdded()) m.setTitle("Network error");
                    }
                });
    }

    private String summary(WeatherModels.CurrentWeather w) {
        String desc = w.weather != null && !w.weather.isEmpty() ? w.weather.get(0).description : "";
        double rain = w.rain != null ? w.rain.oneHour : 0;
        return String.format(Locale.getDefault(), "%d° · %s · rain %.1f mm/h",
                Math.round(w.main.temp), desc, rain);
    }
}
