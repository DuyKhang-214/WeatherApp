package com.example.weatherapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RssFragment extends Fragment {
    private static final String[][] FEEDS = {
            {"BBC - Science & Environment", "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"},
            {"BBC - World", "https://feeds.bbci.co.uk/news/world/rss.xml"},
            {"NASA Earth Observatory", "https://earthobservatory.nasa.gov/feeds/earth-observatory.rss"},
            {"VnExpress - Latest", "https://vnexpress.net/rss/tin-moi-nhat.rss"}
    };

    private Spinner spinner;
    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private RssAdapter adapter;
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_rss, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        spinner = v.findViewById(R.id.spinnerFeed);
        swipe = v.findViewById(R.id.swipeRss);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        RecyclerView rv = v.findViewById(R.id.rvRss);

        adapter = new RssAdapter(this::openLink);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        String[] names = new String[FEEDS.length];
        for (int i = 0; i < FEEDS.length; i++) names[i] = FEEDS[i][0];
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { load(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        swipe.setOnRefreshListener(this::load);
    }

    private void load() {
        final String url = FEEDS[spinner.getSelectedItemPosition()][1];
        swipe.setRefreshing(true);
        tvEmpty.setVisibility(View.GONE);
        executor.execute(() -> {
            List<RssItem> items = null;
            String error = null;
            try {
                Request req = new Request.Builder().url(url).header("User-Agent", "WeatherApp/1.0").build();
                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) throw new IOException("HTTP " + resp.code());
                    items = RssParser.parse(resp.body().byteStream());
                }
            } catch (Exception e) {
                error = e.getMessage() == null ? "Could not load feed" : e.getMessage();
            }
            final List<RssItem> result = items;
            final String err = error;
            Activity a = getActivity();
            if (a == null) return;
            a.runOnUiThread(() -> {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                if (result != null) {
                    adapter.set(result);
                    tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
                    tvEmpty.setText("No articles found");
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Could not load feed: " + err);
                }
            });
        });
    }

    /** Implicit Intent: open the article in the user's browser. */
    private void openLink(RssItem item) {
        if (item.link == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.link)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No browser available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
