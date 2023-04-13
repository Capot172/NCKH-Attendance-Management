package com.atechclass.attendance.interfaces;

import com.atechclass.attendance.adapter.ClassRecyclerViewAdapter;
import com.atechclass.attendance.model.ClassModel;

public interface IOnClickItemClass {
    void onClick (ClassModel classModel);
    void onClickPopMenu(ClassRecyclerViewAdapter.ClassViewHolder holder, ClassModel classModel, int position);
}
