package com.attwatch.entity;

/**
 * Created by Pavan on 2/24/2016.
 */
public class Subjects {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    int id;
    String name;
    int limit;
}
