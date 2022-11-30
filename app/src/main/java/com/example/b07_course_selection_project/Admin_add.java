package com.example.b07_course_selection_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.b07_course_selection_project.Course.Course;
import com.example.b07_course_selection_project.MultiSelect.KeyPairBoolData;
import com.example.b07_course_selection_project.databinding.ActivityAdminAddBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Admin_add extends AppCompatActivity {
    ActivityAdminAddBinding binding;

    List<KeyPairBoolData> courseList;



    // Following is for session selector dropdown
    boolean[] selectedSession;
    List<Integer> sessionList = new ArrayList<Integer>();
    String[] sessionArray = {"Fall", "Winter", "Summer"};
    // Above is for session selector dropdown




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.addCourses.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                addCourse();
            }
        });
        clearAll();
        binding.sessionselector.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                showSelector(binding.sessionselector, "Select Sessions");
            }
        });
        fetchCourses();
        binding.spinner.setSearchEnabled(true);
        binding.spinner.setSearchHint("Type here to search!");
        binding.clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAll();
            }
        });

    }
    private void initSele(){
        selectedSession = new boolean[sessionArray.length];
        sessionList.clear();
    }
    private void fetchCourses(){
        List<String> result = new ArrayList<String>();
        FirebaseDatabase.getInstance().getReference("Courses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot data:snapshot.getChildren()){
                    result.add(data.child("code").getValue().toString());
                }
                Collections.sort(result);
                populatePair(result);
                showSearchSelector();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                return;
            }
        });
    }

    private void populatePair(List<String> list){
        courseList = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            KeyPairBoolData k = new KeyPairBoolData();
            k.setId(i+1);
            k.setName(list.get(i));
            k.setSelected(false);
            courseList.add(k);
        }
    }

    private void showSearchSelector(){
        //create alertdialog
        binding.spinner.setItems(courseList, items -> {});


    }

    private void showSelector(TextView v, String text){
        //create alertdialog
        v.setError(null);
        AlertDialog.Builder builder = new AlertDialog.Builder(
                Admin_add.this
        );
        builder.setTitle(text);
        builder.setCancelable(false); //this ignores the back button
        //create a multichoice set
        builder.setMultiChoiceItems(sessionArray, selectedSession, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                if(b){
                    //if clicked then we add into the selected list
                    sessionList.add(i);
                    Collections.sort(sessionList);
                }else{
                    //if not we remove
                    sessionList.remove(Integer.valueOf(i));
                }
            }
        });
        //ok selected
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //make string for display
                StringBuilder stringBuilder = new StringBuilder();
                for(int j = 0; j < sessionList.size(); j++){
                    stringBuilder.append(sessionArray[sessionList.get(j)]);
                    if(j != sessionList.size() - 1){
                        stringBuilder.append(", ");
                    }
                }
                //sets the text
                v.setText(stringBuilder.toString());
            }
        });
        //cancel button in the selector
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss(); //exits menu
            }
        });
        //clear button in the selector
        builder.setNeutralButton("Clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for(int j = 0; j < selectedSession.length; j++){
                    selectedSession[j] = false;
                    sessionList.clear();
                    v.setText("");
                }
            }
        });
        builder.show(); //show the menu
    }
    private void clearAll(){
        binding.coursecode.setText("");
        binding.coursenum.setText("");
        binding.coursename.setText("");
        binding.courselevel.setText("");
        binding.sessionselector.setText("");
        initSele();
        fetchCourses();
    }
    private void addCourse(){
        String coursetype = binding.coursecode.getText().toString().trim();
        String coursenum = binding.coursenum.getText().toString().trim();
        String coursename = binding.coursename.getText().toString().trim();
        if(coursenum.length() == 1){
            coursenum = "0" + coursenum;
        }
        String level = binding.courselevel.getText().toString().trim();
        List<String> sessions = fetchSession();
        List<String> courses = fetchPrereq();
        if(!checkCourseType(coursetype))
            return;
        if(!checkLevel(level))
            return;
        if(!checkCourseNum(coursenum))
            return;
        if(!checkName(coursename))
            return;
        if(!checkSessions(sessions))
            return;
        String courseCode = coursetype + level + coursenum;
        Course temp = new Course(coursename, courseCode, courses, sessions);
        FirebaseDatabase.getInstance().getReference("Courses").child(courseCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    FirebaseDatabase.getInstance().getReference("Courses").child(courseCode).setValue(temp)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        if(!courses.isEmpty())
                                            notifyNewDepender(courseCode, courses);
                                        clearAll();
                                        Toast.makeText(Admin_add.this, "Course added!", Toast.LENGTH_LONG).show();
                                    }
                                    else{
                                        Toast.makeText(Admin_add.this, "Course failed to be added!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
                else{
                    Toast.makeText(Admin_add.this, "Course already exists!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                return;
            }
        });
    }

    private void notifyNewDepender(String coursecode, List<String> courses){
        for(String i:courses){
            FirebaseDatabase.getInstance().getReference("Courses").child(i).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
                    Course temp = snapshot.getValue(Course.class);
                    temp.dependentAdd(coursecode);
                    FirebaseDatabase.getInstance().getReference("Courses").child(i).setValue(temp);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    return;
                }
            });
        }
    }

    private boolean checkCourseType(String coursetype){
        if(coursetype.isEmpty()){
            binding.coursecode.setError("Course code cannot be empty!");
            binding.coursecode.requestFocus();
            return false;
        }
        if(coursetype.length() > 3){
            binding.coursecode.setError("Course code can only be 3 characters!");
            binding.coursecode.requestFocus();
            return false;
        }
        return true;
    }
    private boolean checkName(String name){
        if(name.isEmpty()){
            binding.coursename.setError("Course name cannot be empty!");
            binding.coursename.requestFocus();
            return false;
        }
        return true;
    }

    private boolean checkCourseNum(String coursenum){
        if(coursenum.isEmpty()){
            binding.coursenum.setError("Course number cannot be empty!");
            binding.coursenum.requestFocus();
            return false;
        }
        if(coursenum.length() > 2){
            binding.coursenum.setError("Course number should have only two characters!");
            binding.coursenum.requestFocus();
            return false;
        }
        return true;
    }
    private boolean checkLevel(String level){
        if(level.isEmpty()){
            binding.courselevel.setError("Level cannot be empty!");
            binding.courselevel.requestFocus();
            return false;
        }
        if(level.length() > 1){
            binding.courselevel.setError("Level should be single character!");
            binding.courselevel.requestFocus();
            return false;
        }
        return true;
    }
    private boolean checkSessions(List<String> sessions){
        if(sessions.isEmpty()) {
            binding.sessionselector.setError("Please choose atleast one session!");
            binding.sessionselector.requestFocus();
            return false;
        }
        return true;
    }
    private List<String> fetchSession(){
        List<String> result = new ArrayList<String>();
        for(int j = 0; j < sessionList.size(); j++){
            result.add(sessionArray[sessionList.get(j)]);
        }
        return result;
    }
    private List<String> fetchPrereq(){
        List<String> result = new ArrayList<String>();
        for(KeyPairBoolData i:courseList){
            if(i.isSelected()){
                result.add(i.getName());
            }
        }
        return result;
    }

    //TODO Make Prerequisite button for getting prerequisite of the course
}