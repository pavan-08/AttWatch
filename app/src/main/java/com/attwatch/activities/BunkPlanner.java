package com.attwatch.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.attwatch.R;
import com.attwatch.adapters.PlannerRecyclerAdapter;
import com.attwatch.helpers.AsyncBunkPlansEvaluator;
import com.attwatch.helpers.BunkPlanNotifier;
import com.attwatch.helpers.DBHelper;
import com.attwatch.helpers.onBootReceiver;
import com.attwatch.interfaces.EventListener;
import com.attwatch.interfaces.TaskListener;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BunkPlanner extends AppCompatActivity implements EventListener{

    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private TextView textView;
    private View.OnClickListener planBunk;
    private DBHelper dbHelper;
    private ProgressBar progressBar;
    private PlannerRecyclerAdapter plannerRecyclerAdapter;
    private TaskListener taskListener;
    private boolean isRefreshing = false;

    SimpleDateFormat sdfParser;
    private static final String message = "Absence Planner will help you miss a day previously planned" +
            " for various reasons like entrance exams, competitions, etc.\n" +
            "It guides daily about which lectures to attend to be able to miss on a planned date.\n" +
            "It indicates the immediate possibility of missing a planned date as well, which will update as attendance gets recorded daily.";

    private static final String end = "<i>Note: All indications are subject to current attendance" +
            " and may change as attendance changes or new absence days are planned.<br/>";
    private static final String details = "<p><font color=\"#4CAF50\"><b>Green</b></font> indicates missing that day is completely possible.</p>" +
            "<p><font color=\"#FF9800\"><b>Orange</b></font> indicates missing that day is probably possible.</p>" +
            "<p><font color=\"#F44336\"><b>Red</b></font> indicates missing that day is not possible under any circumstances.</p>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.material_indigo_700));
        }
        setContentView(R.layout.activity_bunk_planner);
        toolbar = (Toolbar) findViewById(R.id.planner_toolbar);
        fab = (FloatingActionButton) findViewById(R.id.planner_fab);
        recyclerView = (RecyclerView) findViewById(R.id.planner_recycler_view);
        textView = (TextView) findViewById(R.id.planner_textview);
        progressBar = (ProgressBar) findViewById(R.id.activity_loader);
        sdfParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dbHelper = new DBHelper(this);
        taskListener = new TaskListener() {
            @Override
            public void onTaskBegin() {
                isRefreshing = true;
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTaskCompleted() {
                progressBar.setVisibility(View.GONE);
                plannerRecyclerAdapter.setItems();
                getIntent().putExtra("paused", true);
                if(plannerRecyclerAdapter.getItemCount() > 0) {
                    textView.setVisibility(View.INVISIBLE);
                }
                isRefreshing = false;
            }
        };

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        plannerRecyclerAdapter = new PlannerRecyclerAdapter(this);
        recyclerView.setAdapter(plannerRecyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            boolean hide=false;
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(hide){
                    fab.animate().translationY(3*fab.getHeight()).setInterpolator(new AccelerateInterpolator()).start();
                } else{
                    fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy>8){
                    hide=true;
                } else if(dy<-5){
                    hide=false;
                }
            }
        });

        planBunk = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(BunkPlanner.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, monthOfYear, dayOfMonth);
                        if(getSharedPreferences(BunkPlanNotifier.PREF_FILE, Context.MODE_PRIVATE).getBoolean(BunkPlanNotifier.PREF_SET, false)) {
                            onBootReceiver.setBunkPlanNotifierAlarm(BunkPlanner.this);
                        }
                        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                            Snackbar.make(view, "That's a Sunday. Enjoy.", Snackbar.LENGTH_SHORT)
                                    .show();
                        }else if (c.compareTo(calendar) < 0) {
                            try {
                                dbHelper.open();
                                dbHelper.addBunkPlan(calendar.getTimeInMillis());
                                dbHelper.close();
                                new AsyncBunkPlansEvaluator(taskListener, BunkPlanner.this, sdfParser.format(calendar.getTime()), ">=").execute();
                            } catch (SQLiteConstraintException e) {
                                //System.out.println(e.toString());
                                Snackbar.make(findViewById(R.id.planner_root),"That date is already planned.", Snackbar.LENGTH_SHORT).show();
                            }
                            catch (SQLException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Snackbar.make(view, "Past is history. Future is a mystery. Hence it can be solved with a plan.", Snackbar.LENGTH_LONG)
                                    .setAction("PLAN", planBunk)
                                    .show();
                        }

                    }
                }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        };
        fab.setOnClickListener(planBunk);
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Snackbar.make(view, "Plan a day to be missed.", Snackbar.LENGTH_LONG)
                        .setAction("Plan", planBunk).show();
                return true;
            }
        });

        textView.setText(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!getIntent().getBooleanExtra("paused", false)) {
            new AsyncBunkPlansEvaluator(taskListener, BunkPlanner.this).execute();
        } else {
            plannerRecyclerAdapter.setItems();
            if(plannerRecyclerAdapter.getItemCount() > 0) {
                textView.setVisibility(View.INVISIBLE);
            }
        }
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(BunkPlanner.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bunk_planner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.planner_info:
                AlertDialog.Builder info = new AlertDialog.Builder(BunkPlanner.this);
                info.setTitle("Absence Planner");
                info.setIcon(android.R.drawable.ic_menu_info_details);
                info.setMessage(Html.fromHtml("<font size=\"4\" >" + details + "</br>" + end + "</font>"));
                info.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                info.show();
                break;
            case R.id.planner_refresh:
                if(!isRefreshing) {
                    new AsyncBunkPlansEvaluator(taskListener, BunkPlanner.this);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setProgressBarVisibility(int visibility) {
        progressBar.setVisibility(visibility);
    }

    @Override
    public void setTextVisibility(int visibility) {
        textView.setVisibility(visibility);
    }
}
