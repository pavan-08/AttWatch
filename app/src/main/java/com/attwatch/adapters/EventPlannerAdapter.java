package com.attwatch.adapters;

import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.attwatch.R;
import com.attwatch.entity.Event;
import com.attwatch.helpers.DBHelper;
import com.attwatch.interfaces.EventsListChangeListener;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by pavan on 18/03/2017.
 */

public class EventPlannerAdapter extends RecyclerView.Adapter<EventPlannerAdapter.Holder> {
    private ArrayList<Event> events = new ArrayList<Event>();
    private AppCompatActivity activity;
    private LayoutInflater layoutInflater;
    private DBHelper dbHelper;
    private ActionMode.Callback actionModeCallback;
    private ActionMode actionMode;
    private EventsListChangeListener listener;

    public EventPlannerAdapter(AppCompatActivity appCompatActivity) {
        this.activity = appCompatActivity;
        layoutInflater = LayoutInflater.from(activity);
        dbHelper = new DBHelper(activity);
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
                        actionMode.setTitle(String.valueOf(events.size()));
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
    }

    private void deleteSelected() {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        try {
            dbHelper.open();
            int i = 0;
            for(Event et: events) {
                if(et.isSelected()) {
                    ids.add(i);
                    dbHelper.deleteEvent(et.getId());
                }
                i++;
            }
            dbHelper.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for(int i: ids) {
            events.remove(i);
            notifyItemRemoved(i);
        }
        listener.listChanged();
        actionMode.finish();
    }

    public void setItems() {
        events.clear();
        try {
            dbHelper.open();
            events = dbHelper.getEvents();
            dbHelper.close();
            notifyDataSetChanged();
            listener.listChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void changeSelectionAll(boolean selected) {
        for(Event et: events) {
            et.setSelected(selected);
        }
        notifyItemRangeChanged(0, events.size());
    }

    private boolean isNoneSelected() {
        for (Event et :
                events) {
            if (et.isSelected()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public EventPlannerAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.e_planner_layout, parent, false);
        return new EventPlannerAdapter.Holder(view);
    }

    @Override
    public void onBindViewHolder(final EventPlannerAdapter.Holder holder, int position) {
        holder.name.setText(events.get(position).getName());
        holder.moreContent.setText(events.get(position).getStart() + " to " + events.get(position).getEnd());
        if(events.get(position).isSelected()) {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(activity, R.color.indigo_highlight));
        } else {
            holder.cardView.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.white));
        }
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(actionMode == null) {
                    actionMode = activity.startSupportActionMode(actionModeCallback);
                    actionMode.setTitle(String.valueOf(1));
                    events.get(holder.getAdapterPosition()).setSelected(true);
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
                    if(events.get(holder.getAdapterPosition()).isSelected()) {
                        events.get(holder.getAdapterPosition()).setSelected(false);
                        actionMode.setTitle(String.valueOf(Integer.parseInt(actionMode.getTitle().toString()) - 1));
                    } else {
                        events.get(holder.getAdapterPosition()).setSelected(true);
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

    public void setItemsChangedListener(EventsListChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public class Holder extends RecyclerView.ViewHolder {
        public TextView name, moreContent;
        public CardView cardView;
        public Holder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.planner_recycler_date);
            moreContent = (TextView) itemView.findViewById(R.id.more_text_view);
            /*circle = (ImageView) itemView.findViewById(R.id.planner_circle);
            upper = (ImageView) itemView.findViewById(R.id.planner_upper_line);
            lower = (ImageView) itemView.findViewById(R.id.planner_lower_line);*/
            cardView = (CardView) itemView.findViewById(R.id.planner_cardview);
        }
    }
}
