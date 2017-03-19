package com.psychapps.attwatch.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.psychapps.attwatch.helpers.DBHelper;
import com.psychapps.attwatch.R;
import com.psychapps.attwatch.entity.Subjects;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;


public class IntroScreen extends Activity {
    private final int SPLASH_DISPLAY_LENGTH = 2000;
    private DBHelper dbHelper;
    private TextView optimizing;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_screen);
        dbHelper = new DBHelper(this);
        optimizing = (TextView) findViewById(R.id.intro_optimize);
        optimizing.setVisibility(View.INVISIBLE);
        if(checkIfOnFiles()) {
            optimizing.setVisibility(View.VISIBLE);
            transferData();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Create an Intent that will start the Main-Activity. */
                optimizing.setVisibility(View.INVISIBLE);
                Intent mainIntent = new Intent(IntroScreen.this, MainActivity.class);
                IntroScreen.this.startActivity(mainIntent);
                IntroScreen.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    private boolean checkIfOnFiles() {
        File dir = getFilesDir();
        File file = new File(dir, "Subjects");
        return file.exists();
    }

    public void transferData() {
        StringBuffer stringBuffer =new StringBuffer();
        ArrayList<String> subs =new ArrayList<>();
        ArrayList<String> hour = new ArrayList<>();
        int attended = 0, missed = 0;
        Subjects subject = new Subjects();
        int limit = 0;
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("Subjects")));
            int inputChar;
            while ((inputChar = inputReader.read()) != -1) {
                if ((char) inputChar == '~') {
                    subs.add(stringBuffer.toString());
                    stringBuffer.delete(0, stringBuffer.length());
                } else {
                    stringBuffer.append((char) inputChar);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuffer stringBuffer1=new StringBuffer();
        for(int i=1;i<=6;i++) {
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                        openFileInput("hours"+String.valueOf(i))));
                String input;
                while ((input = inputReader.readLine()) != null) {
                    hour.add(input);
                }
                deleteRecord("hours"+String.valueOf(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Plotting table: subjects
        if(subs.size() == 0) {
            deleteRecord("Subjects");
        }
        for(int i=0;i<subs.size();i++){
            try {
                stringBuffer = new StringBuffer();
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                        openFileInput(subs.get(i))));
                int inputChar;
                while ((inputChar = inputReader.read()) != -1) {
                    if ((char) inputChar == '~') {
                        stringBuffer.delete(0, stringBuffer.length());
                    } else if ((char) inputChar == '`') {
                        limit = Integer.parseInt(stringBuffer.toString());
                    }
                    else {
                        stringBuffer.append((char) inputChar);
                    }
                }
                dbHelper.open();
                dbHelper.addSubject(subs.get(i), limit);
                dbHelper.close();
                deleteRecord(subs.get(i));
                deleteRecord("a" + subs.get(i));
                deleteRecord("m" + subs.get(i));
                if(i == subs.size() - 1) {
                    deleteRecord("Subjects");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Plotting table: time_table, attendance
        if (hour.size() > 0) {
            for (int i = 2; i < 8; i++) {
                String input;
                for (int j = 0; j < Integer.parseInt(hour.get(i-2)); j++) {
                    long id = 0;
                    try {
                        BufferedReader inputReader2 = new BufferedReader(new InputStreamReader(
                                openFileInput(String.valueOf(i) + String.valueOf(j))));

                        while ((input = inputReader2.readLine()) != null) {
                            if(!input.equals("Add Subject")) {
                                dbHelper.open();
                                subject = new Subjects();
                                subject = dbHelper.getSubject(input);
                                String day = getDay(i-2);
                                id = dbHelper.addLecture(day,subject.getId());
                                //Toast.makeText(getBaseContext(), subject.getId() + " " + subject.getName(), Toast.LENGTH_SHORT).show();
                                dbHelper.close();
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try{
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                                openFileInput("a" + String.valueOf(i) + String.valueOf(j))));
                        while ((input = inputReader.readLine()) != null) {
                            attended = Integer.parseInt(input);
                        }
                        dbHelper.open();
                        for(int k = 0; k < attended; k++) {
                            dbHelper.addAttendance((int)id, 1, subject.getId());
                        }
                        dbHelper.close();
                    } catch(IOException e) {
                        attended = 0;
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    try {
                        BufferedReader inputReader1 = new BufferedReader(new InputStreamReader(
                                openFileInput("m" + String.valueOf(i) + String.valueOf(j))));
                        while ((input = inputReader1.readLine()) != null) {
                            missed = Integer.parseInt(input);
                        }
                        dbHelper.open();
                        for(int k = 0; k < missed; k++) {
                            dbHelper.addAttendance((int)id, 0, subject.getId());
                        }
                        dbHelper.close();
                    } catch(IOException e) {
                        attended = 0;
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    deleteRecord(String.valueOf(i) + String.valueOf(j));
                    deleteRecord("a" + String.valueOf(i) + String.valueOf(j));
                    deleteRecord("m"+String.valueOf(i)+String.valueOf(j));
                }
            }
        }
    }

    public static String getDay(int i) {
        String day = "";
        switch(i) {
            case 0:
                day = "Monday";
                break;
            case 1:
                day = "Tuesday";
                break;
            case 2:
                day = "Wednesday";
                break;
            case 3:
                day = "Thursday";
                break;
            case 4:
                day = "Friday";
                break;
            case 5:
                day = "Saturday";
                break;
            case 6:
                day = "Sunday";
                break;
        }
        return day;
    }

    private void deleteRecord(String filename){
        File dir = getFilesDir();
        File file = new File(dir, filename);
        boolean deleted=file.delete();
    }
}
