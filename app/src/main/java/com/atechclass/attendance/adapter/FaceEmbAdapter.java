package com.atechclass.attendance.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.OnClickItem;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FaceEmbAdapter extends RecyclerView.Adapter<FaceEmbAdapter.ViewHolder>{
    List<StorageReference> listURL;
    OnClickItem onClickItem;
    public FaceEmbAdapter(List<StorageReference> list, OnClickItem onClickItem) {
        this.listURL = list;
        this.onClickItem = onClickItem;
    }

    public void setList(List<StorageReference> list) {
        this.listURL = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_data_face, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorageReference gsReference = listURL.get(position);
        gsReference.getDownloadUrl().addOnSuccessListener(uri -> {
            Picasso.get().load(uri)
                    .placeholder(R.drawable.img_item_class).error(R.drawable.img_item_class)
                    .into(holder.imgEmb);
        });

        holder.imgEmb.setOnLongClickListener(view -> {
            Vibrator vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(50);
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Dữ liệu gương mặt")
                    .setMessage("Bạn có muốn xóa dữ liệu gương mặt này không?")
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        onClickItem.deleteItem(position);
                    })
                    .setNegativeButton("Hủy", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    }).show();
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return listURL.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgEmb;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgEmb = itemView.findViewById(R.id.img_data_face);
        }
    }
}
