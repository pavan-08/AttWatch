package com.psychapps.attwatch.adapters;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.psychapps.attwatch.R;
import com.psychapps.attwatch.entity.BunkPlanner;
import com.psychapps.attwatch.entity.PlannerSubjects;
import com.psychapps.attwatch.entity.Subjects;
import com.psychapps.attwatch.entity.TimeTable;
import com.psychapps.attwatch.helpers.AsyncBunkPlansEvaluator;
import com.psychapps.attwatch.helpers.DBHelper;
import com.psychapps.attwatch.interfaces.EventListener;
import com.psychapps.attwatch.interfaces.TaskListener;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Pavan on 6/17/2016.
 */
public class PlannerRecyclerAdapter extends RecyclerView.Adapter<PlannerRecyclerAdapter.Holder> {

    private AppCompatActivity activity;
    private LayoutInflater layoutInflater;
    private ArrayList<BunkPlanner> bunkPlanners = new ArrayList<BunkPlanner>();
    private ArrayList<PlannerSubjects> plannerSubjects = new ArrayList<PlannerSubjects>();
    private ArrayList<Integer> deletePositions = new ArrayList<Integer>();
    private DBHelper dbHelper;
    private SimpleDateFormat sdf;
    private SimpleDateFormat sdfParser;
    private ActionMode.Callback actionModeCallback;
    private ActionMode actionMode;
    private TaskListener taskListener;
    private EventListener eventListener;

    public PlannerRecyclerAdapter(AppCompatActivity appCompatActivity) {
        this.activity = appCompatActivity;
        layoutInflater = LayoutInflater.from(activity);
        dbHelper = new DBHelper(activity);
        sdf = new SimpleDateFormat("EEE, MMM d, ''yy", Locale.getDefault());
        sdfParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        actionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.action_menu_bunk_planner, menu);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.material_indigo_400));
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mActionMode, MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.planner_select_all:
                        changeSelectionAll(true);
                        actionMode.setTitle(String.valueOf(bunkPlanners.size()));
                        break;
                    case R.id.planner_delete:
                        deleteSelected();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mActionMode) {
                actionMode = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.material_indigo_700));
                }
                changeSelectionAll(false);
            }
        };
        eventListener = (EventListener)activity;
        taskListener = new TaskListener() {
            @Override
            public void onTaskBegin() {
                eventListener.setProgressBarVisibility(View.VISIBLE);
                activity.getIntent().putExtra("paused", false);
            }

            @Override
            public void onTaskCompleted() {
                actionMode.finish();
                eventListener.setProgressBarVisibility(View.GONE);
                activity.getIntent().putExtra("paused", true);
                int x = 0;
                for(int i : deletePositions) {
                    bunkPlanners.remove(i);
                    for(int j = x; j < deletePositions.size(); j++) {
                        if(deletePositions.get(j) > i) {
                            deletePositions.set(j, deletePositions.get(j) - 1);
                        }
                    }
                    notifyItemRemoved(i);
                    x++;
                }
                deletePositions.clear();
                setItems();
                if(getItemCount() == 0) {
                    eventListener.setTextVisibility(View.VISIBLE);
                }
            }
        };
    }


    @Override
    public PlannerRecyclerAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.planner_recycler_layout, parent, false);
        return new PlannerRecyclerAdapter.Holder(view);
    }

    @Override
    public void onBindViewHolder(final PlannerRecyclerAdapter.Holder holder, int position) {
        try {
            holder.date.setText(sdf.format(sdfParser.parse(bunkPlanners.get(position).getDate())));
        } catch (ParseException e) {
            e.printStackTrace();
            holder.date.setText(bunkPlanners.get(position).getDate());
        }
        if(bunkPlanners.get(position).isSelected()) {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(activity, R.color.indigo_highlight));
        } else {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.white));
        }
        if(bunkPlanners.get(position).isExpanded()) {
            holder.toggleContent.setImageResource(R.drawable.ic_expand_less_black_24dp);
            holder.moreContent.setText(getFormattedSubjects(holder.getAdapterPosition()));
        } else {
            holder.toggleContent.setImageResource(R.drawable.ic_expand_more_black_24dp);
            holder.moreContent.setText("");
        }
        holder.toggleContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bunkPlanners.get(holder.getAdapterPosition()).toggleExpanded();
                notifyItemChanged(holder.getAdapterPosition());
            }
        });
        ((GradientDrawable)holder.circle.getBackground()).setColor(ContextCompat.getColor(activity, computeColor(holder.getAdapterPosition())));
        ((GradientDrawable)holder.lower.getBackground()).setColor(ContextCompat.getColor(activity, computeColor(holder.getAdapterPosition())));
        ((GradientDrawable)holder.upper.getBackground()).setColor(ContextCompat.getColor(activity, computeColor(holder.getAdapterPosition())));
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(actionMode == null) {
                    actionMode = activity.startSupportActionMode(actionModeCallback);
                    actionMode.setTitle(String.valueOf(1));
                    bunkPlanners.get(holder.getAdapterPosition()).setSelected(true);
                    notifyItemChanged(holder.getAdapterPosition());
                    return true;
                }
                return false;
            }
        });
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(actionMode != null) {
                    if(bunkPlanners.get(holder.getAdapterPosition()).isSelected()) {
                        bunkPlanners.get(holder.getAdapterPosition()).setSelected(false);
                        actionMode.setTitle(String.valueOf(Integer.parseInt(actionMode.getTitle().toString()) - 1));
                    } else {
                        bunkPlanners.get(holder.getAdapterPosition()).setSelected(true);
                        actionMode.setTitle(String.valueOf(Integer.parseInt(actionMode.getTitle().toString()) + 1));
                    }
                    if(isNoneSelected()) {
                        actionMode.finish();
                    }
                    notifyItemChanged(holder.getAdapterPosition());
                }
            }
        });
    }

    private boolean isNoneSelected() {
        for (BunkPlanner bp :
                bunkPlanners) {
            if (bp.isSelected()) {
                return false;
            }
        }
        return true;
    }

    private void changeSelectionAll(boolean selected) {
        for(BunkPlanner bp: bunkPlanners) {
            bp.setSelected(selected);
        }
        notifyItemRangeChanged(0, bunkPlanners.size());
    }

    private void deleteSelected() {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        deletePositions.clear();
        int i = 0;
        for(BunkPlanner bp: bunkPlanners) {
            if(bp.isSelected()) {
                ids.add(bp.get_id());
                deletePositions.add(i);
            }
            i++;
        }
        try {
            dbHelper.open();
            dbHelper.deleteBunkPlans(ids);
            dbHelper.close();
            new AsyncBunkPlansEvaluator(taskListener, activity).execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    private int computeColor(int adapterPosition) {
        adapterPosition = adapterPosition > -1 ? adapterPosition : 0;
        switch (bunkPlanners.get(adapterPosition).getStatus()) {
            case 0:
                return R.color.material_orange;
            case 1:
                return R.color.material_green;
            case -1:
                return R.color.material_red;
        }
        return 0;
    }

    private String getFormattedSubjects(int adapterPosition) {
        String formattedString = "";
        for (int i = 0; i < plannerSubjects.get(adapterPosition).getSubjects().size(); i++) {
            formattedString += String.valueOf(i+1) + ". " + plannerSubjects.get(adapterPosition).getSubjects().get(i).getName() + "\n";
        }
        if(plannerSubjects.get(adapterPosition).getSubjects().size() == 0) {
            formattedString = "Seems you have no lectures, you may easily miss the day.";
        }
        return formattedString;
    }

    @Override
    public int getItemCount() {
        return bunkPlanners.size();
    }

    public void setItems() {
        bunkPlanners.clear();
        try {
            dbHelper.open();
            bunkPlanners = dbHelper.getBunkPlans();
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        fetchSubjects();
    }

    private void fetchSubjects() {
        plannerSubjects.clear();
        for (BunkPlanner bunkPlan:
             bunkPlanners) {
            PlannerSubjects plannerSubject = new PlannerSubjects();
            ArrayList<Subjects> subjectList = new ArrayList<Subjects>();
            ArrayList<TimeTable> lectures = new ArrayList<TimeTable>();
            plannerSubject.setBp_id(bunkPlan.get_id());
            try {
                String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(sdfParser.parse(bunkPlan.getDate()));
                dbHelper.open();
                lectures = dbHelper.getLectures(day);
                for (TimeTable lecture :
                        lectures) {
                    subjectList.add(lecture.getSubject());
                }
                dbHelper.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } finally {
                plannerSubject.setSubjects(subjectList);
                plannerSubjects.add(plannerSubject);
                notifyItemRangeChanged(0, bunkPlanners.size());
            }
        }
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public TextView date, moreContent;
        public AppCompatImageButton toggleContent;
        public ImageView circle, upper, lower;
        public CardView cardView;

        public Holder(final View itemView) {
            super(itemView);
            date = (TextView) itemView.findViewById(R.id.planner_recycler_date);
            moreContent = (TextView) itemView.findViewById(R.id.more_text_view);
            toggleContent = (AppCompatImageButton) itemView.findViewById(R.id.show_more_less);
            circle = (ImageView) itemView.findViewById(R.id.planner_circle);
            upper = (ImageView) itemView.findViewById(R.id.planner_upper_line);
            lower = (ImageView) itemView.findViewById(R.id.planner_lower_line);
            cardView = (CardView) itemView.findViewById(R.id.planner_cardview);
        }
    }
}
