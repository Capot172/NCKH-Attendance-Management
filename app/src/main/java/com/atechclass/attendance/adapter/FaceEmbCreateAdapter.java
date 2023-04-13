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
import com.atechclass.attendance.model.DataFace;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FaceEmbCreateAdapter extends RecyclerView.Adapter<FaceEmbCreateAdapter.ViewHolder>{
    List<DataFace> listData;
    OnClickItem onClickItem;
    public FaceEmbCreateAdapter(OnClickItem onClickItem) {
        this.onClickItem = onClickItem;
    }

    public void setList(List<DataFace> listData) {
        this.listData = listData;
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
        DataFace face = listData.get(position);

        holder.imgEmb.setImageBitmap(face.getAvatar());

        holder.imgEmb.setOnLongClickListener(view -> {
            Vibrator vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(50);
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Dữ liệu gương mặt")
                    .setMessage("Bạn có muốn xóa dữ liệu ring mặt này không?")
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
        return listData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgEmb;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgEmb = itemView.findViewById(R.id.img_data_face);
        }
    }
}
