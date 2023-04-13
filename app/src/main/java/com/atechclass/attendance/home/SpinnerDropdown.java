package com.atechclass.attendance.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atechclass.attendance.R;
import com.atechclass.attendance.model.SniperHome;

import java.util.ArrayList;

public class SpinnerDropdown extends ArrayAdapter<SniperHome> {
    public SpinnerDropdown(@NonNull Context context, ArrayList<SniperHome> list) {
        super(context, 0, list);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position, View view, ViewGroup parent) {
        if(view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.spinner_dropdown, parent, false);
        }
        TextView textView = view.findViewById(R.id.txt_view_name);

        SniperHome sniperHome = getItem(position);

        if(sniperHome != null) {
            textView.setText(sniperHome.getName());
        }
        return view;
    }
}
