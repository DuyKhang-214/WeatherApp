package com.example.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private WeatherFragment weatherFragment;
    private MapFragment mapFragment;
    private RssFragment rssFragment;
    private ProfileFragment profileFragment;
    private Fragment active;
    private Prefs prefs;
    private MenuItem searchItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { openLogin(); return; }

        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getSupportFragmentManager();
        weatherFragment = (WeatherFragment) fm.findFragmentByTag("weather");
        if (weatherFragment == null) {
            weatherFragment = new WeatherFragment();
            mapFragment = new MapFragment();
            rssFragment = new RssFragment();
            profileFragment = new ProfileFragment();
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, weatherFragment, "weather")
                    .add(R.id.fragmentContainer, mapFragment, "map")
                    .add(R.id.fragmentContainer, rssFragment, "rss")
                    .add(R.id.fragmentContainer, profileFragment, "profile")
                    .commitNow();
        } else {
            mapFragment = (MapFragment) fm.findFragmentByTag("map");
            rssFragment = (RssFragment) fm.findFragmentByTag("rss");
            profileFragment = (ProfileFragment) fm.findFragmentByTag("profile");
        }

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> { showForNav(item.getItemId()); return true; });
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_weather);
        else showForNav(nav.getSelectedItemId());

        if (getIntent().getBooleanExtra("welcome", false)) {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            String name = u != null && u.getDisplayName() != null && !u.getDisplayName().isEmpty()
                    ? u.getDisplayName() : (u != null ? u.getEmail() : "");
            Toast.makeText(this, "Welcome, " + name, Toast.LENGTH_SHORT).show();
        }
    }

    private void showForNav(int id) {
        if (id == R.id.nav_map) show(mapFragment, "Precipitation map");
        else if (id == R.id.nav_rss) show(rssFragment, "Weather news");
        else if (id == R.id.nav_profile) show(profileFragment, "Profile");
        else show(weatherFragment, "Weather");
    }

    private void show(Fragment f, String title) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        for (Fragment x : new Fragment[]{weatherFragment, mapFragment, rssFragment, profileFragment}) {
            if (x == f) t.show(x); else t.hide(x);
        }
        t.commit();
        active = f;
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        invalidateOptionsMenu();
    }

    // ---------- Options Menu ----------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        sv.setQueryHint("Search city…");
        sv.setMaxWidth(Integer.MAX_VALUE);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                weatherFragment.onSearchSubmit(q.trim());
                sv.clearFocus();
                return true;
            }
            @Override public boolean onQueryTextChange(String s) {
                weatherFragment.onSearchChanged(s);
                return true;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override public boolean onMenuItemActionExpand(MenuItem item) { return true; }
            @Override public boolean onMenuItemActionCollapse(MenuItem item) {
                weatherFragment.hideSuggestions();
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean w = active == weatherFragment;
        menu.findItem(R.id.action_search).setVisible(w);
        menu.findItem(R.id.action_refresh).setVisible(w);
        menu.findItem(R.id.action_favorite).setVisible(w);
        menu.findItem(R.id.action_share).setVisible(w);
        menu.findItem(prefs.isMetric() ? R.id.unit_metric : R.id.unit_imperial).setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) { weatherFragment.refresh(); return true; }
        if (id == R.id.action_favorite) { weatherFragment.addCurrentToFavorites(); return true; }
        if (id == R.id.action_share) { weatherFragment.share(); return true; }
        if (id == R.id.unit_metric || id == R.id.unit_imperial) {
            prefs.setUnits(id == R.id.unit_metric ? "metric" : "imperial");
            item.setChecked(true);
            weatherFragment.refresh();
            return true;
        }
        if (id == R.id.action_logout) { confirmLogout(); return true; }
        return super.onOptionsItemSelected(item);
    }

    public void collapseSearch() {
        if (searchItem != null) searchItem.collapseActionView();
    }

    public void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Do you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    openLogin();
                })
                .show();
    }

    private void openLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
