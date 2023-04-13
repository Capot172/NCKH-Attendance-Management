package com.atechclass.attendance.interfaces;

import com.atechclass.attendance.adapter.LessonAdapter;
import com.atechclass.attendance.model.LessonModel;

public interface IOnClickItemLessons {
    void onClickPopupMenu(LessonAdapter.SubSubjectViewHolder holder, LessonModel lessonModel, String nameLesson);
    void onClick(LessonModel lessonModel);
}
