package com.picpicker;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1001;
    private static final int REQUEST_DELETE_CONFIRM = 1;
    private static final String MODE_ALL = "all";
    private static final String MODE_BATCH = "batch";
    private static final int BATCH_SIZE = 20;

    private ViewPager2 viewPager;
    private SwipeInterceptorLayout swipeLayout;
    private PhotoAdapter adapter;
    private List<Uri> photoUris;
    private List<Uri> deleteList;
    private List<Uri> favoriteList;
    private TextView tvPhotoInfo;
    private Button btnDone;
    private String browseMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(android.graphics.Color.BLACK);

        viewPager = findViewById(R.id.view_pager);
        swipeLayout = findViewById(R.id.swipe_layout);
        tvPhotoInfo = findViewById(R.id.tv_photo_info);
        btnDone = findViewById(R.id.btn_done);

        deleteList = new ArrayList<>();
        favoriteList = new ArrayList<>();

        browseMode = getIntent().getStringExtra("browse_mode");
        if (browseMode == null) {
            browseMode = MODE_ALL;
        }

        if (checkPermission()) {
            loadPhotos();
        } else {
            requestPermission();
        }

        btnDone.setOnClickListener(v -> finishBrowse());

        ImageButton btnHelp = findViewById(R.id.btn_help);
        btnHelp.setOnClickListener(v -> showGestureTutorial());
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPhotos();
        } else {
            Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadPhotos() {
        photoUris = getPhotoUris();
        if (photoUris.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_photos), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Collections.shuffle(photoUris);

        if (MODE_BATCH.equals(browseMode) && photoUris.size() > BATCH_SIZE) {
            photoUris = new ArrayList<>(photoUris.subList(0, BATCH_SIZE));
        }

        adapter = new PhotoAdapter(this, photoUris);
        viewPager.setAdapter(adapter);

        updatePhotoInfo();
        setupSwipeListener();
    }

    private List<Uri> getPhotoUris() {
        List<Uri> uris = new ArrayList<>();
        String[] projection = {MediaStore.Images.Media._ID};
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                    Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    uris.add(uri);
                }
            }
        }
        return uris;
    }

    private void setupSwipeListener() {
        swipeLayout.setOnSwipeListener(new SwipeInterceptorLayout.OnSwipeListener() {
            @Override
            public void onSwipeUp() {
                markForDelete();
            }

            @Override
            public void onSwipeDown() {
                markAsFavorite();
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
                updatePhotoInfo();
            }
        });
    }

    private void handleDrag(float deltaY) {
        View currentView = getCurrentView();
        if (currentView == null) return;

        currentView.setTranslationY(deltaY);

        View overlayDelete = currentView.findViewById(R.id.overlay_delete);
        View overlayFavorite = currentView.findViewById(R.id.overlay_favorite);

        float progress = Math.min(1f, Math.abs(deltaY) / SwipeInterceptorLayout.COMMIT_THRESHOLD);

        if (deltaY < 0) {
            overlayDelete.setVisibility(View.VISIBLE);
            overlayDelete.setAlpha(progress * 0.7f);
            overlayFavorite.setVisibility(View.GONE);
        } else {
            overlayFavorite.setVisibility(View.VISIBLE);
            overlayFavorite.setAlpha(progress * 0.7f);
            overlayDelete.setVisibility(View.GONE);
        }
    }

    private void handleDragReset() {
        View currentView = getCurrentView();
        if (currentView == null) return;

        View overlayDelete = currentView.findViewById(R.id.overlay_delete);
        View overlayFavorite = currentView.findViewById(R.id.overlay_favorite);

        currentView.animate()
                .translationY(0f)
                .setDuration(200)
                .start();

        if (overlayDelete.getVisibility() == View.VISIBLE) {
            overlayDelete.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> overlayDelete.setVisibility(View.GONE))
                    .start();
        }
        if (overlayFavorite.getVisibility() == View.VISIBLE) {
            overlayFavorite.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> overlayFavorite.setVisibility(View.GONE))
                    .start();
        }
    }

    private void updatePhotoInfo() {
        if (photoUris.isEmpty()) {
            tvPhotoInfo.setText("0/0");
            return;
        }
        int current = viewPager.getCurrentItem() + 1;
        int total = photoUris.size();
        tvPhotoInfo.setText(current + "/" + total);
    }

    private void markForDelete() {
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= photoUris.size()) {
            return;
        }

        Uri uri = photoUris.get(currentPosition);
        deleteList.add(uri);
        photoUris.remove(currentPosition);

        View currentView = getCurrentView();
        if (currentView != null) {
            currentView.animate()
                    .translationY(-currentView.getHeight() * 1.5f)
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        resetViewState(currentView);
                        adapter.notifyDataSetChanged();
                        handleAfterRemove(currentPosition);
                        Toast.makeText(this, getString(R.string.marked_for_delete), Toast.LENGTH_SHORT).show();
                    })
                    .start();
        } else {
            adapter.notifyDataSetChanged();
            handleAfterRemove(currentPosition);
            Toast.makeText(this, getString(R.string.marked_for_delete), Toast.LENGTH_SHORT).show();
        }
    }

    private void markAsFavorite() {
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= photoUris.size()) {
            return;
        }

        Uri uri = photoUris.get(currentPosition);
        favoriteList.add(uri);
        photoUris.remove(currentPosition);

        View currentView = getCurrentView();
        if (currentView != null) {
            currentView.animate()
                    .translationY(currentView.getHeight() * 1.5f)
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        resetViewState(currentView);
                        adapter.notifyDataSetChanged();
                        handleAfterRemove(currentPosition);
                        Toast.makeText(this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
                    })
                    .start();
        } else {
            adapter.notifyDataSetChanged();
            handleAfterRemove(currentPosition);
            Toast.makeText(this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetViewState(View view) {
        view.setTranslationY(0f);
        view.setAlpha(1f);
        View overlayDelete = view.findViewById(R.id.overlay_delete);
        View overlayFavorite = view.findViewById(R.id.overlay_favorite);
        View overlayUnfavorite = view.findViewById(R.id.overlay_unfavorite);
        if (overlayDelete != null) {
            overlayDelete.setVisibility(View.GONE);
            overlayDelete.setAlpha(0f);
        }
        if (overlayFavorite != null) {
            overlayFavorite.setVisibility(View.GONE);
            overlayFavorite.setAlpha(0f);
        }
        if (overlayUnfavorite != null) {
            overlayUnfavorite.setVisibility(View.GONE);
            overlayUnfavorite.setAlpha(0f);
        }
    }

    private void handleAfterRemove(int removedPosition) {
        if (photoUris.isEmpty()) {
            if (MODE_BATCH.equals(browseMode)) {
                showBatchCompleteDialog();
            } else {
                finishBrowse();
            }
            return;
        }

        int newPosition = removedPosition;
        if (newPosition >= photoUris.size()) {
            newPosition = photoUris.size() - 1;
        }

        viewPager.setCurrentItem(newPosition, false);
        updatePhotoInfo();
    }

    private void showBatchCompleteDialog() {
        saveFavorites();
        if (!deleteList.isEmpty()) {
            Intent intent = new Intent(MainActivity.this, DeleteConfirmActivity.class);
            intent.putExtra("deleteList", new ArrayList<>(deleteList));
            intent.putExtra("batch_mode", true);
            startActivityForResult(intent, REQUEST_DELETE_CONFIRM);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.batch_complete_title)
                    .setPositiveButton(R.string.batch_next_group, (dialog, which) -> reloadBatch())
                    .setNegativeButton(R.string.batch_finish, (dialog, which) -> {
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void reloadBatch() {
        favoriteList.clear();
        deleteList.clear();
        loadPhotos();
    }

    private void finishBrowse() {
        saveFavorites();
        if (!deleteList.isEmpty()) {
            Intent intent = new Intent(MainActivity.this, DeleteConfirmActivity.class);
            intent.putExtra("deleteList", new ArrayList<>(deleteList));
            startActivityForResult(intent, REQUEST_DELETE_CONFIRM);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void saveFavorites() {
        List<Uri> existingFavorites = HomeActivity.getFavorites(this);
        existingFavorites.addAll(favoriteList);
        HomeActivity.saveFavorites(this, existingFavorites);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DELETE_CONFIRM) {
            if (MODE_BATCH.equals(browseMode)) {
                deleteList.clear();
                new AlertDialog.Builder(this)
                        .setTitle(R.string.batch_complete_title)
                        .setPositiveButton(R.string.batch_next_group, (dialog, which) -> reloadBatch())
                        .setNegativeButton(R.string.batch_finish, (dialog, which) -> {
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    private void showGestureTutorial() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gesture_tutorial, null);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setCancelable(true);

        dialogView.setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.card_tutorial).setOnClickListener(v -> {});

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        finishBrowse();
    }
}
