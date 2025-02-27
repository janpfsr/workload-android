package com.gmail.konstantin.schubert.workload;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.IOError;
import java.util.ArrayList;
import java.util.List;


public class DBObjectBuilder {


    private static final String TAG = DBObjectBuilder.class.getSimpleName();
    private ContentResolver mContentResolver;

    public DBObjectBuilder(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    public Cursor getLectureCursor(boolean onlyActive) {
        String where = null;
        if (onlyActive) {
            where = SurveyContentProvider.DB_STRINGS_LECTURE.ISACTIVE + "=1";
        }

        Cursor cursor = mContentResolver.query(
                Uri.parse("content://" + SurveyContentProvider.AUTHORITY + "/lectures/null/any/"),
                null,
                where,
                null,
                SurveyContentProvider.DB_STRINGS_LECTURE.NAME
        );
        return cursor;

    }

    public List<Lecture> getLectureList(boolean onlyActive) {

        Cursor cursor = getLectureCursor(onlyActive);
        List<Lecture> lectures = new ArrayList<Lecture>();
        while (cursor.moveToNext()) {
            lectures.add(buildLectureFromCursor(cursor));
        }
        cursor.close();
        return lectures;
    }


    public Cursor getNotIdle(String table) {
        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/";
        uri += table + "/";
        uri += "null/any/";
        String where = SurveyContentProvider.DB_STRINGS.STATUS + "<>" + SurveyContentProvider.SYNC_STATUS.IDLE;
        return mContentResolver.query(Uri.parse(uri), null, where, null, null);
    }

    public void mark_as_transacting(int id, String table) {
        Log.d(TAG, "mark_as_transacting: " + id + " in table " + table);

        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/";
        uri += table + "/";
        uri += SurveyContentProvider.SYNC_STEER_COMMAND.MARK_TRANSACTING + "/";
        uri += String.valueOf(id) + "/";

        int rows_affected = mContentResolver.update(Uri.parse(uri), new ContentValues(), null, null);
        if (rows_affected != 1) throw new IOError(new Throwable());
    }


    public WorkloadEntry getWorkloadEntryByLocalId(int local_id) {

        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/workentries/null/";
        uri += String.valueOf(local_id) + "/";
        Cursor cursor = mContentResolver.query(Uri.parse(uri), null, null, null, null);
        cursor.moveToFirst();
        WorkloadEntry entry = buildWorkloadEntryFromCursor(cursor);
        cursor.close();
        return entry;
    }


    public void addLecture(Lecture lecture, String syncSteerCommand) {
        String uriString = "content://" + SurveyContentProvider.AUTHORITY;
        uriString += "/lectures/" + syncSteerCommand + "/";
        uriString += "/any";
        ContentValues values = getValues(lecture);
        mContentResolver.insert(Uri.parse(uriString), values);
    }

    public WorkloadEntry buildWorkloadEntryFromCursor(Cursor cursor) {
        Week week = new Week(
                cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_WORKENTRY.YEAR)),
                cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_WORKENTRY.WEEK))
        );
        int lecture_id = cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_WORKENTRY.LECTURE_ID));
        float hoursInLecture = cursor.getFloat(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_IN_LECTURE));
        float hoursForHomework = cursor.getFloat(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_FOR_HOMEWORK));
        float hoursStudying = cursor.getFloat(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_STUDYING));
        return new WorkloadEntry(week, lecture_id, hoursInLecture, hoursForHomework, hoursStudying);

    }

    public Lecture buildLectureFromCursor(Cursor cursor) {

        int _ID = cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS._ID));
        String name = cursor.getString(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.NAME));
        String semester = cursor.getString(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.SEMESTER));
        Week startWeek = new Week(
                cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.STARTYEAR)),
                cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.STARTWEEK))
        );
        Week endWeek = new Week(
                cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.ENDYEAR)),
                cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.ENDWEEK))
        );
        boolean isActive = (cursor.getInt(cursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.ISACTIVE)) == 1);
        return new Lecture(_ID, name, semester, startWeek, endWeek, isActive);
    }


    public List<Lecture> getLecturesInWeek(Week thatWeek, boolean onlyActive) {
        // gets lectures that are active in 'thatWeek'
        List<Lecture> allLectures = getLectureList(onlyActive);
        List<Lecture> lecturesThatWeek = new ArrayList<Lecture>();
        for (Lecture lecture : allLectures) {
            if (lecture.startWeek.compareTo(thatWeek) <= 0 && thatWeek.compareTo(lecture.endWeek) <= 0) {
                lecturesThatWeek.add(lecture);
            }
        }
        return lecturesThatWeek;
    }

    /**
     * Returns true if a workload entry exists for all considered lectures in the considered week.
     * @param lectures The list of lectures to be considered.
     * @param week The week to be considered.
     * @return
     */
    public boolean allLecturesHaveDataInWeek(List<Lecture> lectures, Week week) {
        for (Lecture lecture : lectures) {
            if (!lecture.hasDataInWeek(mContentResolver, week)) {
                return false;
            }
        }
        return true;
    }

    public List<Lecture> getLecturesOfSemester(String semester, boolean onlyActive) {

        Cursor lectureCursor = getLectureCursor(onlyActive);
        List<Lecture> lecturesInSemester = new ArrayList<>();
        int columnIndex = lectureCursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.SEMESTER);
        while (lectureCursor.moveToNext()) {
            String lectureSemester = lectureCursor.getString(columnIndex);
            if (lectureSemester.equals(semester)) {
                lecturesInSemester.add(buildLectureFromCursor(lectureCursor));
            }
        }
        lectureCursor.close();
        return lecturesInSemester;
    }


    public List<Semester> getSemesterList(boolean onlyActive) {
        // Returns unsorted list of semesters

        List<Semester> semesters = new ArrayList<>();
        Cursor lectureCursor = getLectureCursor(onlyActive);
        int semesterColumnIndex = lectureCursor.getColumnIndex(SurveyContentProvider.DB_STRINGS_LECTURE.SEMESTER);
        //Wow, this would be two lines in python. Am I doing this wrong?
        while (lectureCursor.moveToNext()) {
            boolean found = false;
            for (Semester semester : semesters) {
                if (semester.to_string().equals(lectureCursor.getString(semesterColumnIndex))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                semesters.add(new Semester(lectureCursor.getString(semesterColumnIndex)));
            }
        }
        lectureCursor.close();
        return semesters;
    }

    public Cursor getWorkloadEntry(int lecture_id, Week week) {
        String where = SurveyContentProvider.DB_STRINGS_WORKENTRY.LECTURE_ID + "=" + String.valueOf(lecture_id) + " AND ";
        where += SurveyContentProvider.DB_STRINGS_WORKENTRY.YEAR + "=" + String.valueOf(week.year()) + " AND ";
        where += SurveyContentProvider.DB_STRINGS_WORKENTRY.WEEK + "=" + String.valueOf(week.week());
        Cursor cursor = mContentResolver.query(Uri.parse("content://" + SurveyContentProvider.AUTHORITY + "/workentries/null/any/"), null, where, null, null);
        return cursor;
    }

    public void addWorkloadEntry(WorkloadEntry entry, String syncSteerCommand) {
        ContentValues values = new ContentValues(3);
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.YEAR, entry.week.year());
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.WEEK, entry.week.week());
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.LECTURE_ID, entry.lecture_id);
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_FOR_HOMEWORK, entry.getHoursForHomework());
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_IN_LECTURE, entry.getHoursInLecture());
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_STUDYING, entry.getHoursStudying());
        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/workentries/";
        uri += syncSteerCommand + "/";
        uri += "any/";
        mContentResolver.insert(Uri.parse(uri), values);

    }


    public List<WorkloadEntry> getWorkloadEntries(Lecture lecture) {
        /* Gets the workload entries for a given lecture.
        * If passed null it will get the entries for all lectures.
        * */
        String where = null;
        if (lecture != null) {
            where = SurveyContentProvider.DB_STRINGS_WORKENTRY.LECTURE_ID + "=" + String.valueOf(lecture._ID);
        }

        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/workentries/null/any/";
        Cursor cursor = mContentResolver.query(Uri.parse(uri), null, where, null, null);

        List<WorkloadEntry> workloadEntries = new ArrayList<WorkloadEntry>();
        while (cursor.moveToNext()) {

            WorkloadEntry newWorkloadEntry = buildWorkloadEntryFromCursor(cursor);
            workloadEntries.add(newWorkloadEntry);
        }
        cursor.close();
        return workloadEntries;
    }

    public void updateWorkloadEntry(WorkloadEntry entry, String syncSteerCommand) {
        Log.d(TAG, "updateWorkloadEntry: " + entry.toString() + syncSteerCommand);

        ContentValues values = new ContentValues();
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_FOR_HOMEWORK, entry.getHoursForHomework());
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_IN_LECTURE, entry.getHoursInLecture());
        values.put(SurveyContentProvider.DB_STRINGS_WORKENTRY.HOURS_STUDYING, entry.getHoursStudying());
        String where = SurveyContentProvider.DB_STRINGS_WORKENTRY.LECTURE_ID + "=" + String.valueOf(entry.lecture_id) + " AND ";
        where += SurveyContentProvider.DB_STRINGS_WORKENTRY.YEAR + "=" + String.valueOf(entry.week.year()) + " AND ";
        where += SurveyContentProvider.DB_STRINGS_WORKENTRY.WEEK + "=" + String.valueOf(entry.week.week());

        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/workentries/";
        uri += syncSteerCommand + "/";
        uri += "any/";
        int result = mContentResolver.update(Uri.parse(uri), values, where, null);
        if (result < 0) {
            Log.d(TAG, "Could not update " + uri + ". Maybe SYNC_STATUS of row is not IDLE");
        }
    }

    public void updateLecture(Lecture lecture, String syncSteerCommand) {
        Log.d(TAG, "updateLecture: " + lecture.toString() + syncSteerCommand);

        ContentValues values = getValues(lecture);
        values.remove(SurveyContentProvider.DB_STRINGS._ID); // We add the ID in the url.

        String uri = "content://" + SurveyContentProvider.AUTHORITY + "/lectures/";
        uri += syncSteerCommand + "/";
        uri += String.valueOf(lecture._ID) + "/";
        int result = mContentResolver.update(Uri.parse(uri), values, null, null);
        if (result < 0) {
            Log.d(TAG, "Could not update " + uri + ". Maybe SYNC_STATUS of row is not IDLE");
        }
    }

    private ContentValues getValues(Lecture lecture) {
        ContentValues values = new ContentValues();
        values.put(SurveyContentProvider.DB_STRINGS._ID, lecture._ID);
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.NAME, lecture.name);
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.SEMESTER, lecture.semester);
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.ENDWEEK, lecture.endWeek.week());
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.ENDYEAR, lecture.endWeek.year());
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.STARTWEEK, lecture.startWeek.week());
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.STARTYEAR, lecture.startWeek.year());
        values.put(SurveyContentProvider.DB_STRINGS_LECTURE.ISACTIVE, lecture.isActive ? 1 : 0);
        return values;
    }


}
