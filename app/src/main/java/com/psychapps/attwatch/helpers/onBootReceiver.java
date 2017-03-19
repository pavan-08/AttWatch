package com.psychapps.attwatch.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;


/**
 * Created by Pavan on 18/06/2015.
 */
public class onBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager am= (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent1=new Intent(context,notify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        String status=ReadLine("notify", context);
        String hour=ReadLine("hour",context);
        String minute=ReadLine("minute",context);
        if (status.equals("on") && !hour.equals("") && !minute.equals("")) {
            int mHour = Integer.parseInt(hour);
            int mMinute = Integer.parseInt(minute);
            long alarm = getAlarmTime(mHour, mMinute);
            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, 24 * 60 * 60 * 1000, pendingIntent);

        }
        setBunkPlanNotifierAlarm(context);
    }

    public static void setBunkPlanNotifierAlarm(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent bunkNotifier = new Intent(context, BunkPlanNotifier.class);
        bunkNotifier.putExtra("notify", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, bunkNotifier, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
        SharedPreferences sp = context.getSharedPreferences(BunkPlanNotifier.PREF_FILE, Context.MODE_PRIVATE);
        int hour = sp.getInt(BunkPlanNotifier.PREF_HOUR, 5);
        int min = sp.getInt(BunkPlanNotifier.PREF_MIN, 0);
        long alarm = getAlarmTime(hour, min);
        am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, 24 * 60 * 60 * 1000, pendingIntent);
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean(BunkPlanNotifier.PREF_SET, true);
        spe.apply();
        //Toast.makeText(context, "Alarm set", Toast.LENGTH_SHORT).show();
    }

    public static long getAlarmTime(int hour, int min) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, 0);
        long alarm = 0;
        if(calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            alarm = calendar.getTimeInMillis() + (AlarmManager.INTERVAL_DAY);
        } else {
            alarm = calendar.getTimeInMillis();
        }
        return alarm;
    }

    public String ReadLine(String filename,Context context)
    {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    context.openFileInput(filename)));
            String inputString;

            while ((inputString = inputReader.readLine()) != null)
            {
                stringBuffer.append(inputString );
            }
        }catch (IOException e) {
            e.printStackTrace();}
        return stringBuffer.toString();
    }
}
