package com.atechclass.attendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.interfaces.IOnClickItemClass;
import com.atechclass.attendance.R;
import com.atechclass.attendance.model.ClassModel;

import java.util.List;

public class ClassRecyclerViewAdapter extends RecyclerView.Adapter<ClassRecyclerViewAdapter.ClassViewHolder>{

    private List<ClassModel> mItemList;
    IOnClickItemClass iOnClickItemClass;

    public ClassRecyclerViewAdapter(List<ClassModel> list, IOnClickItemClass iOnClickItemClass) {
        this.mItemList = list;
        this.iOnClickItemClass = iOnClickItemClass;
    }

    public void updateList(List<ClassModel> newList) {
        this.mItemList = newList;
    }

    public List<ClassModel> getList() {
        return mItemList;
    }
    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent,false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassModel classModel = mItemList.get(position);
        if(classModel == null) return;

        holder.title.setText(classModel.getSubject());

        holder.tableLayout.setOnClickListener(view -> iOnClickItemClass.onClick(classModel));
        holder.imageView.setOnClickListener(view -> iOnClickItemClass.onClickPopMenu(holder, classModel, position));
    }

    @Override
    public int getItemCount() {
        return mItemList != null ? mItemList.size() : 0;
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TableLayout tableLayout;
        ImageView imageView;
        public PopupMenu popupMenu;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tableLayout = itemView.findViewById(R.id.tableLayout);
            title = itemView.findViewById(R.id.title_list_class);
            imageView = itemView.findViewById(R.id.icMenu);
        }
    }
}
