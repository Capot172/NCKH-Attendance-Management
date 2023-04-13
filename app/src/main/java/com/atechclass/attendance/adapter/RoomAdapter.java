package com.atechclass.attendance.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atechclass.attendance.R;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RoomAdapter extends ArrayAdapter<String> {
    private final List<String> roomList;
    public RoomAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        roomList = new ArrayList<>(objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_custom_suggest,parent,false);
        }
        TextView tvRoom = convertView.findViewById(R.id.tv_show);
        String room = getItem(position);
        tvRoom.setText(room);
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                List<String> roomListSg = new ArrayList<>();
                if(charSequence == null || charSequence.length() == 0){
                    roomListSg.addAll(roomList);
                } else {
                    String filter = charSequence.toString().toLowerCase().trim();
                    for(String room : roomList){
                        if(removeAccent(room.toLowerCase()).contains(removeAccent(filter))){
                            roomListSg.add(room);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = roomListSg;
                filterResults.count = roomListSg.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                clear();
                addAll((List<String> )filterResults.values);
                notifyDataSetInvalidated();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return  resultValue.toString();
            }
        };
    }

    public static String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }
}
