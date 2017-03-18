package com.attwatch.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.attwatch.R;
import com.attwatch.entity.Attendance;
import com.attwatch.helpers.DBHelper;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Pavan on 6/17/2016.
 */
public class LogRecyclerAdapter extends RecyclerView.Adapter<LogRecyclerAdapter.Holder> {

    private LayoutInflater mLayoutInflater;
    private Activity activity;
    private DBHelper dbHelper;
    private ArrayList<Attendance> attendanceList = new ArrayList<Attendance>();

    public LogRecyclerAdapter(Activity activity) {
        this.activity = activity;
        mLayoutInflater = LayoutInflater.from(activity);
        dbHelper = new DBHelper(activity);
    }

    @Override
    public LogRecyclerAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View row = mLayoutInflater.inflate(R.layout.log_recycler_layout, parent, false);
        return new LogRecyclerAdapter.Holder(row);
    }

    @Override
    public void onBindViewHolder(LogRecyclerAdapter.Holder holder, int position) {
        holder.day.setText(attendanceList.get(position).getLecture().getDay());
        holder.date.setText(attendanceList.get(position).getTimestamp());
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    public void setItems(int subjectID, int status) {
        try {
            dbHelper.open();
            attendanceList = dbHelper.getSubjectAttendanceList(subjectID, status);
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class Holder extends RecyclerView.ViewHolder {
        TextView date, day;
        public Holder(View itemView) {
            super(itemView);
            date = (TextView) itemView.findViewById(R.id.log_date);
            day = (TextView) itemView.findViewById(R.id.log_day);
        }
    }
}
