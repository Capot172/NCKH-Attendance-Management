package com.atechclass.attendance.OnBroading;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.atechclass.attendance.R;

public class SlideLayout extends PagerAdapter {
    Context context;
    LayoutInflater layoutInflater;
    public  SlideLayout(Context context)
    {
        this.context=context;
    }
    public int[] slide_image={ R.drawable.img_screen_1,R.drawable.img_screen_2,R.drawable.img_screen_3};



    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (RelativeLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        String [] slide_text={
                context.getString(R.string.slg1), context.getString(R.string.slg2), context.getString(R.string.slg3)
        };

        String [] slideTitle = {context.getString(R.string.title1), "", "" };
        String [] slideVersion = {context.getString(R.string.version) ,context.getString(R.string.title2), context.getString(R.string.title3)};
        layoutInflater =(LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.fragment_slide_layout,container,false);

        ImageView slideimgae= view.findViewById(R.id.img_view);
        TextView sildetext= view.findViewById(R.id.third_text);
        TextView txtTitleIntro = view.findViewById(R.id.first_text);
        TextView txtVer = view.findViewById(R.id.second_text);
        slideimgae.setImageResource(slide_image[position]);
        sildetext.setText(slide_text[position]);
        txtTitleIntro.setText(slideTitle[position]);
        txtVer.setText(slideVersion[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout) object);
    }
}