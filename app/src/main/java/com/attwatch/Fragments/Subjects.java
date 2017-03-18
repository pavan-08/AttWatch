package com.attwatch.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.attwatch.helpers.DBHelper;
import com.attwatch.activities.MainActivity;
import com.attwatch.R;
import com.attwatch.adapters.SubjectRecyclerAdapter;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Pavan on 30/05/2015.
 */
public class Subjects extends Fragment {
    private static Toolbar mToolbar;
    private RecyclerView mRecycler;
    private SubjectRecyclerAdapter mAdapter;
    private FloatingActionButton FAB;
    private TextView intro;
    private DBHelper dbHelper;
    private static View view;
    private AppBarLayout appBarLayout;

    public Subjects(){

    }
    public static Subjects newInstance(int position,Toolbar toolbar) {
        Subjects fragment = new Subjects();
        Bundle args =new Bundle();
        args.putInt("position",position);
        mToolbar=toolbar;
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_layout, container, false);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
       /* FrameLayout frameLayout = (FrameLayout)view.findViewById(R.id.frame1);
        frameLayout.setPadding(0, ScrollingFABBehavior.getToolbarHeight(getActivity()),0,0);*/
        final int num =getArguments().getInt("position");
        appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.appBarLayout);
        ArrayList<com.attwatch.entity.Subjects> subs= new ArrayList<>();
        dbHelper = new DBHelper(getActivity());
        intro=(TextView)view.findViewById(R.id.lec_intro);
        mRecycler = (RecyclerView) view.findViewById(R.id.view);
        mAdapter = new SubjectRecyclerAdapter(getActivity(),num, getActivity().getSupportFragmentManager());
        FAB=(FloatingActionButton)view.findViewById(R.id.fab1);
        FAB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Snackbar.make(view, "Add Subject", Snackbar.LENGTH_SHORT).show();
                return true;
            }
        });
        intro.setText("Click on + to add subjects");
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
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int hours;
                AlertDialog.Builder add = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getLayoutInflater(savedInstanceState);
                final View layout = inflater.inflate(R.layout.add_subject, null);
                add.setView(layout);
                add.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText sub = (EditText) layout.findViewById(R.id.editText);
                        EditText per = (EditText) layout.findViewById(R.id.editText2);
                        String subject = sub.getText().toString();
                        String percent = per.getText().toString();
                        if (subject.equals("")) {
                            Toast.makeText(getActivity(), "Empty Subject Inputs", Toast.LENGTH_SHORT).show();
                        } else if (percent.equals("")) {
                            Toast.makeText(getActivity(), "Empty Percent Inputs", Toast.LENGTH_SHORT).show();
                        } else if (MainActivity.isNotNumeric(percent)) {
                            Toast.makeText(getActivity(), percent + "is not a number", Toast.LENGTH_LONG).show();
                        } else if (Integer.parseInt(percent) > 100 || Integer.parseInt(percent) < 0) {
                            Toast.makeText(getActivity(), "invalid percent limit", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                dbHelper.open();
                                dbHelper.addSubject(subject, Integer.parseInt(percent));
                                ArrayList<com.attwatch.entity.Subjects> temp = new ArrayList<com.attwatch.entity.Subjects>();
                                temp.add(dbHelper.getSubject(subject));
                                mAdapter.addItem(temp);
                                mAdapter.addAttended("0");
                                mAdapter.addMissed("0");
                                dbHelper.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            mRecycler.setAdapter(mAdapter);
                            mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
                        }


                    }
                });

                add.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getActivity(), "Canceled", Toast.LENGTH_SHORT).show();
                    }
                });

                add.show();
            }
        });

        try {
            dbHelper.open();
            subs = (ArrayList< com.attwatch.entity.Subjects>)dbHelper.getSubjects().clone();
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for(int i = 0; i < subs.size(); i++) {
            try {
                dbHelper.open();
                mAdapter.addAttended(dbHelper.getSubjectAttendance(subs.get(i).getId(), 1));
                mAdapter.addMissed(dbHelper.getSubjectAttendance(subs.get(i).getId(), 0));
                dbHelper.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        mAdapter.addItem(subs);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                FAB.animate().translationY(-3f*verticalOffset);
            }
        });
    }
}
