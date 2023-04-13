package com.atechclass.attendance.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.IOnClickStudent;
import com.atechclass.attendance.model.StudentModel;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class StudentRecyclerViewAdapter extends RecyclerView.Adapter<StudentRecyclerViewAdapter.StudentViewHolder> {

    public List<StudentModel> students;
    private IOnClickStudent iOnClickStudent;
    private Context context;
    private String uID;
    private String subjectID;
    private static final String SUBJECTS_PATH = "Subjects";
    private StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    public StudentRecyclerViewAdapter(List<StudentModel> students, IOnClickStudent iOnClickStudent, Context context, String uID, String subjectID) {
        this.students = students;
        this.iOnClickStudent = iOnClickStudent;
        this.context = context;
        this.uID = uID;
        this.subjectID = subjectID;
        setHasStableIds(true);
    }

    public void updateList(List<StudentModel> mList) {
        this.students = mList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentModel studentModel = students.get(position);

        String fullName;
        if (studentModel.getLastName() != null){
            fullName = studentModel.getLastName() + " " + studentModel.getName();
        } else {
            fullName = studentModel.getName();
        }
        holder.name.setText(fullName.trim());

//        String number = String.valueOf(position+1);
//
//        if (position < 9){
//            number = "0" + number;
//        }

        Map<String, Integer> lessonMap = studentModel.getLessonMap();
        int present = 0;
        int late = 0;
        for (Map.Entry<String, Integer> set: lessonMap.entrySet()){
            int state = set.getValue();
            switch (state){
                case 0:
                    break;
                case 1:
                    present++;
                    break;
                case 2:
                default:
                    late ++;
            }
        }

        String progressText = present + "/" + lessonMap.size() + " ";
        holder.progress.setText(progressText);

        float progressValue = 1f*present/lessonMap.size()*100;
        float secondProgressValue = progressValue + 1f*late/lessonMap.size()*100;

        holder.progressBar.setProgress((int) progressValue);
        holder.progressBar.setSecondaryProgress((int) secondProgressValue);

        String idText = " " + studentModel.getId();
        holder.id.setText(idText);
        holder.avatar.setImageResource(R.drawable.img_item_class);
        String path = uID + "/" + SUBJECTS_PATH + "/" +
                subjectID + "/" + studentModel.getId() + "/avatar"+ "/" + "avatar.jpg";
        storageReference.child(path).getDownloadUrl()
                .addOnSuccessListener(uri -> Picasso.get().load(uri)
                            .placeholder(R.drawable.img_item_class).error(R.drawable.img_item_class)
                            .into(holder.avatar))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        holder.btnEdit.setOnClickListener(view -> iOnClickStudent.onClickPopMenu(holder, studentModel, position,
                                task.getResult()));
                    else
                        holder.btnEdit.setOnClickListener(view -> iOnClickStudent.onClickPopMenu(holder, studentModel, position,
                                null));
                });
//        if (studentModel.getAvatar() != null && !studentModel.getAvatar().equals("")) {
//            String avatarString = studentModel.getAvatar();
//            byte[] decodedString = Base64.decode(avatarString, Base64.URL_SAFE);
//            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//            holder.avatar.setImageBitmap(decodedByte);
//        } else
//            holder.avatar.setImageResource(R.drawable.img_item_class);


        holder.student.setOnClickListener(view -> iOnClickStudent.onClick(studentModel));
    }

    @Override
    public int getItemCount() {
        return students != null ? students.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder{
        TextView serial;
        TextView id;
        TextView progress;
        TextView name;
        ImageView btnEdit;
        ViewGroup student;
        ProgressBar progressBar;
        CircleImageView avatar;
        public PopupMenu popupMenu;

        public StudentViewHolder(@NonNull View view){
            super(view);
            serial = view.findViewById(R.id.serial);
            name = view.findViewById(R.id.name);
            btnEdit = view.findViewById(R.id.btn_edit);
            student = view.findViewById(R.id.student);
            id = view.findViewById(R.id.tv_student_id);
            progress = view.findViewById(R.id.progress);
            progressBar = view.findViewById(R.id.progressBar);
            avatar = view.findViewById(R.id.circle);
        }
    }
}
