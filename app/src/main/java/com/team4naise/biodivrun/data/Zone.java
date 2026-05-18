package com.team4naise.biodivrun.data;

public class Zone {
    int id, rayon;
    String nomZone;
    long latMin, latMax, lonMin, lonMax;

    public Zone( int id, String nomZ, int ray, long latMin, long latMax, long lonMin, long lonMax){
        this.id = id;
        this.nomZone = nomZ;
        this.rayon = ray;
        this.latMin = latMin;
        this.latMax = latMax;
        this.lonMin = lonMin;
        this.lonMax = lonMax;
    }

    public int getId(){return id;}
    public String getNomZ(){return nomZone;}

    public int getRayon() {return rayon;}

    public long getLatMin() {return latMin;}

    public long getLatMax() {return latMax;}

    public long getLonMin() {return lonMin;}

    public long getLonMax() {return lonMax;}
}
