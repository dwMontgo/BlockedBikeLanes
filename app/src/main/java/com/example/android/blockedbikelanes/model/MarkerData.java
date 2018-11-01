package com.example.android.blockedbikelanes.model;


import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MarkerData {

    public String comment;
    public String time;
    public String date;
    public String address;
    public Double latitude;
    public Double longitude;
    public String url;


    public MarkerData() {
    }

    public MarkerData(String comment, String time, String date, String address, LatLng latLng, String url) {
        this.comment = comment;
        this.time = time;
        this.date = date;
        this.address = address;
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.url = url;

    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

//    public String getLocation() {
//        return location;
//    }
//
//    public void setLocation(String location) {
//        this.location = location;
//    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LatLng toLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}

