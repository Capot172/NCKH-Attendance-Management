package com.atechclass.attendance.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.atechclass.attendance.function.class_list.Lessons;
import com.atechclass.attendance.function.class_list.Student.ListStudents;
import com.atechclass.attendance.function.class_list.Reports;

public class ClassListAdapter extends FragmentStateAdapter {

    private ListStudents listStudents= new ListStudents();
    private Reports reports= new Reports();
    private Lessons lessons= new Lessons();

    public ClassListAdapter(@NonNull FragmentActivity fragmentActivity, Bundle bundle) {
        super(fragmentActivity);

        this.listStudents.setArguments(bundle);
        this.reports.setArguments(bundle);
        this.lessons.setArguments(bundle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1: return reports;
            case 2: return listStudents;
            default: return lessons;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
