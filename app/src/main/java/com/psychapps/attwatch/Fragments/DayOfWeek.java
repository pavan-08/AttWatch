package com.psychapps.attwatch.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.psychapps.attwatch.helpers.DBHelper;
import com.psychapps.attwatch.R;
import com.psychapps.attwatch.adapters.TTRecyclerAdapter;

import android.support.design.widget.FloatingActionButton;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Pavan on 5/24/2016.
 */
public class DayOfWeek extends Fragment {
    private RecyclerView mRecycler;
    private TTRecyclerAdapter mAdapter;
    private String hour;
    private FloatingActionButton FAB;
    private TextView intro;
    private DBHelper dbHelper;
    private AppBarLayout appBarLayout;

    public DayOfWeek() {

    }

    public static DayOfWeek newInstance(String position) {
        DayOfWeek fragment = new DayOfWeek();
        Bundle args =new Bundle();
        args.putString("position",position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_layout, container, false);
        dbHelper = new DBHelper(getActivity());
        try {
            dbHelper.open();
            hour = dbHelper.getLectureCount(getArguments().getString("position"));
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
            hour = "";
        }

        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final
    Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*FrameLayout frameLayout = (FrameLayout)view.findViewById(R.id.frame1);
        frameLayout.setPadding(0, ScrollingFABBehavior.getToolbarHeight(getActivity()),0,0);*/
        final String day = getArguments().getString("position");
        intro=(TextView)view.findViewById(R.id.lec_intro);
        mRecycler = (RecyclerView) view.findViewById(R.id.view);
        FAB=(FloatingActionButton)view.findViewById(R.id.fab1);
        appBarLayout = (AppBarLayout)getActivity().findViewById(R.id.appBarLayout);
        FAB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Snackbar.make(view, "Add new lecture", Snackbar.LENGTH_SHORT).show();
                return true;
            }
        });
        FAB.setOnClickListener(new View.OnClickListener() {
            AlertDialog.Builder dialog =new AlertDialog.Builder(getActivity());

            @Override
            public void onClick(View v) {
                ArrayList<com.psychapps.attwatch.entity.Subjects> subs =new ArrayList<>();
                try {
                    dbHelper.open();
                    subs = (ArrayList<com.psychapps.attwatch.entity.Subjects>)dbHelper.getSubjects().clone();
                    dbHelper.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ArrayList<String> subNames = new ArrayList<String>();
                for(int i = 0; i < subs.size(); i++) {
                    subNames.add(subs.get(i).getName());
                }
                final CharSequence[] subjects = subNames.toArray(new CharSequence[subs.size()]);
                if(subs.size()<1){
                    Snackbar.make(view, "Add some subjects at the tab 'Subjects'.", Snackbar.LENGTH_SHORT).show();
                } else {
                    final ArrayList<com.psychapps.attwatch.entity.Subjects> finalSubs = (ArrayList<com.psychapps.attwatch.entity.Subjects>)subs.clone();
                    dialog.setItems(subjects, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                dbHelper.open();
                                long id = dbHelper.addLecture(day, finalSubs.get(which).getId());
                                mAdapter.addLecture(dbHelper.getLecture((int) id));
                                dbHelper.close();
                                /*Intent bIntent= new Intent(getActivity(),MainActivity.class);
                                bIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(bIntent);*/
                                //mPager.getAdapter().notifyDataSetChanged();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    subs.clear();
                    dialog.show();
                }
            }
        });
        mAdapter = new TTRecyclerAdapter(getActivity(), day);
        try {
            dbHelper.open();
            mAdapter.setTimeTables(dbHelper.getLectures(day));
            for(int i = 0; i < Integer.parseInt(hour); i++) {
                mAdapter.addAttended(dbHelper.getAttendanceCount(mAdapter.getLecture(i), 1), i);
                mAdapter.addMissed(dbHelper.getAttendanceCount(mAdapter.getLecture(i), 0), i);
            }
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        if(mAdapter.getItemCount()>0){
            intro.setVisibility(View.INVISIBLE);
        } else{
            intro.setVisibility(View.VISIBLE);
        }
        mRecycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                intro.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                if (mAdapter.getItemCount() == 0) {
                    intro.setVisibility(View.VISIBLE);
                }

            }
        });
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                FAB.animate().translationY(-3f*verticalOffset);
            }
        });
    }
}
