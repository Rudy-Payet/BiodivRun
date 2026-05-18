package com.team4naise.biodivrun.data;

import androidx.annotation.NonNull;

public class Faune {
    String nom, statut;
    Zone zone;

    public Faune(String nom, String statut, Zone zone) {
        this.nom = nom;
        this.statut = statut;
        this.zone = zone;
    }

    public void setNom(String nom){this.nom = nom;}
    public void setStatut(String statut){this.statut = statut;}
    public void setZone(Zone zone){this.zone = zone;}
    public String getNom(){return nom;}
    public String getStatut(){return statut;}
    public Zone getZone(){return zone;}
}