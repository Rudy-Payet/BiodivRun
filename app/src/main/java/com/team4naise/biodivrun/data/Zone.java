package com.team4naise.biodivrun.data;

public class Zone {
    int id;
    long hautDroit, hautGauche, basDroit, basGauche;

    public Zone( int id, long hautD, long hautG, long basD, long basG){
        this.id = id;
        this.hautDroit = hautD;
        this.hautGauche = hautG;
        this.basDroit = basD;
        this.basGauche = basG;
    }

    public int getId(){return id;}

    public long getHautDroit() {return hautDroit;}

    public long getHautGauche() {return hautGauche;}

    public long getBasDroit() {return basDroit;}

    public long getBasGauche() {return basGauche;}
}
