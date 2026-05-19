package com.team4naise.biodivrun.data;

public class Zone {
    private int id_zone, rayon;
    private String nomZone;
    private double latMin, latMax, lonMin, lonMax;

    public Zone( int id, String nomZ, int ray, double latMin, double latMax, double lonMin, double lonMax){
        this.id_zone = id;
        this.nomZone = nomZ;
        this.rayon = ray;
        this.latMin = latMin;
        this.latMax = latMax;
        this.lonMin = lonMin;
        this.lonMax = lonMax;
    }

    public int getId(){return id_zone;}
    public String getNomZ(){return nomZone;}

    public int getRayon() {return rayon;}

    public double getLatMin() {return latMin;}

    public double getLatMax() {return latMax;}

    public double getLonMin() {return lonMin;}

    public double getLonMax() {return lonMax;}
}
