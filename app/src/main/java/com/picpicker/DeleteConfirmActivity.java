package com.picpicker;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DeleteConfirmActivity extends AppCompatActivity {

    private static final int REQUEST_DELETE_CONSENT = 100;

    private GridView gridView;
    private Button btnCancelAll;
    private Button btnConfirmDelete;
    private TextView tvTitle;
    private List<Uri> deleteList;
    private List<Boolean> selectedList;
    private PhotoGridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_confirm);

        gridView = findViewById(R.id.grid_view);
        btnCancelAll = findViewById(R.id.btn_cancel_all);
        btnConfirmDelete = findViewById(R.id.btn_confirm_delete);
        tvTitle = findViewById(R.id.tv_delete_title);

        deleteList = getIntent().getParcelableArrayListExtra("deleteList");
        if (deleteList == null) {
            deleteList = new ArrayList<>();
        }

        selectedList = new ArrayList<>();
        for (int i = 0; i < deleteList.size(); i++) {
            selectedList.add(true);
        }

        tvTitle.setText(getString(R.string.review_delete_title, deleteList.size()));

        adapter = new PhotoGridAdapter();
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            CheckBox checkBox = view.findViewById(R.id.cb_select);
            checkBox.setChecked(!checkBox.isChecked());
            selectedList.set(position, checkBox.isChecked());
        });

        btnCancelAll.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnConfirmDelete.setOnClickListener(v -> deleteSelectedPhotos());
    }

    private void deleteSelectedPhotos() {
        List<Uri> toDelete = new ArrayList<>();
        for (int i = 0; i < deleteList.size(); i++) {
            if (selectedList.get(i)) {
                toDelete.add(deleteList.get(i));
            }
        }

        if (toDelete.isEmpty()) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestDeleteConsent(toDelete);
        } else {
            int deletedCount = 0;
            for (Uri uri : toDelete) {
                if (deletePhotoDirectly(uri)) {
                    deletedCount++;
                }
            }
            removeDeletedFromFavorites(toDelete);
            Toast.makeText(this, getString(R.string.deleted_count, deletedCount), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    private void requestDeleteConsent(List<Uri> toDelete) {
        try {
            PendingIntent pendingIntent = android.provider.MediaStore.createDeleteRequest(
                    getContentResolver(), toDelete);
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_DELETE_CONSENT, null, 0, 0, 0);
        } catch (Exception e) {
            e.printStackTrace();
            int deletedCount = 0;
            for (Uri uri : toDelete) {
                if (deletePhotoDirectly(uri)) {
                    deletedCount++;
                }
            }
            Toast.makeText(this, getString(R.string.deleted_count, deletedCount), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    private boolean deletePhotoDirectly(Uri uri) {
        try {
            return getContentResolver().delete(uri, null, null) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void removeDeletedFromFavorites(List<Uri> deletedUris) {
        if (deletedUris.isEmpty()) return;
        List<Uri> favorites = HomeActivity.getFavorites(this);
        favorites.removeAll(deletedUris);
        HomeActivity.saveFavorites(this, favorites);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DELETE_CONSENT) {
            int deletedCount = 0;
            List<Uri> actuallyDeleted = new ArrayList<>();
            if (resultCode == RESULT_OK) {
                for (int i = 0; i < deleteList.size(); i++) {
                    if (selectedList.get(i)) {
                        deletedCount++;
                        actuallyDeleted.add(deleteList.get(i));
                    }
                }
            }
            removeDeletedFromFavorites(actuallyDeleted);
            Toast.makeText(this, getString(R.string.deleted_count, deletedCount), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    private class PhotoGridAdapter extends android.widget.BaseAdapter {

        @Override
        public int getCount() {
            return deleteList.size();
        }

        @Override
        public Object getItem(int position) {
            return deleteList.get(position);
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
            CheckBox checkBox = convertView.findViewById(R.id.cb_select);

            Uri uri = deleteList.get(position);
            Glide.with(DeleteConfirmActivity.this).load(uri).into(imageView);
            checkBox.setChecked(selectedList.get(position));

            return convertView;
        }
    }
}
