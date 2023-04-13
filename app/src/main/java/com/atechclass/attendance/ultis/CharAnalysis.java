package com.atechclass.attendance.ultis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.Log;

import com.atechclass.attendance.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;

public class CharAnalysis extends BarChart {
    Context mContext;
    private final BarChart mBarChart;
    int []days = new int[7];
    List<Integer> textColors;

    public CharAnalysis(Context context, BarChart barChart) {
        super(context);
        this.mContext = context;
        this.mBarChart = barChart;
    }

    public void initBarChart(ArrayList<BarEntry> list) {
        BarDataSet barDataSetWeek = new BarDataSet(list, "Data set");
        barDataSetWeek.setBarShadowColor(mContext.getResources().getColor(R.color.pastel_red));
        barDataSetWeek.setAxisDependency(YAxis.AxisDependency.RIGHT);
        textColors = new ArrayList<>();
        for(int i = 0; i < list.size(); i++) {
            if(i == 0) {
                days[i] = mContext.getResources().getColor(R.color.color_grey);
                textColors.add(getResources().getColor(R.color.black));
            } else if (i == 5) {
                days[i] = mContext.getResources().getColor(R.color.pastel_yellow);
                textColors.add(mContext.getResources().getColor(R.color.pastel_red));
            } else {
                days[i] = mContext.getResources().getColor(R.color.pastel_green);
                textColors.add(getResources().getColor(R.color.black));
            }
        }
        barDataSetWeek.setValueTextColors(textColors);
        mBarChart.setXAxisRenderer(new ColoredLabelAxisRenderer(mBarChart.getViewPortHandler(), mBarChart.getXAxis(), mBarChart.getTransformer(YAxis.AxisDependency.LEFT), textColors));
        barDataSetWeek.setColors(days);
        BarData barData = new BarData(barDataSetWeek);
        barData.setBarWidth(.6f);
        mBarChart.setData(barData);
    }


    public void initBarChart2C(ArrayList<BarEntry> list1, ArrayList<BarEntry> list2, ArrayList<BarEntry> list3) {
        BarDataSet barDataSetPresent = new BarDataSet(list1, "Data set 1");
        barDataSetPresent.setColor(getResources().getColor(R.color.pastel_green));
        BarDataSet barDataSetAbsent = new BarDataSet(list2, "Data set 2");
        barDataSetAbsent.setColor(getResources().getColor(R.color.pastel_red));
        BarDataSet barDataSetLate = new BarDataSet(list3, "Data set 3");
        barDataSetLate.setColor(getResources().getColor(R.color.pastel_yellow));

        barDataSetPresent.setAxisDependency(YAxis.AxisDependency.RIGHT);
        barDataSetAbsent.setAxisDependency(YAxis.AxisDependency.RIGHT);
        barDataSetLate.setAxisDependency(YAxis.AxisDependency.RIGHT);
        BarData barData = new BarData(barDataSetPresent, barDataSetAbsent, barDataSetLate);
        barData.setBarWidth(barWidth);
        mBarChart.setData(barData);
    }

    public void hideLine(boolean hint) {
        mBarChart.setFitBars(true);
        mBarChart.getDescription().setEnabled(false);
        mBarChart.animateY(500);

        mBarChart.setScaleYEnabled(false);
        mBarChart.setScaleXEnabled(false);
        mBarChart.setPinchZoom(false);
        mBarChart.setClickable(false);
        mBarChart.setDoubleTapToZoomEnabled(false);

        mBarChart.setMaxVisibleValueCount(40);
        //định dạng tuần
        if (hint) {
            mBarChart.setDrawBarShadow(false);
            mBarChart.getXAxis().setDrawGridLines(false);
            mBarChart.setMaxVisibleValueCount(1);
            mBarChart.getAxisLeft().setDrawAxisLine(false);
            mBarChart.getAxisLeft().setDrawGridLines(true);
            mBarChart.getAxisRight().setDrawGridLines(false);
        } else
        //định dạng học kì
        {
            mBarChart.setVisibleXRangeMaximum(3);
            mBarChart.getXAxis().setDrawGridLines(true);
        }
//        mBarChart.setViewPortOffsets(-14, 10, 26, 30);
        mBarChart.setExtraOffsets(-5, -10, -5, -5);

        mBarChart.getLegend().setEnabled(false);

        mBarChart.getXAxis().setDrawAxisLine(false);

        mBarChart.getAxisRight().setDrawLabels(false);
        mBarChart.getAxisRight().setDrawAxisLine(false);
        mBarChart.getAxisLeft().setDrawLabels(false);
        mBarChart.setGridBackgroundColor(getResources().getColor(R.color.white));

    }

    // Set chiều cao, số cột, số ngày
    public void setUpAxis(String[] axis) {
        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(axis));
//        xAxis.setTextColor(getResources().getColor(R.color.black));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(7);
        xAxis.setTypeface(Typeface.DEFAULT_BOLD);
        xAxis.setTextSize(12f);
        YAxis rightAxis = mBarChart.getAxisRight();
        rightAxis.setAxisMinimum(0);
        rightAxis.setAxisMaximum(100);
    }

    float barSpace = 0.01f;
    float groupSpace = 0.1f;
    float barWidth = 0.29f;
    public void setUpAxis2C(List<String> axis, int maximum) {
        XAxis xAxis = mBarChart.getXAxis();
        String valuesAxis[] = new String[axis.size()];
        for(int i = 0; i < axis.size(); i++) {
            valuesAxis[i] = axis.get(i);
        }

        if(axis.size() == 1) {
            mBarChart.setVisibleXRangeMaximum(1);
        } else if(axis.size() == 2) {
            mBarChart.setVisibleXRangeMaximum(2);
        }

        xAxis.setValueFormatter(new IndexAxisValueFormatter(valuesAxis));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(valuesAxis.length);
        xAxis.setTextColor(getResources().getColor(R.color.black));
        xAxis.setTypeface(Typeface.DEFAULT_BOLD);
        xAxis.setTextSize(12f);
        xAxis.setCenterAxisLabels(true);

        mBarChart.setDragEnabled(true);
        mBarChart.invalidate();
        mBarChart.groupBars(0, groupSpace, barSpace);

        YAxis rightAxis = mBarChart.getAxisRight();
        rightAxis.setAxisMinimum(0);
        rightAxis.setAxisMaximum(100f);
    }

    public void setUpAxis2CDemo(String[] axis, int maximum) {
        XAxis xAxis = mBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(axis));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(maximum);
        xAxis.setTextColor(getResources().getColor(R.color.black));

        xAxis.setTextSize(9f);
        xAxis.setCenterAxisLabels(true);


        mBarChart.setDragEnabled(true);

        mBarChart.groupBars(0, groupSpace, barSpace);
        mBarChart.invalidate();

        YAxis rightAxis = mBarChart.getAxisRight();
        rightAxis.setAxisMinimum(0);
        rightAxis.setAxisMaximum(100f);
    }

    public static class ColoredLabelAxisRenderer extends XAxisRenderer {
        List<Integer> lableColors;

        public ColoredLabelAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans, List<Integer> colors) {
            super(viewPortHandler, xAxis, trans);
            this.lableColors = colors;
        }

        @Override
        protected void drawLabels(Canvas c, float pos, MPPointF anchor) {
            final float labelRotationAngleDegrees = mXAxis.getLabelRotationAngle();
            boolean centeringEnabled = mXAxis.isCenterAxisLabelsEnabled();

            float[] positions = new float[mXAxis.mEntryCount * 2];

            for (int i = 0; i < positions.length; i += 2) {

                // only fill x values
                if (centeringEnabled) {
                    positions[i] = mXAxis.mCenteredEntries[i / 2];
                } else {
                    positions[i] = mXAxis.mEntries[i / 2];
                }
            }

            mTrans.pointValuesToPixel(positions);

            for (int i = 0; i < positions.length; i += 2) {

                float x = positions[i];

                if (mViewPortHandler.isInBoundsX(x)) {

                    String label = mXAxis.getValueFormatter().getFormattedValue(mXAxis.mEntries[i / 2], mXAxis);
                    int color = getColorForXValue((int) mXAxis.mEntries[i / 2]); //added

                    mAxisLabelPaint.setColor(color);

                    if (mXAxis.isAvoidFirstLastClippingEnabled()) {

                        // avoid clipping of the last
                        if (i == mXAxis.mEntryCount - 1 && mXAxis.mEntryCount > 1) {
                            float width = Utils.calcTextWidth(mAxisLabelPaint, label);

                            if (width > mViewPortHandler.offsetRight() * 2
                                    && x + width > mViewPortHandler.getChartWidth())
                                x -= width / 2;

                            // avoid clipping of the first
                        } else if (i == 0) {

                            float width = Utils.calcTextWidth(mAxisLabelPaint, label);
                            x += width / 2;
                        }
                    }

                    drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees);
                }
            }
        }

        private int getColorForXValue(int index) {
            if (index >= lableColors.size()) return mXAxis.getTextColor();

            if (index < 0) return mXAxis.getTextColor();

            return lableColors.get(index);
        }
    }

}

