package com.picpicker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_BROWSE = 1;
    private static final int REQUEST_FAVORITES = 2;
    private static final String PREFS_NAME = "picpicker_prefs";
    private static final String KEY_FAVORITES = "favorites";

    private MaterialCardView cardStartBrowse;
    private MaterialCardView cardFavorites;
    private TextView tvFavoriteCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getWindow().setStatusBarColor(android.graphics.Color.BLACK);

        cardStartBrowse = findViewById(R.id.card_start_browse);
        cardFavorites = findViewById(R.id.card_favorites);
        tvFavoriteCount = findViewById(R.id.tv_favorite_count);

        updateFavoriteCount();

        cardStartBrowse.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivityForResult(intent, REQUEST_BROWSE);
        });

        cardFavorites.setOnClickListener(v -> {
            List<Uri> favorites = getFavorites(this);
            if (favorites.isEmpty()) {
                return;
            }
            Intent intent = new Intent(HomeActivity.this, FavoriteActivity.class);
            intent.putExtra("favoriteList", new ArrayList<>(favorites));
            startActivityForResult(intent, REQUEST_FAVORITES);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFavoriteCount();
    }

    private void updateFavoriteCount() {
        List<Uri> favorites = getFavorites(this);
        if (favorites.isEmpty()) {
            tvFavoriteCount.setText(getString(R.string.no_favorites_yet));
        } else {
            tvFavoriteCount.setText(getString(R.string.favorite_count, favorites.size()));
        }
    }

    static void saveFavorites(android.content.Context context, List<Uri> favorites) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FAVORITES + "_size", favorites.size());
        for (int i = 0; i < favorites.size(); i++) {
            editor.putString(KEY_FAVORITES + "_" + i, favorites.get(i).toString());
        }
        editor.apply();
    }

    static List<Uri> getFavorites(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int size = prefs.getInt(KEY_FAVORITES + "_size", 0);
        List<Uri> favorites = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String uriString = prefs.getString(KEY_FAVORITES + "_" + i, null);
            if (uriString != null) {
                favorites.add(Uri.parse(uriString));
            }
        }
        return favorites;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateFavoriteCount();
    }
}
