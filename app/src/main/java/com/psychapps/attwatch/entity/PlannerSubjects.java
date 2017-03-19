package com.psychapps.attwatch.entity;

import java.util.ArrayList;

/**
 * Created by Pavan on 6/17/2016.
 */
public class PlannerSubjects {
    int bp_id;
    ArrayList<Subjects> subjects;

    public int getBp_id() {
        return bp_id;
    }

    public void setBp_id(int bp_id) {
        this.bp_id = bp_id;
    }

    public ArrayList<Subjects> getSubjects() {
        return subjects;
    }

    public void setSubjects(ArrayList<Subjects> subjects) {
        this.subjects = subjects;
    }
}
