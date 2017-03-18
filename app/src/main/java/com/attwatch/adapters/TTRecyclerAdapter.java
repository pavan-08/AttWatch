package com.attwatch.adapters;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.attwatch.helpers.DBHelper;
import com.attwatch.R;
import com.attwatch.entity.TimeTable;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Pavan on 5/22/2016.
 */
public class TTRecyclerAdapter extends RecyclerView.Adapter<TTRecyclerAdapter.Holder> {
    public Activity activity;
    private LayoutInflater mLayoutInflater;
    private String day;
    private ArrayList<TimeTable> timeTables = new ArrayList<>();
    private ArrayList<Integer> attended = new ArrayList<>();
    private ArrayList<Integer> missed = new ArrayList<>();
    private DBHelper dbHelper;

    public TTRecyclerAdapter (Activity act) {
        this.activity = act;
        mLayoutInflater= LayoutInflater.from(act);
    }

    public TTRecyclerAdapter(Activity act, String day){
        this.day = day;
        this.activity=act;
        dbHelper = new DBHelper(activity);
        mLayoutInflater=LayoutInflater.from(act);
    }

    @Override
    public TTRecyclerAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View row = mLayoutInflater.inflate(R.layout.recycler_layout1, parent, false);
        Holder holder = new Holder(row);
        return holder;
    }

    @Override
    public void onBindViewHolder(final TTRecyclerAdapter.Holder holder, int position) {
        holder.sub_name.setText(timeTables.get(position).getSubject().getName());
        holder.attended.setText(attended.get(position).toString());
        holder.missed.setText(missed.get(position).toString());
        if(timeTables.get(position).getStatus() == 1) {
            holder.status.setVisibility(View.VISIBLE);
        } else {
            holder.status.setVisibility(View.GONE);
        }
        if(position == getItemCount() - 1) {
            holder.rectangle.setVisibility(View.INVISIBLE);
        } else {
            holder.rectangle.setVisibility(View.VISIBLE);
        }
        ((GradientDrawable)holder.circle.getBackground()).setColor(ContextCompat.getColor(activity, R.color.material_indigo));
        ((GradientDrawable)holder.rectangle.getBackground()).setColor(ContextCompat.getColor(activity, R.color.material_indigo));
        holder.attended.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i = Integer.parseInt(holder.attended.getText().toString());
                i++;
                try {
                    dbHelper.open();
                    dbHelper.addAttendance(timeTables.get(holder.getAdapterPosition()).getId(), 1, timeTables.get(holder.getAdapterPosition()).getSubject().getId());
                    dbHelper.close();
                    attended.set(holder.getAdapterPosition(), i);
                    holder.attended.setText(String.valueOf(i));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        holder.missed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i = Integer.parseInt(holder.missed.getText().toString());
                i++;
                try {
                    dbHelper.open();
                    dbHelper.addAttendance(timeTables.get(holder.getAdapterPosition()).getId(), 0, timeTables.get(holder.getAdapterPosition()).getSubject().getId());
                    dbHelper.close();
                    missed.set(holder.getAdapterPosition(), i);
                    holder.missed.setText(String.valueOf(i));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        holder.attended.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int i = Integer.parseInt(holder.attended.getText().toString());
                if(i > 0) {
                    i--;
                    try {
                        dbHelper.open();
                        dbHelper.deleteAttendance(timeTables.get(holder.getAdapterPosition()).getId(), 1);
                        dbHelper.close();
                        attended.set(holder.getAdapterPosition(), i);
                        holder.attended.setText(String.valueOf(i));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
        holder.missed.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int i = Integer.parseInt(holder.missed.getText().toString());
                if(i>0) {
                    i--;
                    try {
                        dbHelper.open();
                        dbHelper.deleteAttendance(timeTables.get(holder.getAdapterPosition()).getId(), 0);
                        dbHelper.close();
                        missed.set(holder.getAdapterPosition(), i);
                        holder.missed.setText(String.valueOf(i));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);

                final CharSequence options[] = {"Delete", "Reset"};
                alert.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (options[which] == "Delete") {
                            AlertDialog.Builder ask = new AlertDialog.Builder(activity);
                            ask.setMessage("Do you want to delete attendance recorded in this lecture for the subject as well?");
                            ask.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        dbHelper.open();
                                        dbHelper.deleteAllAttendance(timeTables.get(holder.getAdapterPosition()).getId());
                                        dbHelper.deleteLecture(timeTables.get(holder.getAdapterPosition()).getId());
                                        dbHelper.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    removeLecture(holder.getAdapterPosition());
                                    notifyItemRemoved(holder.getAdapterPosition());
                                }
                            });
                            ask.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        dbHelper.open();
                                        dbHelper.deleteLecture(timeTables.get(holder.getAdapterPosition()).getId());
                                        dbHelper.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    removeLecture(holder.getAdapterPosition());
                                    notifyItemRemoved(holder.getAdapterPosition());
                                }
                            });
                            ask.show();

                        } else if (options[which] == "Reset") {
                            try {
                                dbHelper.open();
                                dbHelper.deleteAllAttendance(timeTables.get(holder.getAdapterPosition()).getId());
                                dbHelper.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            attended.set(holder.getAdapterPosition(), 0);
                            missed.set(holder.getAdapterPosition(), 0);
                            notifyItemChanged(holder.getAdapterPosition());
                        }
                    }
                });
                alert.show();

                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return attended.size();
    }

    public void setTimeTables(ArrayList<TimeTable> timeTables) {
        this.timeTables = (ArrayList<TimeTable>)timeTables.clone();
    }

    public void addLecture(TimeTable timeTable) {
        timeTables.add(timeTable);
        attended.add(0);
        missed.add(0);
        notifyDataSetChanged();
    }

    public void addAttended(int count, int position) {
        attended.add(position, count);
    }

    public void addMissed(int count, int position) {
        missed.add(position, count);
        notifyDataSetChanged();
    }

    public void removeLecture(int position) {
        timeTables.remove(position);
        attended.remove(position);
        missed.remove(position);
    }

    public int getLecture(int position) {
        return timeTables.get(position).getId();
    }

    public class Holder extends RecyclerView.ViewHolder {
        RelativeLayout cardView;
        TextView sub_name;
        TextView status;
        Button attended;
        Button missed;
        ImageView circle;
        ImageView rectangle;
        public Holder(View itemView) {
            super(itemView);
            cardView=(RelativeLayout) itemView.findViewById(R.id.subjectCard);
            sub_name = (TextView) itemView.findViewById(R.id.textView);
            attended = (Button) itemView.findViewById(R.id.button);
            missed = (Button) itemView.findViewById(R.id.button2);
            status = (TextView)itemView.findViewById(R.id.textView6);
            circle = (ImageView) itemView.findViewById(R.id.centerCircle);
            rectangle = (ImageView) itemView.findViewById(R.id.lowerRectangle);
        }
    }
}
