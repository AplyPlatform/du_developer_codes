package io.droneplay.droneplaymission.model;

public class FlightRecordItem {
    public EtcItem etc;
    public String dsec;
    public String lat;
    public String lng;
    public float alt;
    public int act;
    public int speed;
    public int actparam;
    public long dtimestamp;

    public FlightRecordItem() {
        this.etc = new EtcItem();
    }
}