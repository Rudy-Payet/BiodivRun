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
        View cellView = inflater.inflate(R.layout.item_espece, parent, false);
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
        holder.txtStatut.setText("Statut : " + statut);

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
        TextView txtStatut;
        ImageView image;

        public EspeceViewHolder(View cellView) {
            super(cellView);
            // Todo dans la vue item.xml (le cellView), il faut faire coincider ces
            //  noms. (Emerich)
            //  Ajouter en fonction des autre éléments à afficher
            txtNom = cellView.findViewById(R.id.txtNom);
            txtStatut = cellView.findViewById(R.id.txtStatut);
            image = cellView.findViewById(R.id.image);
        }
    }
}