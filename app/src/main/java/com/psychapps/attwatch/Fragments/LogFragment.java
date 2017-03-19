package com.psychapps.attwatch.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.psychapps.attwatch.R;
import com.psychapps.attwatch.adapters.LogRecyclerAdapter;

/**
 * Created by Pavan on 6/5/2016.
 */
public class LogFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogRecyclerAdapter logRecyclerAdapter;

    public LogFragment() {

    }

    public static LogFragment newInstance(int subjectID, int status) {
        LogFragment logFragment = new LogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);
        args.putInt("subjectID", subjectID);
        logFragment.setArguments(args);
        return logFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.log_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = (RecyclerView) view.findViewById(R.id.log_recycler_view);
        logRecyclerAdapter = new LogRecyclerAdapter(getActivity());

        logRecyclerAdapter.setItems(getArguments().getInt("subjectID"), getArguments().getInt("status"));
        recyclerView.setAdapter(logRecyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }
}
