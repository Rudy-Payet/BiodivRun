package com.team4naise.biodivrun.data;

public class Espece {
    String nom, taille, statut, description;
    Zone zone;

    public Espece(String nom, String statut, Zone zone) {
        this.nom = nom;
        this.statut = statut;
        this.zone = zone;
    }

    public void setNom(String nom){this.nom = nom;}
    public void setTaille(String t){this.taille = t;}
    public void setStatut(String statut){this.statut = statut;}
    public void setZone(Zone zone){this.zone = zone;}
    public String getTaille(){return taille;}
    public String getNom(){return nom;}
    public String getStatut(){return statut;}
    public Zone getZone(){return zone;}
}