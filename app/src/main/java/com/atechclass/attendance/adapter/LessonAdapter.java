package com.atechclass.attendance.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.transition.AutoTransition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.IOnClickItemLessons;
import com.atechclass.attendance.model.LessonModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.SubSubjectViewHolder>{

    Context context;
    List<LessonModel> list;
    IOnClickItemLessons clickItemLessons;
    AutoTransition autoTransition;
    int dayFirst;
    int dayEnd;
    private final int[] color1 = {rgb("#53C781"), rgb("#F27373"), rgb("#F8FC2A")};

    public LessonAdapter(Context context, List<LessonModel> list, AutoTransition autoTransition, IOnClickItemLessons clickItemLessons) {
        this.context = context;
        this.list = list;
        this.autoTransition = autoTransition;
        this.clickItemLessons = clickItemLessons;
        setHasStableIds(true);
    }

    public void toDay(int dayFirst, int dayEnd) {
        this.dayFirst = dayFirst;
        this.dayEnd = dayEnd;
    }

    @NonNull
    @Override
    public SubSubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson, parent, false);
        return new SubSubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubSubjectViewHolder holder, int position) {

        LessonModel lessonModel = list.get(position);
        initWidgets(holder, lessonModel);
        if(dayFirst == position || dayEnd == position) {
            if (dayFirst == position) {
                holder.maskToday.setVisibility(View.VISIBLE);
                holder.maskToday.setText(R.string.today);
            }
            if (dayEnd == position)
                holder.maskEndDay.setVisibility(View.VISIBLE);
        } else if (dayFirst == -1 && position == 0){
            holder.maskToday.setVisibility(View.VISIBLE);
            holder.maskToday.setText(R.string.noLesson);
        }

        holder.btnMenu.setOnClickListener(view -> clickItemLessons.onClickPopupMenu(holder, lessonModel,
                context.getString(R.string.session) + (position + 1)));
        holder.parent.setOnClickListener(view -> clickItemLessons.onClick(lessonModel));

        int[] percent = { lessonModel.getPresent(), lessonModel.getAbsent(), lessonModel.getLate()};
        if(lessonModel.getPresent() == 0 && lessonModel.getLate() == 0 && lessonModel.getAbsent() == 0) {
            holder.notice.setVisibility(View.GONE);
            holder.ratio.setVisibility(View.GONE);
            holder.viewDetail.setVisibility(View.GONE);
        }
        else {
            holder.notice.setVisibility(View.GONE);
            holder.ratio.setVisibility(View.VISIBLE);
            holder.viewDetail.setVisibility(View.VISIBLE);
            setUpPieChart(holder, percent, color1);
        }
    }

    private ArrayList<PieEntry> pieEntries;
    private void setUpPieChart(SubSubjectViewHolder holder, int[] percent, int[] color) {
        getEntries(percent);
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        PieData pieData = new PieData(pieDataSet);
        pieDataSet.setColors(color);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(0f);
        pieDataSet.setSliceSpace(.5f);
        holder.ratio.setRotationEnabled(false);
        holder.ratio.setData(pieData);
        holder.ratio.setHoleRadius(0);
        holder.ratio.setDrawHoleEnabled(false);
        holder.ratio.setDescription(null);
        holder.ratio.getLegend().setEnabled(false);
        holder.ratio.animateXY(200, 200);
    }

    public int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        return Color.rgb(r, g, b);
    }

    private void getEntries(int[] percent) {
        pieEntries = new ArrayList<>();
        for (int j : percent) {
            pieEntries.add(new PieEntry(j));
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private void initWidgets(@NonNull SubSubjectViewHolder holder, LessonModel lessonModel) {
        SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm", Locale.getDefault().stripExtensions());
        holder.startTime.setText(formatTime.format(lessonModel.getStartTime()));
        holder.endTime.setText(formatTime.format(lessonModel.getEndTime()));
        DateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        Calendar cldDay = Calendar.getInstance();
        cldDay.setTimeInMillis(lessonModel.getDayTime());
        String session = context.getString(R.string.session) + lessonModel.getPosition() + " (" + format.format(cldDay.getTime()) + ")";
        holder.session.setText(session);
        holder.present.setText(String.valueOf(lessonModel.getPresent()));
        holder.absent.setText(String.valueOf(lessonModel.getAbsent()));
        holder.late.setText(String.valueOf(lessonModel.getLate()));
        holder.room.setText(lessonModel.getRoom());
        holder.location.setText(lessonModel.getAddress());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(List<LessonModel> subjects) {
        this.list = subjects;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class SubSubjectViewHolder extends RecyclerView.ViewHolder {

        private TextView maskToday;
        private TextView maskEndDay;
        private TextView session;
        private TextView startTime;
        private TextView endTime;
        private TextView present;
        private TextView absent;
        private TextView late;
        private TextView notice;
        private TextView location;
        private TextView room;
        private ImageButton btnMenu;
        private RelativeLayout viewDetail;
        private ViewGroup parent;
        private PieChart ratio;

        public SubSubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            initWidgets(itemView);
        }

        private void initWidgets(@NonNull View itemView) {
            maskToday = itemView.findViewById(R.id.mask_today);
            maskEndDay = itemView.findViewById(R.id.mask_end_today);
            session = itemView.findViewById(R.id.sub_session);
            startTime = itemView.findViewById(R.id.sub_start_time);
            endTime = itemView.findViewById(R.id.sub_end_time);
            ratio = itemView.findViewById(R.id.sub_percent);
            present = itemView.findViewById(R.id.sub_present);
            absent = itemView.findViewById(R.id.sub_absent);
            btnMenu = itemView.findViewById(R.id.btn_menu);
            viewDetail = itemView.findViewById(R.id.viewDetails);
            parent = itemView.findViewById(R.id.parent_sub);
            late = itemView.findViewById(R.id.sub_late);
            notice = itemView.findViewById(R.id.txt_notice);
            location = itemView.findViewById(R.id.txt_location);
            room = itemView.findViewById(R.id.txt_room);
        }
    }
}
