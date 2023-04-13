package com.atechclass.attendance.ultis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atechclass.attendance.interfaces.IOnChange;

public class MyReceiver extends BroadcastReceiver{
    IOnChange onChange;

    public void initReceverListemner(IOnChange onChange) {
        this.onChange = onChange;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(action.equals(Intent.ACTION_TIME_CHANGED) ||
                action.equals(Intent.ACTION_TIMEZONE_CHANGED) && onChange != null) {
            onChange.isChange(true);
        }
    }
}
