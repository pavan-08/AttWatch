package com.attwatch.entity;

/**
 * Created by Pavan on 2/24/2016.
 */
public class Attendance {
    int id;
    int status;
    String timestamp;
    TimeTable lecture;
    Subjects subject;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public TimeTable getLecture() {
        return lecture;
    }

    public void setLecture(TimeTable lecture) {
        this.lecture = lecture;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Subjects getSubject() {
        return subject;
    }

    public void setSubject(Subjects subject) {
        this.subject = subject;
    }

}
