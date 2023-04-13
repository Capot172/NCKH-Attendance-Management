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

public class AddressAdapter extends ArrayAdapter<String> {
    private final List<String> list;
    public AddressAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        list = new ArrayList<>(objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_custom_suggest, parent, false);
        }
        TextView tvAddress = convertView.findViewById(R.id.tv_show);
        String address = getItem(position);
        tvAddress.setText(address);
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                List<String> listAddress = new ArrayList<>();
                if(charSequence == null || charSequence.length() == 0){
                    listAddress.addAll(list);
                } else {
                    String filter = charSequence.toString().toLowerCase().trim();
                    for(String address : list){
                        if(removeAccent(address.toLowerCase()).contains(removeAccent(filter))){
                            listAddress.add(address);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = listAddress;
                filterResults.count = listAddress.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                clear();
                addAll((List<String>)filterResults.values);
                notifyDataSetInvalidated();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return resultValue.toString();
            }
        };
    }

    public static String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }
}
