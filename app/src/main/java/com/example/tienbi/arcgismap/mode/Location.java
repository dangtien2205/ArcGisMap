package com.example.tienbi.arcgismap.mode;

import java.io.Serializable;

/**
 * Created by TienBi on 13/11/2016.
 */
public class Location implements Serializable {
    private int id_point;
    private String name_point;
    private int id_type;
    private String description;
    private float latitude;
    private float longtitude;
    private String state;

    public Location() {
    }

    public Location(int id_point, String name_point, int id_type, String description, float latitude, float longtitude, String state) {
        this.id_point = id_point;
        this.name_point = name_point;
        this.id_type = id_type;
        this.description = description;
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.state = state;
    }

    public int getId_point() {
        return id_point;
    }

    public String getName_point() {
        return name_point;
    }

    public int getId_type() {
        return id_type;
    }

    public String getDescription() {
        return description;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongtitude() {
        return longtitude;
    }

    public String getState() {
        return state;
    }
}
