package com.psychapps.attwatch.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.psychapps.attwatch.entity.BunkPlanner;
import com.psychapps.attwatch.entity.TimeTable;
import com.psychapps.attwatch.interfaces.TaskListener;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Pavan on 6/20/2016.
 */
public class AsyncBunkPlansEvaluator extends AsyncTask<Void, Void, Void> {

    private String baseDate;
    private String compare;
    private TaskListener listener;
    private Context context;
    private DBHelper dbHelper;
    private SimpleDateFormat sdfParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public AsyncBunkPlansEvaluator(TaskListener listener, Context context) {
        this.listener = listener;
        this.context = context;
        dbHelper = new DBHelper(context);
        this.baseDate = "now";
        this.compare = ">";
    }

    public AsyncBunkPlansEvaluator(TaskListener listener, Context context, String baseDate, String compare) {
        this(listener, context);
        this.baseDate = baseDate;
        this.compare = compare;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.onTaskBegin();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        updateStatusOfPlans();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        listener.onTaskCompleted();
    }

    private void updateStatusOfPlans() {
        ArrayList<BunkPlanner> bunkPlans;
        ArrayList<TimeTable> lectures;
        try {
            dbHelper.open();
            bunkPlans = dbHelper.getLiveBunkPlans(baseDate, compare);
            //System.out.println("date\tattended\tmissed\tbunks\tlecturecount\tplannedbunks");
            for (com.psychapps.attwatch.entity.BunkPlanner bp :
                    bunkPlans) {
                String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(sdfParser.parse(bp.getDate()));
                lectures = dbHelper.getDistinctLectures(day);
                int status = 1;
                //System.out.println("Day = "+ day);
                if(!isAnEvent(bp.getDate())) {
                    for (TimeTable lecture :
                            lectures) {
                        float attended = Float.parseFloat(dbHelper.getSubjectAttendance(lecture.getSubject().getId(), 1));
                        float missed = Float.parseFloat(dbHelper.getSubjectAttendance(lecture.getSubject().getId(), 0));
                        float bunks = (100f / (float) lecture.getSubject().getLimit()) * attended - attended - missed;
                        //float compensate = (100f/(float)(100 - lecture.getSubject().getLimit())) * missed - missed - attended;
                        float lectureCount = getLectureCountForSubject(lecture.getSubject().getId(), bp.getDate());
                        float prevPlanCount = getPlanCountForSubject(lecture.getSubject().getId(), bp.getDate());
                        float curricularEventCount = getEventCount(bp.getDate(), 0, lecture.getSubject().getId()); // 0 is for curricular events
                        float extraCurricularEventCount = getEventCount(bp.getDate(), 1, lecture.getSubject().getId()); // 1 is for extra curricular events
                        //System.out.println(bp.getDate()+"\t" + attended + "\t" + missed +"\t"+bunks+"\t"+lectureCount+"\t"+prevPlanCount);
                        if ((bunks - (lectureCount - curricularEventCount) >= 0 || (attended + missed) == 0) && status == 1) {
                            status = 1;
                        } else {

//                            System.out.println("lectureCount = " + lectureCount);
//                            System.out.println("curricular = " + curricularEventCount);
//                            System.out.println("extra-curricular = " + extraCurricularEventCount);
//                            System.out.println("prev plan = "+ prevPlanCount);
                            attended += (lectureCount - curricularEventCount - prevPlanCount - extraCurricularEventCount);
                            missed += (prevPlanCount + extraCurricularEventCount);
//                            System.out.println("Attended = " + attended);
//                            System.out.println("Missed = " + missed);
                            //bunks = (100f/(float)lecture.getSubject().getLimit())*attended - attended - missed;
                            if (attended / (attended + missed) * 100f >= lecture.getSubject().getLimit()) {
                                status = 0;
                            } else {
                                status = -1;
                                break;
                            }
                        }
                    }
                    if (lectures.size() == 0) {
                        status = 1;
                    }
                } else {
                    status = -1;
                }
                //System.out.println(bp.getDate() + " " + status);
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.FeedEntry.COLUMN_NAME_STATUS, status);
                dbHelper.getSQLInstance().update(DBHelper.FeedEntry.TABLE_NAME_BUNK_PLANNER, cv , DBHelper.FeedEntry._ID + "=" + bp.get_id(), null);
            }
            dbHelper.close();
        } catch (ParseException | SQLException e) {
            e.printStackTrace();
        }

    }

    private float getEventCount(String bpDate, int typeOfEvent, int subjectID) {
        int ret = 0;
        //long diff;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String sql = "SELECT * FROM " + DBHelper.FeedEntry.TABLE_NAME_EVENTS
                   + " WHERE date(" + DBHelper.FeedEntry.COLUMN_NAME_START + ") < '" + bpDate +"' "
                   + "AND " + DBHelper.FeedEntry.COLUMN_NAME_TYPE + " = " + typeOfEvent;
        Cursor mCursor = dbHelper.getSQLInstance().rawQuery(sql, null);
        if(mCursor != null && mCursor.getCount() != 0) {
            mCursor.moveToFirst();
            try {
                do {
                    Set<String> days = new HashSet<String>();
                    Date sd;
                    Calendar c = Calendar.getInstance();
                    for(sd = sdf.parse(mCursor.getString(3)); sd.compareTo(sdf.parse(mCursor.getString(4))) <= 0; ) {
                        days.add(dayFormat.format(sd));
                        //increment date logic
                        c.setTime(sd);
                        c.add(Calendar.DATE, 1);  // number of days to add
                        sd = c.getTime();  // sd is now the new date
                    }
                    String daysg = "(";
                    int j = 0;
                    for(String day: days) {
                        daysg += "'" +day + "'";
                        j++;
                        if(j != days.size())
                            daysg += ", ";
                    }
                    daysg += ")";
                    String sql1 = "SELECT COUNT(*) as count FROM " + DBHelper.FeedEntry.TABLE_NAME_TIMETABLE
                                + " WHERE "+ DBHelper.FeedEntry.COLUMN_NAME_SUBJECT + " = " + subjectID
                                + " AND " + DBHelper.FeedEntry.COLUMN_NAME_DAY + " IN " + daysg;
                    Cursor cursor = dbHelper.getSQLInstance().rawQuery(sql1, null);
                    if(cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        ret += cursor.getInt(0);
                        cursor.close();
                    }
                    /*diff = sdf.parse(mCursor.getString(4)).getTime() - sdf.parse(mCursor.getString(3)).getTime();
                    ret += (int) (diff / (1000 * 60 * 60 * 24));*/
                } while(mCursor.moveToNext());
                mCursor.close();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private boolean isAnEvent(String bpDate) {
        String sql = "SELECT * FROM " + DBHelper.FeedEntry.TABLE_NAME_EVENTS
                   + " WHERE date(" + DBHelper.FeedEntry.COLUMN_NAME_START + ") <= '" + bpDate + "' "
                   + "AND date(" + DBHelper.FeedEntry.COLUMN_NAME_END + ") >= '" + bpDate + "'";
        Cursor mCursor = dbHelper.getSQLInstance().rawQuery(sql, null);

        return mCursor != null && mCursor.getCount() != 0;
    }

    private int getLectureCountForSubject(int subjectID, String date) {
        int[] lectureCount = new int[7];
        int[] dayCount = new int[7];
        int sum = 0;
        Arrays.fill(lectureCount,0);
        Arrays.fill(dayCount, 0);
        Cursor mCursor = dbHelper.getSQLInstance().query(DBHelper.FeedEntry.TABLE_NAME_TIMETABLE,
                new String[]{DBHelper.FeedEntry.COLUMN_NAME_DAY, "COUNT("+ DBHelper.FeedEntry.COLUMN_NAME_DAY+")"},
                DBHelper.FeedEntry.COLUMN_NAME_SUBJECT + "=" + subjectID, null,
                DBHelper.FeedEntry.COLUMN_NAME_DAY, null, DBHelper.FeedEntry.COLUMN_NAME_DAY + " ASC");
        if(mCursor != null && mCursor.getCount() != 0) {
            mCursor.moveToFirst();
            do{
                switch(mCursor.getString(0)) {
                    case "Monday":
                        lectureCount[1] = mCursor.getInt(1);
                        break;
                    case "Tuesday":
                        lectureCount[2] = mCursor.getInt(1);
                        break;
                    case "Wednesday":
                        lectureCount[3] = mCursor.getInt(1);
                        break;
                    case "Thursday":
                        lectureCount[4] = mCursor.getInt(1);
                        break;
                    case "Friday":
                        lectureCount[5] = mCursor.getInt(1);
                        break;
                    case "Saturday":
                        lectureCount[6] = mCursor.getInt(1);
                        break;
                }
            } while (mCursor.moveToNext());
            mCursor.close();
        }

        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        try {
            c2.setTime(sdfParser.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int w1 = c1.get(Calendar.DAY_OF_WEEK);
        int w2 = c2.get(Calendar.DAY_OF_WEEK);
        c1.add(Calendar.DAY_OF_WEEK, -(w1 - 1));
        c2.add(Calendar.DAY_OF_WEEK, -(w2 - 1));
        Arrays.fill(dayCount, (int)((float)(c2.getTimeInMillis() - c1.getTimeInMillis())/(1000*60*60*24*7) + 0.5f));
        //System.out.println((int)((float)(c2.getTimeInMillis() - c1.getTimeInMillis())/(1000*60*60*24*7) + 0.5f));
        if(compare.equals(">")) {
            for(int i = 0; i < w1; i++) {
                dayCount[i]--;
            }
        } else {
            for(int i = 0; i < w1-1; i++) {
                dayCount[i]--;
            }
        }
        for(int i = 0; i < w2; i++) {
            dayCount[i]++;
        }
        for(int i = 0; i < 7; i++) {
            sum += (lectureCount[i] * dayCount[i]);
        }
        //System.out.println(sum);
        return sum;
    }

    private float getPlanCountForSubject(int subjectID, String date) {
        String sql = "SELECT COUNT(*) AS count from bunk_planner as b inner join time_table as t " +
                "on case cast(strftime('%w', b.date) as integer) " +
                "when 0 then 'Sunday' " +
                "when 1 then 'Monday' " +
                "when 2 then 'Tuesday' " +
                "when 3 then 'Wednesday' " +
                "when 4 then 'Thursday' " +
                "when 5 then 'Friday' " +
                "when 6 then 'Saturday' " +
                "end = t.day " +
                "where ((date(b.date) > date('now') and date(b.date) <= '" + date +"' " +
                "and b.status > -1) " +
                "or date(b.date) = '" + date + "') " +
                "and t.subject = " + subjectID;
        Cursor mCursor = dbHelper.getSQLInstance().rawQuery(sql, null);
        if(mCursor != null && mCursor.getCount() !=0) {
            mCursor.moveToFirst();
            int ret = mCursor.getInt(0);
            mCursor.close();
            return ret;
        }
        return 0;
    }

}
