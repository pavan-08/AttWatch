package com.attwatch.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.attwatch.R;
import com.attwatch.adapters.InfoRecyclerAdapter;

/**
 * Created by Pavan on 30/04/2015.
 */
public class Timpass extends Fragment {
    private View view;
    private RecyclerView mRecycler;
    private InfoRecyclerAdapter mAdapter;

    public  Timpass(){

    }

    public static Timpass newInstance(int position){
        Timpass fragment =new Timpass();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view =inflater.inflate(R.layout.timpass1,null);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*RelativeLayout relativeLayout = (RelativeLayout)view.findViewById(R.id.rel1);
        relativeLayout.setPadding(0, ScrollingFABBehavior.getToolbarHeight(getActivity()),0,0);*/
        mRecycler=(RecyclerView)view.findViewById(R.id.infoRecycler);
        mAdapter=new InfoRecyclerAdapter(getActivity());
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
    }
}
