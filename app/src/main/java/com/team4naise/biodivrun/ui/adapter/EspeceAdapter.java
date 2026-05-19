package com.team4naise.biodivrun.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.team4naise.biodivrun.R;
import com.team4naise.biodivrun.data.Espece;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class EspeceAdapter extends RecyclerView.Adapter<EspeceAdapter.EspeceViewHolder> {

    private final Context context;
    private final ArrayList<Espece> values;

    public EspeceAdapter(Context context, ArrayList<Espece> values){
        this.context = context;
        this.values = values;
    }

    @Override
    public EspeceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cellView = inflater.inflate(R.layout.item_card, parent, false);
        // TODO item_espece est le cellView, il faut adapter en fonction de ce
        //  que fait Emerich
        return new EspeceViewHolder(cellView);
    }

    @Override
    public void onBindViewHolder(EspeceViewHolder holder, int position) {
        Espece espece = values.get(position);

        String nom = espece.getNom();
        String statut = espece.getStatut();

        // Retrait du .jpg pour correspondre au nom dans 'drawable'
        // Android Studio retire automatiquement l'extension à la compilation
        String nomImageBase = espece.getImagePath().replace(".jpg", "");
        // TODO fonction et attribut à ajotuter à la classe Espece (Max)

        holder.txtNom.setText(nom);
        holder.txtBadge.setText(statut);// holder.txtStatut.setText("Statut : " + statut); ya pas besoin
        holder.txtAutreNom.setText("ze suit 1 non sientifik");  // vide pour l'instant
        // TODO Max : ajouter getScientificName() dans Espece, puis remplacer par :
        // holder.txtAutreNom.setText(espece.getScientificName()) ou qqch comme ça;

        // Récupération dynamique de l'image
        int resID = context.getResources().getIdentifier(nomImageBase, "drawable", context.getPackageName());
        if(resID != 0) {
            holder.image.setImageResource(resID);
        }
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    // Le ViewHolder qui remplace les findViewById à répétition
    // Améliore la fluidité de l'application quand on a beacoup d'éléments dans la
    // liste
    public static class EspeceViewHolder extends RecyclerView.ViewHolder {
        TextView txtNom;
        TextView txtAutreNom;
        TextView txtBadge;
        ImageView image;


        public EspeceViewHolder(View cellView) {
            super(cellView);
            // Todo dans la vue item.xml (le cellView), il faut faire coincider ces
            //  noms. (Emerich)
            //  Ajouter en fonction des autre éléments à afficher
            txtNom = cellView.findViewById(R.id.tv_title); //nom commun(péi)
            txtAutreNom= cellView.findViewById(R.id.tv_subtitle); //nom scientifique
            txtBadge = cellView.findViewById(R.id.tv_badge); //badge = "statue" du style protégée, en voie d'extinction....
            image = cellView.findViewById(R.id.iv_thumbnail); //image de profil de l'espèce
        }
    }
}