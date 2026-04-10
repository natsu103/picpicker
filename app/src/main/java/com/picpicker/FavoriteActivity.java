package com.picpicker;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private RelativeLayout gridMode;
    private RelativeLayout browseMode;
    private GridView gridFavorites;
    private TextView tvTitle;
    private Button btnBack;
    private ViewPager2 viewPager;
    private SwipeInterceptorLayout swipeLayout;
    private TextView tvBrowseInfo;
    private PhotoAdapter browseAdapter;

    private List<Uri> favoriteList;
    private boolean isBrowseMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        gridMode = findViewById(R.id.grid_mode);
        browseMode = findViewById(R.id.browse_mode);
        gridFavorites = findViewById(R.id.grid_favorites);
        tvTitle = findViewById(R.id.tv_favorite_title);
        btnBack = findViewById(R.id.btn_back);
        viewPager = findViewById(R.id.view_pager);
        swipeLayout = findViewById(R.id.swipe_layout);
        tvBrowseInfo = findViewById(R.id.tv_browse_info);

        favoriteList = getIntent().getParcelableArrayListExtra("favoriteList");
        if (favoriteList == null) {
            favoriteList = new ArrayList<>();
        }

        updateGridTitle();
        FavoriteGridAdapter gridAdapter = new FavoriteGridAdapter();
        gridFavorites.setAdapter(gridAdapter);

        gridFavorites.setOnItemClickListener((parent, view, position, id) -> {
            enterBrowseMode(position);
        });

        btnBack.setOnClickListener(v -> finish());

        swipeLayout.setOnSwipeListener(new SwipeInterceptorLayout.OnSwipeListener() {
            @Override
            public void onSwipeUp() {
            }

            @Override
            public void onSwipeDown() {
                unfavoriteCurrent();
            }

            @Override
            public void onDrag(float deltaY) {
                handleDrag(deltaY);
            }

            @Override
            public void onDragReset() {
                handleDragReset();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateBrowseInfo();
            }
        });
    }

    private void enterBrowseMode(int position) {
        isBrowseMode = true;
        gridMode.setVisibility(View.GONE);
        browseMode.setVisibility(View.VISIBLE);

        getWindow().setStatusBarColor(android.graphics.Color.BLACK);

        browseAdapter = new PhotoAdapter(this, favoriteList);
        viewPager.setAdapter(browseAdapter);
        viewPager.setCurrentItem(position, false);
        updateBrowseInfo();
    }

    private void exitBrowseMode() {
        isBrowseMode = false;
        browseMode.setVisibility(View.GONE);
        gridMode.setVisibility(View.VISIBLE);

        getWindow().setStatusBarColor(getResources().getColor(R.color.purple_700, null));

        updateGridTitle();
        FavoriteGridAdapter gridAdapter = new FavoriteGridAdapter();
        gridFavorites.setAdapter(gridAdapter);
    }

    private void handleDrag(float deltaY) {
        View currentView = getCurrentView();
        if (currentView == null) return;

        currentView.setTranslationY(deltaY);

        View overlayFavorite = currentView.findViewById(R.id.overlay_favorite);

        float progress = Math.min(1f, Math.abs(deltaY) / SwipeInterceptorLayout.COMMIT_THRESHOLD);

        if (deltaY > 0) {
            overlayFavorite.setVisibility(View.VISIBLE);
            overlayFavorite.setAlpha(progress * 0.7f);
        } else {
            overlayFavorite.setVisibility(View.GONE);
        }
    }

    private void handleDragReset() {
        View currentView = getCurrentView();
        if (currentView == null) return;

        View overlayFavorite = currentView.findViewById(R.id.overlay_favorite);

        currentView.animate()
                .translationY(0f)
                .setDuration(200)
                .start();

        if (overlayFavorite.getVisibility() == View.VISIBLE) {
            overlayFavorite.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> overlayFavorite.setVisibility(View.GONE))
                    .start();
        }
    }

    private void unfavoriteCurrent() {
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= favoriteList.size()) {
            return;
        }

        Uri uri = favoriteList.get(currentPosition);

        View currentView = getCurrentView();
        if (currentView != null) {
            currentView.animate()
                    .translationY(currentView.getHeight() * 1.5f)
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        resetViewState(currentView);
                        removeFavorite(uri, currentPosition);
                    })
                    .start();
        } else {
            removeFavorite(uri, currentPosition);
        }
    }

    private void removeFavorite(Uri uri, int position) {
        favoriteList.remove(position);

        List<Uri> savedFavorites = HomeActivity.getFavorites(this);
        savedFavorites.remove(uri);
        HomeActivity.saveFavorites(this, savedFavorites);

        if (favoriteList.isEmpty()) {
            Toast.makeText(this, getString(R.string.unfavorited_empty), Toast.LENGTH_SHORT).show();
            exitBrowseMode();
            return;
        }

        browseAdapter = new PhotoAdapter(this, favoriteList);
        viewPager.setAdapter(browseAdapter);

        int newPosition = position;
        if (newPosition >= favoriteList.size()) {
            newPosition = favoriteList.size() - 1;
        }
        viewPager.setCurrentItem(newPosition, false);
        updateBrowseInfo();

        Toast.makeText(this, getString(R.string.unfavorited), Toast.LENGTH_SHORT).show();
    }

    private void resetViewState(View view) {
        view.setTranslationY(0f);
        view.setAlpha(1f);
        View overlayDelete = view.findViewById(R.id.overlay_delete);
        View overlayFavorite = view.findViewById(R.id.overlay_favorite);
        if (overlayDelete != null) {
            overlayDelete.setVisibility(View.GONE);
            overlayDelete.setAlpha(0f);
        }
        if (overlayFavorite != null) {
            overlayFavorite.setVisibility(View.GONE);
            overlayFavorite.setAlpha(0f);
        }
    }

    private void updateGridTitle() {
        tvTitle.setText(getString(R.string.favorites_title, favoriteList.size()));
    }

    private void updateBrowseInfo() {
        if (favoriteList.isEmpty()) return;
        int current = viewPager.getCurrentItem() + 1;
        tvBrowseInfo.setText(current + "/" + favoriteList.size());
    }

    private View getCurrentView() {
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        if (recyclerView != null) {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());
            if (viewHolder != null) {
                return viewHolder.itemView;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (isBrowseMode) {
            exitBrowseMode();
        } else {
            super.onBackPressed();
        }
    }

    private class FavoriteGridAdapter extends android.widget.BaseAdapter {

        @Override
        public int getCount() {
            return favoriteList.size();
        }

        @Override
        public Object getItem(int position) {
            return favoriteList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_photo, parent, false);
            }

            ImageView imageView = convertView.findViewById(R.id.iv_photo);
            convertView.findViewById(R.id.cb_select).setVisibility(View.GONE);

            Uri uri = favoriteList.get(position);
            Glide.with(FavoriteActivity.this).load(uri).into(imageView);

            return convertView;
        }
    }
}
