package com.team4naise.biodivrun.data;

import java.io.Serializable;

public class Espece implements Serializable {
    private String nom, nomSc, uicn, origine, identification, habitat, imageName;
    public Espece(String nom, String nomSc, String uicn, String origine, String ident, String habitat, String imageN) {
        this.nom = nom;
        this.nomSc = nomSc;
        this.uicn = uicn;
        this.origine = origine;
        this.identification = ident;
        this.habitat = habitat;
        this.imageName = imageN;
    }

    public void setNom(String nom){this.nom = nom;}
    public void setNomSc(String nomSc){this.nomSc = nomSc;}
    public void setOrigine(String origine){this.origine = origine;}
    public void setUICN(String uicn){this.uicn = uicn;}
    public String getNom(){return nom;}
    public String getNomSc(){return nomSc;}
    public String getUICN(){return uicn;}
    public String getOrigine(){return origine;}
    public String getIdentification(){return identification;}

    public String getHabitat() {return habitat;}

    public String getImagePath(){return imageName;}
}