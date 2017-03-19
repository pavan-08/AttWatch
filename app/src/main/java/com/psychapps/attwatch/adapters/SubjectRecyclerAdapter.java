package com.psychapps.attwatch.adapters;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.psychapps.attwatch.activities.SubjectLog;
import com.psychapps.attwatch.helpers.DBHelper;
import com.psychapps.attwatch.activities.MainActivity;
import com.psychapps.attwatch.R;
import com.psychapps.attwatch.entity.Subjects;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Pavan on 24/04/2015.
 */
public class SubjectRecyclerAdapter extends RecyclerView.Adapter<SubjectRecyclerAdapter.Holder>{
    private ArrayList<String> mListData = new ArrayList<>();
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> attend = new ArrayList<>();
    private ArrayList<String> miss = new ArrayList<>();
    private ArrayList<String> percent=new ArrayList<>();
    private LayoutInflater mLayoutInflater;
    public Activity activity;
    public static int num;
    private DBHelper dbHelper;
    private FragmentManager fragmentManager;

    public SubjectRecyclerAdapter(Activity act){
        this.activity=act;
        mLayoutInflater=LayoutInflater.from(act);
    }
    public SubjectRecyclerAdapter(Activity act, int number, FragmentManager fragmentManager)
    {
        num=number;
        this.activity=act;
        this.fragmentManager = fragmentManager;
        dbHelper = new DBHelper(act);
        mLayoutInflater=LayoutInflater.from(act);
    }

    @Override
    public SubjectRecyclerAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View row = mLayoutInflater.inflate(R.layout.recycler_layout, parent, false);
            Holder holder = new Holder(row);
            return holder;
    }


    @Override
    public void onBindViewHolder(final SubjectRecyclerAdapter.Holder holder, int position) {
        final String data = mListData.get(position);
        final String per = percent.get(position);
        if(attend.size()>position) {
            String mAttend = attend.get(position);
            holder.attended.setText(mAttend);
        }
        else
        {
            attend.add(position,"0");
        }
        if(miss.size()>position){
            String mMiss = miss.get(position);
            holder.missed.setText(mMiss);
        }
        else
        {
            miss.add(position,"0");
        }

        final int i = Integer.parseInt(holder.attended.getText().toString());
        final int j = Integer.parseInt(holder.missed.getText().toString());
        holder.sub_name.setText(data);
        int t =i+j;
        float p =Float.parseFloat(per);
        if(t!=0){
            if(p>99) {
                if(j>0){
                    holder.detail.setText("Defaulter forever!!");
                } else{
                    holder.detail.setText("Do not miss a single lecture!");
                }
            } else {
                int x = (int) (((100 / p) * i) - i);
                int y = (int) Math.ceil((100 / (100 - p) * j) - j);
                if (x - j < 0) {
                    holder.detail.setText("Attend minimum : " + String.valueOf(y - i));

                } else {
                    holder.detail.setText("Flexible leaves available : " + String.valueOf(x - j));
                }
            }

        } else{
            holder.detail.setText("");
        }

        String st = getStatus(i, j, Integer.parseInt(per));
        int col = getColor(i, j, Integer.parseInt(per));
        holder.status.setText(st);
        if(!st.equals("Start with lectures")) {
            holder.status.append("%");
            holder.progressBar.setProgress((int) Float.parseFloat(st));
        } else {
            holder.progressBar.setProgress(0);
        }
        holder.status.setTextColor(col);
        holder.progressBar.getProgressDrawable().setColorFilter(col, PorterDuff.Mode.SRC_IN);
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            ArrayList<String> hour = new ArrayList<String>();
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);

                final CharSequence options[] = {"Edit", "Reset", "Delete"};
                alert.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (options[which] == "Delete") {
                            AlertDialog.Builder ask = new AlertDialog.Builder(activity);
                            ask.setMessage("Are you sure to delete this subject?");
                            ask.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        dbHelper.open();
                                        dbHelper.deleteSubject(ids.get(holder.getAdapterPosition()));
                                        dbHelper.close();
                                        ids.remove(holder.getAdapterPosition());
                                        attend.remove(holder.getAdapterPosition());
                                        miss.remove(holder.getAdapterPosition());
                                        percent.remove(holder.getAdapterPosition());
                                        mListData.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                            ask.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            ask.show();
                        } else if (options[which] == "Reset") {
                            AlertDialog.Builder ask = new AlertDialog.Builder(activity);
                            ask.setMessage("Are you sure to reset attendance of this subject?");
                            ask.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        dbHelper.open();
                                        dbHelper.deleteAllAttendanceBySubject(ids.get(holder.getAdapterPosition()));
                                        dbHelper.close();
                                        miss.set(holder.getAdapterPosition(), "0");
                                        attend.set(holder.getAdapterPosition(), "0");
                                        notifyItemChanged(holder.getAdapterPosition());
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            ask.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            ask.show();
                        } else if( options[which] == "Edit") {
                            AlertDialog.Builder add=new AlertDialog.Builder(activity);
                            final View layout =mLayoutInflater.inflate(R.layout.add_subject,null);
                            add.setView(layout);
                            final EditText sub = (EditText) layout.findViewById(R.id.editText);
                            final EditText perc = (EditText) layout.findViewById(R.id.editText2);
                            Toolbar toolbar =(Toolbar)layout.findViewById(R.id.view2);
                            toolbar.setTitle("Edit Subject");
                            sub.setText(data);
                            perc.setText(per);
                            add.setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Toast.makeText(activity,"save.",Toast.LENGTH_SHORT).show();
                                    String subject = sub.getText().toString();
                                    String Percent = perc.getText().toString();
                                    if (subject.equals("")) {
                                        Toast.makeText(activity, "Empty Subject Inputs", Toast.LENGTH_SHORT).show();
                                    } else if (percent.equals("")) {
                                        Toast.makeText(activity, "Empty Percent Inputs", Toast.LENGTH_SHORT).show();
                                    } else if (MainActivity.isNotNumeric(Percent)) {
                                        Toast.makeText(activity, percent + "is not a number", Toast.LENGTH_LONG).show();
                                    } else if (Integer.parseInt(Percent) > 100 || Integer.parseInt(Percent) < 0) {
                                        Toast.makeText(activity, "invalid percent limit", Toast.LENGTH_SHORT).show();
                                    } else {
                                        mListData.set(holder.getAdapterPosition(), subject);
                                        percent.set(holder.getAdapterPosition(), Percent);
                                        ContentValues cv = new ContentValues();
                                        cv.put(DBHelper.FeedEntry.COLUMN_NAME_SNAME,subject);
                                        cv.put(DBHelper.FeedEntry.COLUMN_NAME_LIMIT, Integer.parseInt(Percent));
                                        try {
                                            dbHelper.open();
                                            dbHelper.updateSubject(cv, ids.get(holder.getAdapterPosition()));
                                            dbHelper.close();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                        notifyItemChanged(holder.getAdapterPosition());
                                    }
                                }
                            });
                            add.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Toast.makeText(activity, "Canceled", Toast.LENGTH_SHORT).show();
                                }
                            });
                            add.show();
                        }
                    }
                });
                alert.show();
                return true;
            }
        });
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent subjectLogIntent = new Intent(activity, SubjectLog.class);
                subjectLogIntent.putExtra("subjectID", ids.get(holder.getAdapterPosition()));
                subjectLogIntent.putExtra("subjectName", mListData.get(holder.getAdapterPosition()));
                activity.startActivity(subjectLogIntent);
            }
        });
    }

    public static int getColor(int a, int b,int l) {
        int t = a + b;
        if (t != 0) {
            float p = (float)(a)/(float)(t) * 100;
            if(p>=l+5&&p<=100)
                return (Color.parseColor("#00C853"));
            else if(p>=l&&p<l+5)
                return(Color.parseColor("#FF6D00")) ;
            else
                return(Color.parseColor("#D50000")) ;
        } else
            return(Color.parseColor("#607D8B")) ;

    }

    public static String getStatus(int a, int b,int l){
        int t = a + b;
        if (t != 0) {
            float p = (float)(a)/(float)(t) * 100;
            if(p>=l+5&&p<=100)
                return(String.format("%.2f",p));
            else if(p>=l&&p<l+5)
                return(String.format("%.2f",p));
            else
                return(String.format("%.2f",p));
        } else
            return("Start with lectures");
    }
    public void addId(int id) {
        ids.add(id);
    }
    public void addAttended(String item){
        attend.add(item);
    }
    public void addMissed(String item){
        miss.add(item);
    }
    public void addItem(ArrayList<Subjects> items) {
        for(int i = 0; i < items.size(); i++ ) {
            mListData.add(items.get(i).getName());
            percent.add(String.valueOf(items.get(i).getLimit()));
            addId(items.get(i).getId());
            notifyItemInserted(mListData.size()-1);
        }
    }
    @Override
    public int getItemCount() {
        return mListData.size();
    }
    public static class Holder extends RecyclerView.ViewHolder {

        TextView sub_name;
        TextView detail;
        TextView status;
        Button attended;
        Button missed;
        CardView cardView;
        ProgressBar progressBar;
        public Holder(View itemView) {
            super(itemView);
                cardView = (CardView)itemView.findViewById(R.id.cardViewMain);
                sub_name = (TextView) itemView.findViewById(R.id.textView);
                attended = (Button) itemView.findViewById(R.id.button);
                detail=(TextView)itemView.findViewById(R.id.textView14);
                missed = (Button) itemView.findViewById(R.id.button2);
                status = (TextView) itemView.findViewById(R.id.textView2);
                progressBar = (ProgressBar) itemView.findViewById(R.id.attendanceProgress);
        }
    }
}
