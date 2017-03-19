package com.psychapps.attwatch.helpers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.app.NotificationCompat;

import com.psychapps.attwatch.activities.BunkPlannerDialogue;
import com.psychapps.attwatch.entity.BunkPlanner;
import com.psychapps.attwatch.entity.Event;
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
 * Created by Pavan on 6/21/2016.
 */
public class BunkPlanNotifier extends BroadcastReceiver {
    public static final String PREF_FILE = "bmPref";
    public static final String PREF_SET = "set";
    public static final String PREF_HOUR = "hour";
    public static final String PREF_MIN = "min";
    public static final String PREF_PREDICTION = "prediction";
    private DBHelper dbHelper;
    private SimpleDateFormat sdfParser;
    private TaskListener taskListener;
    @Override
    public void onReceive(final Context context, final Intent intent) {
        dbHelper = new DBHelper(context);
        sdfParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        taskListener = new TaskListener() {
            @Override
            public void onTaskBegin() {
                try {
                    dbHelper.open();
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.FeedEntry.COLUMN_NAME_STATUS, 0);
                    dbHelper.getSQLInstance().update(DBHelper.FeedEntry.TABLE_NAME_TIMETABLE, cv, null, null);
                    dbHelper.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTaskCompleted() {
                String subjectList = populateSubjectsToAttend();
                SharedPreferences sp = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor spe = sp.edit();
                spe.putString(PREF_PREDICTION, subjectList);
                spe.apply();
                if(!subjectList.equals("") && intent.getBooleanExtra("notify", true)
                && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    int requestID = (int) System.currentTimeMillis();
                    String content = "Summary of attendance for today.";
                    android.support.v4.app.NotificationCompat.Action action = null;
                    NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                            .bigText(subjectList)
                            .setSummaryText("Touch to view");
                    /*if(subjectList.equals("Your planned bunk is possible today!")){
                        //Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                        //Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        // To count with Play market backstack, After pressing back button,
                        // to taken back to our application, we need to add following flags to intent.
                        //goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        //        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        //        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        //PendingIntent ratePendingIntent = PendingIntent.getActivity(context, requestID + 1, goToMarket, PendingIntent.FLAG_UPDATE_CURRENT);
                        //action = new android.support.v4.app.NotificationCompat.Action(android.R.drawable.star_on, "Rate me",ratePendingIntent);
                    }*/
                    Intent dialogIntent = new Intent(context, BunkPlannerDialogue.class);
                    dialogIntent.putExtra("summary", subjectList);
                    dialogIntent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, requestID, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    notify.sendNotification(pendingIntent, content, context, 9, style, action);
                }
            }
        };
        new AsyncBunkPlansEvaluator(taskListener, context, "now", ">=").execute();
    }

    private String populateSubjectsToAttend() {
        String today = new SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String date = sdfParser.format(Calendar.getInstance().getTime());
        String baseDate = "now";
        String compare = ">";
        String nameList = "";
        ArrayList<BunkPlanner> bunkPlans = null;
        ArrayList<TimeTable> lectures, lecturesToday;
        ArrayList<Integer> subjectsToday = new ArrayList<Integer>();
        Set<Integer> subjectIDs = new HashSet<Integer>();
        BunkPlanner isABunkDay = null;
        Event isEvent = null;
        try {
            dbHelper.open();
            isABunkDay = dbHelper.checkIfBunkPlanToday(date);
            isEvent = isAnEvent(date);
            if(isABunkDay == null && isEvent == null) {
                bunkPlans = dbHelper.getLiveBunkPlans(baseDate, compare);
                lecturesToday = dbHelper.getLectures(today);
                for (TimeTable lecture : lecturesToday) {
                    subjectsToday.add(lecture.getSubject().getId());
                }
                //System.out.println("date\tattended\tmissed\tbunks\tlecturecount\tplannedbunks");
                for (com.psychapps.attwatch.entity.BunkPlanner bp :
                        bunkPlans) {
                    String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(sdfParser.parse(bp.getDate()));
                    lectures = dbHelper.getDistinctLectures(day);
                    for (TimeTable lecture :
                            lectures) {
                        float attended = Float.parseFloat(dbHelper.getSubjectAttendance(lecture.getSubject().getId(), 1));
                        float missed = Float.parseFloat(dbHelper.getSubjectAttendance(lecture.getSubject().getId(), 0));
                        float lectureCount = getLectureCountForSubject(lecture.getSubject().getId(), bp.getDate());
                        float prevPlanCount = getPlanCountForSubject(lecture.getSubject().getId(), bp.getDate());
                        float curricularEventCount = getEventCount(bp.getDate(), 0, lecture.getSubject().getId()); // 0 is for curricular events
                        float extraCurricularEventCount = getEventCount(bp.getDate(), 1, lecture.getSubject().getId()); // 1 is for extra curricular events
                        for (int id : subjectsToday) {
                            if (id == lecture.getSubject().getId()) {
                                missed++;
                            }
                        }
                        float bunks = (100f / (float) lecture.getSubject().getLimit()) * attended - attended - missed;
                        //System.out.println(bp.getDate()+"\t" + attended + "\t" + missed +"\t"+bunks+"\t"+lectureCount+"\t"+prevPlanCount);
                        ////System.out.println(bp.getDate()+"\t" + attended + "\t" + missed +"\t"+bunks+"\t"+lectureCount+"\t"+prevPlanCount);
                    /*if (bunks - lectureCount < 0 && (attended + missed) != 0) {
                        *//*attended += (lectureCount - prevPlanCount);
                        missed += prevPlanCount;
                        //bunks = (100f/(float)lecture.getSubject().getLimit())*attended - attended - missed;
                        if(attended/(attended+missed)*100f >= lecture.getSubject().getLimit()) {

                        } else {

                        }*//*
                        subjectIDs.add(lecture.getSubject().getId());
                    }*/
                        if (bunks - (lectureCount - curricularEventCount ) < 0 && (attended + missed) != 0) {
                            if(bp.getStatus() == 1) {
                                subjectIDs.add(lecture.getSubject().getId());
                                //System.out.println("green to orange");
                            }
                            attended += (lectureCount - prevPlanCount - curricularEventCount - extraCurricularEventCount);
                            missed += (prevPlanCount + extraCurricularEventCount);
                            //bunks = (100f/(float)lecture.getSubject().getLimit())*attended - attended - missed;
                            if (attended / (attended + missed) * 100f < lecture.getSubject().getLimit()) {
                                if (bp.getStatus() == 0) {
                                    //System.out.print("orange to red");
                                    subjectIDs.add(lecture.getSubject().getId());
                                }
                            }
                        }
                    }
                }
                String idString = "(";
                int i = 0;
                for (int id : subjectIDs) {
                    idString += " " + id;
                    i++;
                    if (i != subjectIDs.size()) {
                        idString += ",";
                    }
                }
                idString += " )";

                String subQuery = "SELECT " + DBHelper.FeedEntry.COLUMN_NAME_SUBJECT + " FROM " + DBHelper.FeedEntry.TABLE_NAME_TIMETABLE +
                        " WHERE " + DBHelper.FeedEntry.COLUMN_NAME_DAY + "='" + today + "' AND " +
                        DBHelper.FeedEntry.COLUMN_NAME_SUBJECT + " IN " + idString;

                String sql = "SELECT " + DBHelper.FeedEntry.COLUMN_NAME_SNAME + " FROM " + DBHelper.FeedEntry.TABLE_NAME_SUBJECTS +
                        " WHERE " + DBHelper.FeedEntry._ID + " IN ( " + subQuery + " )";
                Cursor mCursor = dbHelper.getSQLInstance().rawQuery(sql, null);
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.FeedEntry.COLUMN_NAME_STATUS, 1);
                dbHelper.getSQLInstance().update(DBHelper.FeedEntry.TABLE_NAME_TIMETABLE, cv, DBHelper.FeedEntry.COLUMN_NAME_DAY + "='" + today + "' AND " +
                        DBHelper.FeedEntry.COLUMN_NAME_SUBJECT + " IN " + idString, null);
                if (mCursor != null && mCursor.getCount() != 0) {
                    mCursor.moveToFirst();
                    do {
                        nameList += mCursor.getString(0) + "\n";
                    } while (mCursor.moveToNext());
                    mCursor.close();
                }
            }
            dbHelper.close();
        } catch (ParseException | SQLException e) {
            e.printStackTrace();
        }
        if(bunkPlans != null && bunkPlans.size() == 0) {
            return "";
        }else if(!nameList.equals("")) {
            return "You have to necessarily attend these lectures today.\n" + nameList;
        } else if(isABunkDay != null && isABunkDay.getStatus() != -1) {
            return "You can miss lectures today as planned!";
        } else if(isEvent != null) {
            return "You have "+isEvent.getName()+" today";
        }
        return "You may miss any lecture today.";
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

    private Event isAnEvent(String bpDate) {
        String sql = "SELECT * FROM " + DBHelper.FeedEntry.TABLE_NAME_EVENTS
                + " WHERE date(" + DBHelper.FeedEntry.COLUMN_NAME_START + ") <= '" + bpDate + "' "
                + "AND date(" + DBHelper.FeedEntry.COLUMN_NAME_END + ") >= '" + bpDate + "'";
        Cursor mCursor = dbHelper.getSQLInstance().rawQuery(sql, null);

        if(mCursor != null && mCursor.getCount() != 0) {
            mCursor.moveToFirst();
            Event event = new Event();
            event.setId(mCursor.getInt(0));
            event.setName(mCursor.getString(1));
            event.setType(mCursor.getInt(2));
            event.setStart(mCursor.getString(3));
            event.setEnd(mCursor.getString(4));
            return event;
        }
        return null;
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
        for(int i = 0; i < w1; i++) {
            dayCount[i]--;
        }
        for(int i = 0; i < w2; i++) {
            dayCount[i]++;
        }
        for(int i = 0; i < 7; i++) {
            sum += (lectureCount[i] * dayCount[i]);
        }
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
