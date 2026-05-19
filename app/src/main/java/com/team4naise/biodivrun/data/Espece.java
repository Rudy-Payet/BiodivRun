package com.team4naise.biodivrun.data;

public class Espece {
    private int id_espece;
    private String nom, nomSc, taille, uicn, origine, identification, habitat, imagePath;
    public Espece(String nom, String uicn, Zone zone) {
        this.nom = nom;
        this.uicn = uicn;
        this.origine = origine;
    }

    public void setNom(String nom){this.nom = nom;}
    public void setTaille(String t){this.taille = t;}
    public void setOrigine(String origine){this.origine = origine;}
    public void setUICN(String uicn){this.uicn = uicn;}
    public String getTaille(){return taille;}
    public String getNom(){return nom;}
    public String getUICN(){return uicn;}
    public String getOrigine(){return origine;}
    public String getIdentification(){return identification;}
    public String getImagePath(){return imagePath;}
}