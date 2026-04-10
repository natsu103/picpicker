package com.picpicker;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

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
        if (overlayDelete != null) {
            overlayDelete.setVisibility(View.GONE);
            overlayDelete.setAlpha(0f);
        }
        if (overlayFavorite != null) {
            overlayFavorite.setVisibility(View.GONE);
            overlayFavorite.setAlpha(0f);
        }

        Uri uri = photoUris.get(position);
        Glide.with(context).load(uri).into(holder.imageView);
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
