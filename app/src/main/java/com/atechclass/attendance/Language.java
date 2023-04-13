package com.atechclass.attendance;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

public class Language {
    Locale locale;
    Context context;
    private static final String LANGUAGE = "language";

    public Language(Context context) {
        this.context = context;
    }

    public void Language(){
        SharedPreferences sp_language = context.getSharedPreferences(LANGUAGE, Context.MODE_PRIVATE );
        String check = sp_language.getString(LANGUAGE, "Vietnamese");
        switch (check) {
            case "English":
                locale = new Locale("en");
                ChangeLanguage(locale);
        //        refreshResource((ViewGroup) view);
                break;
            case "Japanese":
                locale = new Locale("ja");
                ChangeLanguage(locale);
       //         refreshResource((ViewGroup) view);
                break;
            case "Chinese":
                locale = new Locale("ii");
                ChangeLanguage(locale);
       //         refreshResource((ViewGroup) view);
                break;
            case "Korean":
                locale = new Locale("ko");
                ChangeLanguage(locale);
    //            refreshResource((ViewGroup) view);
                break;
            case "Vietnamese":
                locale = new Locale("vi");
                ChangeLanguage(locale);
//                refreshResource((ViewGroup) view);
                break;
        }

    }
    private void ChangeLanguage(Locale locale) {
        Locale.setDefault(locale);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
    }

    private void refreshResource(ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof ViewGroup)
                refreshResource((ViewGroup) view);
            else {
                if(view.getTag()==null){
                    continue;
                }
                int resId = context.getResources().getIdentifier(view.getTag().toString(), "string", context.getPackageName());
                if (resId==0)
                    continue;
                if(view instanceof EditText){
                    EditText editText=(EditText)view;
                    editText.setHint(context.getString(resId));

                }else if(view instanceof TextView){
                    TextView textView=(TextView) view;
                    textView.setText(context.getString(resId));
                }else if(view instanceof Button){
                    Button button=(Button) view;
                    button.setText(context.getString(resId));
                }
            }
        }
    }

}
