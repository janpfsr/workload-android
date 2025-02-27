package com.gmail.konstantin.schubert.workload.activities;


import android.content.Intent;
import android.os.Bundle;

import com.gmail.konstantin.schubert.workload.Adapters.SelectLectureAdapter;
import com.gmail.konstantin.schubert.workload.R;
import com.gmail.konstantin.schubert.workload.Week;


public class SelectLecture extends MyBaseListActivity {
    public final static String MESSAGE_YEAR = "com.gmail.konstantin.schubert.workload.YEAR";
    public final static String MESSAGE_WEEK = "com.gmail.konstantin.schubert.workload.WEEK";
    private static final String TAG = SelectLecture.class.getSimpleName();
    private Week mWeek;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_lecture);

        Intent launchIntent = getIntent();
        Integer year = launchIntent.getIntExtra(MESSAGE_YEAR, -1);
        Integer weekNumber = launchIntent.getIntExtra(MESSAGE_WEEK, -1);
        this.mWeek = new Week(year, weekNumber);
        setListAdapter(new SelectLectureAdapter(this, mWeek));
        setTitle("Choose a lecture");
    }


    @Override
    public void onResume() {
        super.onResume();
        SelectLectureAdapter selectLectureAdapter = (SelectLectureAdapter) getListAdapter();
        //TODO: Make this more efficient?
        selectLectureAdapter.updateMembers(null);
    }


}
