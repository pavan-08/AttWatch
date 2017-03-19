package com.psychapps.attwatch.adapters;

import android.app.Activity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.psychapps.attwatch.R;

import java.util.ArrayList;

/**
 * Created by Pavan on 31/05/2015.
 */
public class InfoRecyclerAdapter extends RecyclerView.Adapter<InfoRecyclerAdapter.Holder> {

    private LayoutInflater mLayoutInflater;
    public Activity activity;
    private ArrayList<String> instructions=new ArrayList<String>();
    private ArrayList<String> numbers=new ArrayList<String>();

    public InfoRecyclerAdapter(Activity act){
        this.activity=act;
        mLayoutInflater= LayoutInflater.from(act);
    }
    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = mLayoutInflater.inflate(R.layout.instruction_layout,parent,false);

        for(int i=0;i<9;i++){
            numbers.add(String.valueOf(i+1)+". ");
        }
        instructions.add("Go to Subjects section, add subjects using the Floating '+' button.");
        instructions.add("Now, in the navigation bar, hit on time table section.");
        instructions.add("Once here, use the floating '+' button to set a lecture of a subject on that day.");
        instructions.add("Prepare the timetable for the week in a similar fashion.");
        instructions.add("Now, you're set, record attended and missed lectures by hitting the green and red number buttons respectively and" +
                " do a long press on the respective buttons to reduce count by 1.");
        instructions.add("Click on a subject in Subjects section to view logs of attendance.");
        instructions.add("Long press in Subjects section on any subject to get more options.");
        instructions.add("Definitely the time tables and subjects change over time; long press on a lecture to see more options and at the top of the app, you have a master reset button.");
        Holder holder=new Holder(item);
        return holder;
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        String link="<a href='http://goo.gl/forms/j0gIXsL2mX'>here</a>";

        if(position==0){
            holder.number.setVisibility(View.INVISIBLE);
            holder.instruction.setVisibility(View.INVISIBLE);
            holder.welcome.setVisibility(View.VISIBLE);
            holder.intro.setVisibility(View.VISIBLE);
        } else{
            holder.number.setVisibility(View.VISIBLE);
            holder.instruction.setVisibility(View.VISIBLE);
            holder.welcome.setVisibility(View.INVISIBLE);
            holder.intro.setVisibility(View.INVISIBLE);
            if(position==9){
                String string="<br/>developed by <i>Pavan R Chhatpar</i><br/><p>pavanchhatpar@gmail.com</p>";
                holder.instruction.setText(Html.fromHtml(string));
                holder.number.setText("  ");

            } /*else if (position == 10) {
                holder.number.setText("10. ");
                holder.instruction.setClickable(true);
                holder.instruction.setMovementMethod(LinkMovementMethod.getInstance());
                holder.instruction.setText("For future updates to be sent to you, submit your email ID ");
                holder.instruction.append(Html.fromHtml(link));
                holder.instruction.append(". Dont worry, you wont be sent any spam");

            }*/ else {
                    holder.number.setText(numbers.get(position-1));
                    holder.instruction.setText(instructions.get(position-1));

                }
            }
        }



    @Override
    public int getItemCount() {
        return 10;
    }

    public static class Holder extends RecyclerView.ViewHolder{
        TextView number;
        TextView instruction ;
        TextView welcome;
        TextView intro;
        CardView cardView;
        public Holder(View itemView) {
            super(itemView);
             number =(TextView)itemView.findViewById(R.id.infoNumber);
             instruction =(TextView)itemView.findViewById(R.id.instruction);
             welcome=(TextView)itemView.findViewById(R.id.WelcomeText);
             intro=(TextView)itemView.findViewById(R.id.introText);
            cardView=(CardView)itemView.findViewById(R.id.infoCard);
        }
    }
}
