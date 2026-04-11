package com.picpicker;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Geocoder;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private Context context;
    private List<Uri> photoUris;

    public PhotoAdapter(Context context, List<Uri> photoUris) {
        this.context = context;
        this.photoUris = photoUris;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo_full, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.itemView.setTranslationY(0f);
        holder.itemView.setAlpha(1f);

        View overlayDelete = holder.itemView.findViewById(R.id.overlay_delete);
        View overlayFavorite = holder.itemView.findViewById(R.id.overlay_favorite);
        View overlayUnfavorite = holder.itemView.findViewById(R.id.overlay_unfavorite);
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

        Uri uri = photoUris.get(position);
        Glide.with(context).load(uri).into(holder.imageView);

        holder.imageView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                showPhotoInfoDialog(photoUris.get(pos));
            }
            return true;
        });
    }

    private void showPhotoInfoDialog(Uri uri) {
        String dateStr = null;
        String timeStr = null;
        String locationStr = null;

        String[] projection = {MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                long dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED));

                long timestamp = dateTaken > 0 ? dateTaken : (dateModified > 0 ? dateModified * 1000 : 0);

                if (timestamp > 0) {
                    LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    dateStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA));
                    timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA));
                }
            }
        } catch (Exception e) {
            // ignore
        }

        if (dateStr == null) {
            dateStr = context.getString(R.string.photo_info_unknown);
            timeStr = "";
        }

        double[] latLng = getGpsFromExif(uri);
        if (latLng != null) {
            locationStr = reverseGeocode(latLng[0], latLng[1]);
        }
        if (locationStr == null) {
            locationStr = context.getString(R.string.photo_info_unknown);
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_photo_info, null);
        TextView tvDate = dialogView.findViewById(R.id.tv_photo_date);
        TextView tvTime = dialogView.findViewById(R.id.tv_photo_time);
        TextView tvLocation = dialogView.findViewById(R.id.tv_photo_location);
        MaterialButton btnGallery = dialogView.findViewById(R.id.btn_open_in_gallery);

        tvDate.setText(dateStr);
        if (timeStr.isEmpty()) {
            tvTime.setVisibility(View.GONE);
        } else {
            tvTime.setText(timeStr);
        }
        tvLocation.setText(locationStr);

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setCancelable(true);

        dialogView.setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.card_info).setOnClickListener(v -> {});

        btnGallery.setOnClickListener(v -> {
            dialog.dismiss();
            openInGallery(uri);
        });

        dialog.show();
    }

    private double[] getGpsFromExif(Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ExifInterface exif = new ExifInterface(is);
            double[] latLong = exif.getLatLong();
            if (latLong != null && latLong[0] != 0 && latLong[1] != 0) {
                return latLong;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String reverseGeocode(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.CHINA);
            var addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                var address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                String locality = address.getLocality();
                String subLocality = address.getSubLocality();
                String featureName = address.getFeatureName();
                if (locality != null) sb.append(locality);
                if (subLocality != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(subLocality);
                }
                if (featureName != null && !featureName.equals(locality) && !featureName.equals(subLocality)) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(featureName);
                }
                if (sb.length() > 0) return sb.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return String.format(Locale.CHINA, "%.4f°%s, %.4f°%s",
                Math.abs(latitude), latitude >= 0 ? "N" : "S",
                Math.abs(longitude), longitude >= 0 ? "E" : "W");
    }

    private void openInGallery(Uri uri) {
        try {
            long id = getMediaStoreId(uri);
            if (id > 0) {
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                Intent intent = new Intent(Intent.ACTION_VIEW, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                context.startActivity(intent);
            }
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    private long getMediaStoreId(Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.Images.Media._ID}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            }
        } catch (Exception e) {
            // ignore
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_full_photo);
        }
    }
}
