package com.atechclass.attendance.ultis;

import android.app.Activity;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.atechclass.attendance.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;

public class PieCharAnalysis {

    public static PieDataSet dataSet;
    public static PieData pieData;
    public static ArrayList<PieEntry> pieEntries;
    private static Activity context;

    private static final int[] colorStatistical = {rgb("#53C781"), rgb("#F27373"), rgb("#F8FC2A")};
    private static final int[] colorLesson = {rgb("#8FB2D9"), rgb("#E4EFF5"), rgb("#E4EFF5")};

    // Set màu cho thống kê số buổi học
    public static void initPieLesson(Activity activity, int data[]) {
        context = activity;
        getEntries(data);
        dataSet = new PieDataSet(pieEntries, "");
        pieData = new PieData(dataSet);
        dataSet.setColors(colorLesson);
        dataSet.setValueTextSize(0f);
        dataSet.setSliceSpace(0f);
        dataSet.setValueTextColor(activity.getResources().getColor(R.color.black));
        dataSet.setValueTextColors(getColor(activity, data));
    }

    // Set màu cho thống kê biểu đồ tròn từng tuân/tháng/tất cả
    public static void initPieStatic(Activity context, int data[]) {
        getEntries(data);
        dataSet = new PieDataSet(pieEntries, "");
        pieData = new PieData(dataSet);
        dataSet.setColors(colorStatistical);
        dataSet.setValueTextSize(0f);
        dataSet.setSliceSpace(0f);
        dataSet.setValueTextColor(context.getResources().getColor(R.color.text_color_primary));
        dataSet.setValueTextColors(getColor(context, data));
    }

    // Chia phần trăm cho thống kê số buổi học/ thống kê biểu đồ tòn cho tuần/tháng/tất cả
    public static void getStatistical(PieChart pStatistical) {
        pStatistical.setData(pieData);
        dataSet.setValueFormatter(new PercentFormatter(pStatistical));
        pStatistical.setHoleColor(ContextCompat.getColor(context, R.color.background_fragment));
        pStatistical.setHoleRadius(50);
        pStatistical.setDrawHoleEnabled(true);
        pStatistical.setDescription(null);
        pStatistical.getLegend().setEnabled(false);
        pStatistical.setClickable(false);
        pStatistical.setEnabled(false);
        pStatistical.setRotationEnabled(false);
        pStatistical.animateXY(500, 500);
    }

    // Độ lớn nhỏ của thống kê biểu đồ tròn
    public static void setCirleWidth(PieChart pStatistical, int value, boolean enabled) {
        pStatistical.setHoleRadius(value);
        pStatistical.setDrawHoleEnabled(enabled);
    }

    public static void setUsePercent(PieChart pStatistical, boolean enable) {
        dataSet.setValueTextSize(10f);
        pStatistical.setEntryLabelTextSize(10f);
        pStatistical.setUsePercentValues(enable);
    }

    private static List<Integer> getColor(Activity context, int[] data) {
        List<Integer> colors = new ArrayList<>();
        if (data[0] == 0)
            colors.add(ContextCompat.getColor(context, R.color.transparent));
        else
            colors.add(ContextCompat.getColor(context, R.color.white));
        if (data[1] == 0)
            colors.add(ContextCompat.getColor(context, R.color.transparent));
        else
            colors.add(ContextCompat.getColor(context, R.color.black));
        if (data[2] == 0)
            colors.add(ContextCompat.getColor(context, R.color.transparent));
        else
            colors.add(ContextCompat.getColor(context, R.color.black));
        return colors;
    }

    public static void getEntries(int[] study) {
        pieEntries = new ArrayList<>();
        for(int i = 0; i < study.length; i++) {
            pieEntries.add(new PieEntry(study[i]));
        }
    }

    private static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }
}
