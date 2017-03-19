package com.psychapps.attwatch.activities;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ViewSwitcher;

import com.psychapps.attwatch.R;
import com.psychapps.attwatch.helpers.BunkPlanNotifier;
import com.psychapps.attwatch.helpers.notify;
import com.psychapps.attwatch.helpers.onBootReceiver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;


public class Settings extends AppCompatActivity {

    private SwitchCompat mswitch, soundSwitch, vibrateSwitch;
    private TextView reminderTime, bunkPlanTime;
    private RelativeLayout soundLayout, vibrateLayout, bunkPlanLayout;
    private TextSwitcher textSwitcher;
    private AppCompatImageButton bunkPlanExpand;
    public static final String PREF_SOUND = "sound";
    public static final String PREF_VIBRATE = "vibrate";
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    private static final String bunkDetail = "Attendance Predictor works internally on data from Absence Planner.\n" +
            "So there will be no prediction notifications unless there is atleast one plan in Absence Planner.\n" +
            "Click to set a different time.";
    private boolean expanded = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.material_indigo_700));
        }
        final AlarmManager am = (AlarmManager) getBaseContext().getSystemService(ALARM_SERVICE);
        Intent intent1 = new Intent(getBaseContext(), notify.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0,intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        String state = new String();
        String sHour=new String();
        String sMinute=new String();
        setContentView(R.layout.activity_settings);
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                        openFileInput("notify")));
                String input;
                while ((input = inputReader.readLine()) != null) {
                    state=input;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("hour")));
            String input;
            while ((input = inputReader.readLine()) != null) {
                sHour=input;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("minute")));
            String input;
            while ((input = inputReader.readLine()) != null) {
                sMinute=input;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        reminderTime =(TextView)findViewById(R.id.textView11);
        mswitch=(SwitchCompat)findViewById(R.id.switch1);
        soundLayout = (RelativeLayout) findViewById(R.id.relative_sound);
        vibrateLayout = (RelativeLayout) findViewById(R.id.relative_vibrate);
        soundSwitch = (SwitchCompat) findViewById(R.id.switch_sound);
        vibrateSwitch = (SwitchCompat) findViewById(R.id.switch_vibrate);
        bunkPlanTime = (TextView) findViewById(R.id.textView_bunk_time);
        bunkPlanLayout = (RelativeLayout) findViewById(R.id.relative_bunk_plan);
        textSwitcher = (TextSwitcher) findViewById(R.id.textSwitcher_bunk_detail);
        bunkPlanExpand = (AppCompatImageButton) findViewById(R.id.expand_bunk_plan);
        sp = getSharedPreferences(BunkPlanNotifier.PREF_FILE, Context.MODE_PRIVATE);

        Toolbar toolbar=(Toolbar)findViewById(R.id.notification_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(state.equals("")||state.equals("off")){
            mswitch.setChecked(false);
            reminderTime.setText("Daily reminder notification to record");
        } else if(state.equals("on")){
            mswitch.setChecked(true);
            reminderTime.setText("Reminder set, daily at "+String.format("%02d:%02d",Integer.parseInt(sHour),Integer.parseInt(sMinute)));
        }

        mswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    final View layout = getLayoutInflater().inflate(R.layout.time_pick, null);
                    final TimePicker time = (TimePicker) layout.findViewById(R.id.timePicker);
                    time.setIs24HourView(true);
                    AlertDialog.Builder dialog = new AlertDialog.Builder(Settings.this);
                    dialog.setView(layout);
                    dialog.setPositiveButton("SET", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSave("notify", "on", MODE_PRIVATE);
                            int hour = time.getCurrentHour();
                            int minute = time.getCurrentMinute();
                            mSave("hour", String.valueOf(hour), MODE_PRIVATE);
                            mSave("minute", String.valueOf(minute), MODE_PRIVATE);
                            long alarm=0;
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, hour);
                            calendar.set(Calendar.MINUTE, minute);
                            calendar.set(Calendar.SECOND, 0);
                            if(calendar.getTimeInMillis()<= System.currentTimeMillis()){
                                alarm=calendar.getTimeInMillis()+(AlarmManager.INTERVAL_DAY);
                            } else{
                                alarm=calendar.getTimeInMillis();
                            }
                            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, 24 * 60 * 60 * 1000, pendingIntent);
                            reminderTime.setText("Reminder set, daily at " + String.format("%02d:%02d", hour, minute));
                        }
                    });
                    dialog.setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mswitch.setChecked(false);
                        }
                    });
                    dialog.show();

                } else {
                    am.cancel(pendingIntent);
                    mSave("notify", "off", MODE_PRIVATE);
                    reminderTime.setText("Daily reminder notification to record");
                }
            }
        });
        soundSettings();
        vibrationSettings();
        bunkPlanSettings();
    }

    private void bunkPlanSettings() {
        final int hour = sp.getInt(BunkPlanNotifier.PREF_HOUR, 5);
        final int minute = sp.getInt(BunkPlanNotifier.PREF_MIN, 0);
        bunkPlanTime.setText("Set at " + String.format("%02d:%02d", hour, minute) + " daily");
        bunkPlanLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog tpd = new TimePickerDialog(Settings.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        spe = sp.edit();
                        spe.putInt(BunkPlanNotifier.PREF_HOUR, i);
                        spe.putInt(BunkPlanNotifier.PREF_MIN, i1);
                        spe.apply();
                        bunkPlanTime.setText("Set at " + String.format("%02d:%02d", i, i1) + " daily");
                        onBootReceiver.setBunkPlanNotifierAlarm(Settings.this);
                    }
                }, hour, minute, true);
                tpd.show();
            }
        });
        textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView myText = new TextView(Settings.this);
                //myText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                myText.setTextSize(16);
                //myText.setTextColor(Color.BLUE);
                return myText;
            }
        });
        textSwitcher.setInAnimation(Settings.this, android.R.anim.fade_in);
        textSwitcher.setOutAnimation(Settings.this, android.R.anim.fade_out);
        bunkPlanExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expanded = !expanded;
                if(expanded) {
                    textSwitcher.setText(bunkDetail);
                    ViewGroup.LayoutParams lp = textSwitcher.getLayoutParams();
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    textSwitcher.setLayoutParams(lp);
                    bunkPlanExpand.setImageResource(R.drawable.ic_expand_less_black_24dp);
                } else {
                    textSwitcher.setText("");
                    ViewGroup.LayoutParams lp = textSwitcher.getLayoutParams();
                    lp.height = 0;
                    textSwitcher.setLayoutParams(lp);
                    bunkPlanExpand.setImageResource(R.drawable.ic_expand_more_black_24dp);
                }
            }
        });
    }

    private void vibrationSettings() {
        vibrateSwitch.setChecked(sp.getBoolean(PREF_VIBRATE, true));
        vibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                spe = sp.edit();
                spe.putBoolean(PREF_VIBRATE, isChecked);
                spe.apply();
            }
        });
        vibrateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrateSwitch.toggle();
            }
        });
    }

    private void soundSettings() {
        soundSwitch.setChecked(sp.getBoolean(PREF_SOUND, true));
        soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                spe = sp.edit();
                spe.putBoolean(PREF_SOUND, isChecked);
                spe.apply();
            }
        });
        soundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundSwitch.toggle();
            }
        });
    }

    public  void mSave(String file, String data, int mode){
        FileOutputStream fos;
        try{
            fos=openFileOutput(file, mode);
            fos.write(data.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Settings.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
