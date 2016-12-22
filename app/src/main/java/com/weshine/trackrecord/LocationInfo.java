package com.weshine.trackrecord;

/**
 * Created by denevrove on 2016/8/11.
 */
public class LocationInfo {

    private String id = "";    // 流水號
    private double latitude;   // 緯度
    private double longitude;  // 經度
    private int accuracy;      // 精確度
    private long datetime;     // 紀錄時間

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }
}
