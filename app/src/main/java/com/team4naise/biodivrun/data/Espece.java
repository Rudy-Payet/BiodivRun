package com.team4naise.biodivrun.data;

import java.io.Serializable;
import com.team4naise.biodivrun.R;
public class Espece implements Serializable {
    private String nom, nomSc, uicn, origine, identification, habitat, imageName;
    public Espece(String nom, String nomSc, String origine,String uicn, String ident, String habitat, String imageN) {
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




    /** L'adapter est déjà pas mal surcharger, je vais mettre ça la, pour l'instant.
     * Retourne le libellé complet à afficher pour le code UICN.
     * Ex : "LC" -> "Préoccupation mineure (LC)"
     */
    public String getUicnLabel() {
        if (uicn == null) return "";
        switch (uicn) {
            case "EX": return "Éteint (EX)";
            case "EW": return "Éteint à l'état sauvage (EW)";
            case "CR": return "En danger critique (CR)";
            case "EN": return "En danger (EN)";
            case "VU": return "Vulnérable (VU)";
            case "NT": return "Quasi menacé (NT)";
            case "LC": return "Préoccupation mineure (LC)";
            case "DD": return "Données insuffisantes (DD)";
            case "NE": return "Non évalué (NE)";
            default:   return uicn;
        }
    }

    /**
     * Retourne la ressource couleur à appliquer au badge UICN.
     * À utiliser avec ContextCompat.getColor(ctx, espece.getUicnColor()).
     */
    public int getUicnColor() {
        if (uicn == null) return R.color.uicn_ne;
        switch (uicn) {
            case "EX": return R.color.uicn_ex;
            case "EW": return R.color.uicn_ew;
            case "CR": return R.color.uicn_cr;
            case "EN": return R.color.uicn_en;
            case "VU": return R.color.uicn_vu;
            case "NT": return R.color.uicn_nt;
            case "LC": return R.color.uicn_lc;
            case "DD": return R.color.uicn_dd;
            default:   return R.color.uicn_ne;
        }
    }

    /**
     * Retourne la couleur du TEXTE à utiliser sur le badge UICN
     * pour garantir la lisibilité (noir sur fond clair, blanc sur fond sombre).
     */
    public int getUicnTextColor() {
        if (uicn == null) return R.color.on_background;
        switch (uicn) {
            // Fonds clairs (jaune, vert-jaune, gris) -> texte noir
            case "VU": case "NT": case "DD": case "NE":
                return R.color.on_background;
            // Fonds sombres/vifs (noir, violet, rouge, orange, vert) -> texte blanc
            default:
                return android.R.color.white;
        }
    }



}