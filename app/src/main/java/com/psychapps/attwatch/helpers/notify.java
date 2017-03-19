package com.psychapps.attwatch.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.psychapps.attwatch.R;
import com.psychapps.attwatch.activities.MainActivity;
import com.psychapps.attwatch.activities.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Pavan on 10/06/2015.
 */
public class notify extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        int requestID=(int) System.currentTimeMillis();
        Intent main = new Intent(context,MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent =PendingIntent.getActivity(context,requestID,main,PendingIntent.FLAG_UPDATE_CURRENT);
        Date date =new Date();
        SimpleDateFormat dateFormat =new SimpleDateFormat("E");
        String content = "";
        if(dateFormat.format(date).equals("Sun")){
            content = "Its Sunday! Time to review attendance";
        } else {
            content = "Tell me how your day was!";
        }
        sendNotification(pendingIntent, content, context, 8, null, null);
    }

    public static void sendNotification(PendingIntent pendingIntent, String content, Context context, int uniqueID, @Nullable android.support.v4.app.NotificationCompat.Style style, @Nullable android.support.v4.app.NotificationCompat.Action action) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences sp = context.getSharedPreferences(BunkPlanNotifier.PREF_FILE, Context.MODE_PRIVATE);
        Uri alarmsound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder =new NotificationCompat.Builder(context);
        builder.setAutoCancel(true);
        builder.setContentTitle(context.getResources().getString(R.string.app_name));
        if(sp.getBoolean(Settings.PREF_VIBRATE, true)) {
            builder.setVibrate(new long[]{100, 400, 100, 400});
        }
        if(sp.getBoolean(Settings.PREF_SOUND, true) && audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            builder.setSound(alarmsound);
        }
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setColor(ContextCompat.getColor(context, R.color.material_indigo));
        builder.setContentIntent(pendingIntent);
        builder.setContentText(content);
        if(action != null) {
            builder.addAction(action);
        }
        builder.setSmallIcon(R.mipmap.ic_notification);
        if(style != null) {
            builder.setStyle(style);
        }
        Notification notification = builder.build();
        NotificationManager manager =(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(uniqueID, notification);
        //Toast.makeText(context, "notified", Toast.LENGTH_SHORT).show();
    }

}
