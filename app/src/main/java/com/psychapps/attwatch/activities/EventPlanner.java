package com.psychapps.attwatch.activities;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.psychapps.attwatch.R;
import com.psychapps.attwatch.adapters.EventPlannerAdapter;
import com.psychapps.attwatch.helpers.DBHelper;
import com.psychapps.attwatch.interfaces.EventsListChangeListener;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventPlanner extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EventPlannerAdapter eventPlannerAdapter;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private TextView textView;
    private DBHelper dbHelper;
    private static final String message = "Event Planner is a place where all curricular and " +
            "extra-curricular events need to be recorded. Curricular events are those when missing " +
            "a day is not possible and extra-curricular events are those when we have to miss lectures " +
            "due to those events.";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_planner);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final LayoutInflater inflater = getLayoutInflater();
        dbHelper = new DBHelper(this);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        textView = (TextView) findViewById(R.id.planner_textview);
        textView.setText(message);
        recyclerView = (RecyclerView) findViewById(R.id.event_recycler);
        eventPlannerAdapter = new EventPlannerAdapter(this);
        eventPlannerAdapter.setItemsChangedListener(new EventsListChangeListener() {
            @Override
            public void listChanged() {
                if(eventPlannerAdapter.getItemCount() > 0) {
                    textView.setVisibility(View.INVISIBLE);
                } else {
                    textView.setVisibility(View.VISIBLE);
                }
            }
        });
        recyclerView.setAdapter(eventPlannerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventPlannerAdapter.setItems();


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEventDialogue(inflater);
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Snackbar.make(v, "Add new event", Snackbar.LENGTH_LONG)
                        .setAction("Add", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addEventDialogue(inflater);
                            }
                        }).show();
                return false;
            }
        });

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

    }

    private void addEventDialogue(LayoutInflater inflater) {
        AlertDialog.Builder add = new AlertDialog.Builder(EventPlanner.this);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault());
        View view = inflater.inflate(R.layout.add_event, null);
        add.setView(view);
        final AppCompatSpinner dropdown = (AppCompatSpinner)view.findViewById(R.id.event_type);
        String[] items = new String[]{"Curricular", "Extra-Curricular"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        final TextView startDate, endDate;
        startDate = (TextView) view.findViewById(R.id.start_date);
        endDate = (TextView) view.findViewById(R.id.end_date);
        final EditText eName = (EditText) view.findViewById(R.id.e_name);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(EventPlanner.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, month, dayOfMonth);
                        startDate.setText(dateFormat.format(new Date(calendar.getTimeInMillis())));
                    }
                }, mYear, mMonth, mDay);
                dpd.show();
            }
        });
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(EventPlanner.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, month, dayOfMonth);
                        endDate.setText(dateFormat.format(new Date(calendar.getTimeInMillis())));
                    }
                }, mYear, mMonth, mDay);
                dpd.show();
            }
        });

        add.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean valid = true;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    valid = !eName.getText().toString().equals("")
                         && sdf.parse(startDate.getText().toString())
                            .compareTo(sdf.parse(endDate.getText().toString())) <= 0;
                } catch (ParseException e) {
                    e.printStackTrace();
                    valid = false;
                }
                if(valid) {
                    try {
                        dbHelper.open();
                        dbHelper.addEvent(eName.getText().toString(), dropdown.getSelectedItemPosition(),
                                startDate.getText().toString(), endDate.getText().toString());
                        dbHelper.close();
                        eventPlannerAdapter.setItems();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(EventPlanner.this, "Check your input data", Toast.LENGTH_SHORT).show();
                }
            }
        });
        add.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        add.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(EventPlanner.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
