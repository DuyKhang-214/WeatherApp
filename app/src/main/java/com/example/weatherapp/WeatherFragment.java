package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherFragment extends Fragment implements SensorEventListener {
    private static final long AUTO_REFRESH_MS = 10 * 60 * 1000L;

    private WeatherViewModel vm;
    private Prefs prefs;
    private View root;
    private LottieAnimationView lottie;
    private SwipeRefreshLayout swipe;
    private TextView tvCity, tvTemp, tvDesc, tvHiLo, tvUpdated, tvFavLabel;
    private ImageView ivIcon;
    private ChipGroup chipFav;
    private LinearLayout hourly, daily;
    private DetailAdapter detailAdapter;
    private MaterialCardView cardSuggestions;
    private ListView lvSuggestions;
    private ArrayAdapter<String> suggestAdapter;
    private final List<WeatherModels.GeoResult> suggestions = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    private int currentAnim = -1;
    private String contextCity;
    private ActivityResultLauncher<String> permissionLauncher;
    private SensorManager sensorManager;
    private long lastShake;

    @Override
    public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) fetchLocation();
                    else toast("Location permission denied");
                });
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_weather, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        prefs = new Prefs(requireContext());
        vm = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        root = v.findViewById(R.id.weatherRoot);
        lottie = v.findViewById(R.id.lottieBackground);
        swipe = v.findViewById(R.id.swipeRefresh);
        tvCity = v.findViewById(R.id.tvCity);
        tvTemp = v.findViewById(R.id.tvTemp);
        tvDesc = v.findViewById(R.id.tvDesc);
        tvHiLo = v.findViewById(R.id.tvHiLo);
        tvUpdated = v.findViewById(R.id.tvUpdated);
        tvFavLabel = v.findViewById(R.id.tvFavLabel);
        ivIcon = v.findViewById(R.id.ivIcon);
        chipFav = v.findViewById(R.id.chipFavorites);
        hourly = v.findViewById(R.id.hourlyContainer);
        daily = v.findViewById(R.id.dailyContainer);
        cardSuggestions = v.findViewById(R.id.cardSuggestions);
        lvSuggestions = v.findViewById(R.id.lvSuggestions);
        FloatingActionButton fab = v.findViewById(R.id.fabLocation);

        RecyclerView rv = v.findViewById(R.id.rvDetails);
        detailAdapter = new DetailAdapter();
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(detailAdapter);
        rv.setNestedScrollingEnabled(false);

        suggestAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_suggestion, new ArrayList<>());
        lvSuggestions.setAdapter(suggestAdapter);
        lvSuggestions.setOnItemClickListener((p, view, pos, id) -> {
            WeatherModels.GeoResult g = suggestions.get(pos);
            hideSuggestions();
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).collapseSearch();
            loadByCoord(g.lat, g.lon);
        });

        swipe.setOnRefreshListener(this::refresh);
        fab.setOnClickListener(x -> requestLocation());

        refreshFavorites();
        loadByCity(prefs.getDefaultCity());
    }

    // ------------------------------------------------------------------ data loading
    private boolean checkKey() {
        if (BuildConfig.OWM_KEY.isEmpty()) {
            swipe.setRefreshing(false);
            Snackbar.make(root, "Add OWM_KEY to local.properties and rebuild", Snackbar.LENGTH_INDEFINITE).show();
            return false;
        }
        return true;
    }

    public void refresh() {
        WeatherModels.CurrentWeather w = vm.current.getValue();
        if (w != null && w.coord != null) loadByCoord(w.coord.lat, w.coord.lon);
        else loadByCity(prefs.getDefaultCity());
    }

    private void loadByCity(String city) {
        if (!checkKey()) return;
        swipe.setRefreshing(true);
        ApiClient.get().currentByCity(city, prefs.getUnits(), BuildConfig.OWM_KEY).enqueue(weatherCallback);
    }

    private void loadByCoord(double lat, double lon) {
        if (!checkKey()) return;
        swipe.setRefreshing(true);
        ApiClient.get().currentByCoord(lat, lon, prefs.getUnits(), BuildConfig.OWM_KEY).enqueue(weatherCallback);
    }

    private final Callback<WeatherModels.CurrentWeather> weatherCallback =
            new Callback<WeatherModels.CurrentWeather>() {
                @Override
                public void onResponse(@NonNull Call<WeatherModels.CurrentWeather> call,
                                       @NonNull Response<WeatherModels.CurrentWeather> r) {
                    if (!isAdded()) return;
                    swipe.setRefreshing(false);
                    if (r.isSuccessful() && r.body() != null && r.body().weather != null && !r.body().weather.isEmpty()) {
                        bind(r.body());
                    } else if (r.code() == 401) {
                        toast("Invalid API key (new keys can take a while to activate)");
                    } else if (r.code() == 404) {
                        toast("City not found");
                    } else {
                        toast("Server error " + r.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<WeatherModels.CurrentWeather> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    swipe.setRefreshing(false);
                    toast("Network error - check your connection");
                }
            };

    private void bind(WeatherModels.CurrentWeather w) {
        vm.current.setValue(w);
        if (w.name != null && !w.name.isEmpty()) prefs.setLastCity(w.name);
        WeatherModels.Condition c = w.weather.get(0);

        String country = w.sys != null && w.sys.country != null ? ", " + w.sys.country : "";
        tvCity.setText(w.name + country);
        tvTemp.setText(Math.round(w.main.temp) + "°");
        tvDesc.setText(WeatherUtils.capitalize(c.description));
        tvHiLo.setText("H: " + Math.round(w.main.temp_max) + "°   L: " + Math.round(w.main.temp_min) + "°");
        tvUpdated.setText("Updated " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        Glide.with(this).load(WeatherUtils.iconUrl(c.icon)).into(ivIcon);

        applyTheme(c.id, c.icon);
        detailAdapter.set(buildDetails(w));
        refreshFavorites();
        loadForecast(w.coord.lat, w.coord.lon);
    }

    /** Gradient background + cross-fading Lottie animation. */
    private void applyTheme(int id, String icon) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                WeatherUtils.gradientFor(id, icon));
        root.setBackground(g);

        final int anim = WeatherUtils.animationFor(id, icon);
        if (anim == currentAnim) return;
        boolean first = currentAnim == -1;
        currentAnim = anim;
        Runnable swap = () -> {
            if (!isAdded()) return;
            lottie.setAnimation(anim);
            lottie.playAnimation();
            lottie.animate().alpha(1f).setDuration(400).start();
        };
        if (first) { lottie.setAlpha(0f); swap.run(); }
        else lottie.animate().alpha(0f).setDuration(250).withEndAction(swap).start();
    }

    private List<String[]> buildDetails(WeatherModels.CurrentWeather w) {
        boolean m = prefs.isMetric();
        List<String[]> l = new ArrayList<>();
        l.add(new String[]{"🌡 Feels like", Math.round(w.main.feels_like) + "°"});
        l.add(new String[]{"💧 Humidity", w.main.humidity + "%"});
        if (w.wind != null) {
            l.add(new String[]{"💨 Wind", String.format(Locale.getDefault(), "%.1f %s %s",
                    w.wind.speed, m ? "m/s" : "mph", WeatherUtils.windDir(w.wind.deg))});
        }
        l.add(new String[]{"⏲ Pressure", w.main.pressure + " hPa"});
        double km = w.visibility / 1000.0;
        l.add(new String[]{"👁 Visibility", String.format(Locale.getDefault(), "%.1f %s",
                m ? km : km * 0.621371, m ? "km" : "mi")});
        if (w.clouds != null) l.add(new String[]{"☁ Cloudiness", w.clouds.all + "%"});
        double rain = w.rain != null ? w.rain.oneHour : 0;
        l.add(new String[]{"🌧 Rain (1h)", String.format(Locale.getDefault(), m ? "%.1f mm" : "%.2f in",
                m ? rain : rain / 25.4)});
        if (w.sys != null) {
            l.add(new String[]{"🌅 Sunrise", WeatherUtils.time(w.sys.sunrise, w.timezone, "HH:mm")});
            l.add(new String[]{"🌇 Sunset", WeatherUtils.time(w.sys.sunset, w.timezone, "HH:mm")});
        }
        l.add(new String[]{"↕ Min / Max", Math.round(w.main.temp_min) + "° / " + Math.round(w.main.temp_max) + "°"});
        return l;
    }

    // ------------------------------------------------------------------ forecast
    private void loadForecast(double lat, double lon) {
        ApiClient.get().forecast(lat, lon, prefs.getUnits(), BuildConfig.OWM_KEY)
                .enqueue(new Callback<WeatherModels.ForecastResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherModels.ForecastResponse> call,
                                           @NonNull Response<WeatherModels.ForecastResponse> r) {
                        if (isAdded() && r.isSuccessful() && r.body() != null && r.body().list != null) {
                            bindForecast(r.body());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<WeatherModels.ForecastResponse> call, @NonNull Throwable t) { }
                });
    }

    private static class DayAgg {
        String label, icon;
        double min = 999, max = -999;
        int bestDiff = 99;
    }

    private void bindForecast(WeatherModels.ForecastResponse f) {
        int tz = f.city != null ? f.city.timezone : 0;

        // Hourly cards are built purely in Java code
        hourly.removeAllViews();
        for (int i = 0; i < Math.min(8, f.list.size()); i++) {
            hourly.addView(buildHourlyCard(f.list.get(i), tz));
        }

        // Daily aggregation from the 3-hourly list
        Map<String, DayAgg> days = new LinkedHashMap<>();
        for (WeatherModels.ForecastItem it : f.list) {
            String key = WeatherUtils.time(it.dt, tz, "yyyy-MM-dd");
            DayAgg a = days.get(key);
            if (a == null) {
                a = new DayAgg();
                a.label = WeatherUtils.time(it.dt, tz, "EEEE");
                days.put(key, a);
            }
            a.min = Math.min(a.min, it.main.temp_min);
            a.max = Math.max(a.max, it.main.temp_max);
            int hour = Integer.parseInt(WeatherUtils.time(it.dt, tz, "H"));
            int diff = Math.abs(hour - 12);
            if (diff < a.bestDiff && it.weather != null && !it.weather.isEmpty()) {
                a.bestDiff = diff;
                a.icon = it.weather.get(0).icon;
            }
        }
        daily.removeAllViews();
        int n = 0;
        for (DayAgg a : days.values()) {
            if (n >= 5) break;
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_daily, daily, false);
            ((TextView) row.findViewById(R.id.tvDay)).setText(n == 0 ? "Today" : a.label);
            ((TextView) row.findViewById(R.id.tvDayRange)).setText(Math.round(a.max) + "° / " + Math.round(a.min) + "°");
            if (a.icon != null) {
                Glide.with(this).load(WeatherUtils.iconUrl(a.icon)).into((ImageView) row.findViewById(R.id.ivDayIcon));
            }
            daily.addView(row);
            n++;
        }
    }

    private View buildHourlyCard(WeatherModels.ForecastItem it, int tz) {
        Context ctx = requireContext();
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        card.setLayoutParams(lp);

        card.addView(styledText(WeatherUtils.time(it.dt, tz, "HH:mm"), 12, false));
        ImageView icon = new ImageView(ctx);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        if (it.weather != null && !it.weather.isEmpty()) {
            Glide.with(this).load(WeatherUtils.iconUrl(it.weather.get(0).icon)).into(icon);
        }
        card.addView(icon);
        card.addView(styledText(Math.round(it.main.temp) + "°", 16, true));
        card.addView(styledText("💧" + Math.round(it.pop * 100) + "%", 11, false));
        return card;
    }

    private TextView styledText(String s, float sp, boolean bold) {
        TextView t = new TextView(requireContext());
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // ------------------------------------------------------------------ search (called from MainActivity)
    public void onSearchSubmit(String q) {
        if (q.isEmpty()) return;
        hideSuggestions();
        loadByCity(q);
    }

    public void onSearchChanged(String s) {
        if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
        if (s == null || s.trim().length() < 3 || BuildConfig.OWM_KEY.isEmpty()) { hideSuggestions(); return; }
        final String q = s.trim();
        searchRunnable = () -> ApiClient.get().geo(q, 5, BuildConfig.OWM_KEY)
                .enqueue(new Callback<List<WeatherModels.GeoResult>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<WeatherModels.GeoResult>> call,
                                           @NonNull Response<List<WeatherModels.GeoResult>> r) {
                        if (!isAdded() || !r.isSuccessful() || r.body() == null) return;
                        suggestions.clear();
                        suggestions.addAll(r.body());
                        suggestAdapter.clear();
                        for (WeatherModels.GeoResult g : suggestions) suggestAdapter.add(g.label());
                        suggestAdapter.notifyDataSetChanged();
                        cardSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<WeatherModels.GeoResult>> call, @NonNull Throwable t) { }
                });
        handler.postDelayed(searchRunnable, 400); // debounce
    }

    public void hideSuggestions() {
        if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
        if (cardSuggestions != null) cardSuggestions.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------ favorites + ContextMenu
    private void refreshFavorites() {
        chipFav.removeAllViews();
        List<String> favs = prefs.getFavorites();
        tvFavLabel.setVisibility(favs.isEmpty() ? View.GONE : View.VISIBLE);
        boolean hasDefault = prefs.hasDefaultCity();
        for (String name : favs) {
            Chip chip = new Chip(requireContext());
            chip.setText((hasDefault && name.equals(prefs.getDefaultCity()) ? "★ " : "") + name);
            chip.setTag(name);
            chip.setTextColor(Color.WHITE);
            chip.setChipBackgroundColor(ColorStateList.valueOf(0x33FFFFFF));
            chip.setChipStrokeWidth(0f);
            chip.setOnClickListener(x -> loadByCity(name));
            registerForContextMenu(chip);
            chipFav.addView(chip);
        }
    }

    public void addCurrentToFavorites() {
        WeatherModels.CurrentWeather w = vm.current.getValue();
        if (w == null || w.name == null) { toast("No city loaded yet"); return; }
        toast(prefs.addFavorite(w.name) ? w.name + " added to favorites" : w.name + " is already a favorite");
        refreshFavorites();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        if (v.getTag() instanceof String) {
            contextCity = (String) v.getTag();
            menu.setHeaderTitle(contextCity);
            new MenuInflater(requireContext()).inflate(R.menu.menu_context_city, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (contextCity == null) return super.onContextItemSelected(item);
        int id = item.getItemId();
        if (id == R.id.ctx_delete) {
            prefs.removeFavorite(contextCity);
            toast("Removed " + contextCity);
        } else if (id == R.id.ctx_default) {
            prefs.setDefaultCity(contextCity);
            toast(contextCity + " is now your default city");
        } else {
            return super.onContextItemSelected(item);
        }
        refreshFavorites();
        return true;
    }

    // ------------------------------------------------------------------ share (implicit Intent)
    public void share() {
        WeatherModels.CurrentWeather w = vm.current.getValue();
        if (w == null) { toast("Nothing to share yet"); return; }
        String text = String.format(Locale.getDefault(),
                "Weather in %s: %d° (%s). Humidity %d%%.",
                w.name, Math.round(w.main.temp), w.weather.get(0).description, w.main.humidity);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(i, "Share forecast"));
    }

    // ------------------------------------------------------------------ GPS
    private void requestLocation() {
        boolean fine = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) fetchLocation();
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        swipe.setRefreshing(true);
        FusedLocationProviderClient c = LocationServices.getFusedLocationProviderClient(requireContext());
        c.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (!isAdded()) return;
                    if (loc != null) loadByCoord(loc.getLatitude(), loc.getLongitude());
                    else { swipe.setRefreshing(false); toast("Location unavailable"); }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    swipe.setRefreshing(false);
                    toast("Could not get location");
                });
    }

    // ------------------------------------------------------------------ lifecycle: auto refresh + shake
    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(autoRefresh, AUTO_REFRESH_MS);
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor acc = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acc != null) sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefresh);
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (isHidden() || !isAdded()) return;
        double g = Math.sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / 9.81;
        long now = System.currentTimeMillis();
        if (g > 2.7 && now - lastShake > 2000) {
            lastShake = now;
            toast("Shake detected - refreshing");
            refresh();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void toast(String m) {
        if (getContext() != null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ details grid adapter
    private static class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.VH> {
        private final List<String[]> data = new ArrayList<>();

        void set(List<String[]> l) { data.clear(); data.addAll(l); notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_detail_card, p, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int i) {
            h.title.setText(data.get(i)[0]);
            h.value.setText(data.get(i)[1]);
        }
        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView title, value;
            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvTitle);
                value = v.findViewById(R.id.tvValue);
            }
        }
    }
}
