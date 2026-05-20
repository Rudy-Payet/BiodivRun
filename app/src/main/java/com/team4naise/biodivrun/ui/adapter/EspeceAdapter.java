package com.team4naise.biodivrun.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.team4naise.biodivrun.R;
import com.team4naise.biodivrun.data.Espece;
import java.util.ArrayList;

public class EspeceAdapter extends RecyclerView.Adapter<EspeceAdapter.EspeceViewHolder> {

    private final Context context;
    private final ArrayList<Espece> values;
    // Interface pour gérer le clic
    public interface OnItemClickListener { void onItemClick(Espece espece); }
    private final OnItemClickListener listener;

    public EspeceAdapter(Context context, ArrayList<Espece> values, OnItemClickListener listener){
        this.context = context;
        this.values = values;
        this.listener = listener;
    }

    @Override
    public EspeceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cellView = inflater.inflate(R.layout.item_card, parent, false);
        return new EspeceViewHolder(cellView);
    }

    @Override
    public void onBindViewHolder(EspeceViewHolder holder, int position) {

        Espece espece = values.get(position);

        // gérer le cas ou les espèces n'ont pas de nom commun
        String nom_commun = espece.getNom();
        String nom_commun_afficher = nom_commun ;
        if (nom_commun.equals("")) {
            nom_commun_afficher = espece.getNomSc();
        }

        holder.tvTitle.setText(nom_commun_afficher); // Nom commun
        String nomCommun = espece.getNom();
        if (nomCommun == null || nomCommun.isEmpty()) {
            // Pas de nom commun : on utilise le nom scientifique en titre, et on cache le sous-titre
            holder.tvTitle.setText(espece.getNomSc());
            holder.tvSubtitle.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setText(nomCommun);
            holder.tvSubtitle.setVisibility(View.VISIBLE);
            holder.tvSubtitle.setText(espece.getNomSc());
        }
        holder.tvBadge.setText(espece.getUicnLabel());// badge uicn labelle complet
        holder.tvBadge.setBackgroundTintList( //badge toujours il appelle la couleur
                ContextCompat.getColorStateList(context, espece.getUicnColor()));
        holder.tvBadge.setTextColor(
                ContextCompat.getColor(context, espece.getUicnTextColor()));  //

        // Retrait du .jpg pour correspondre au nom dans 'drawable'
        // Android Studio retire automatiquement l'extension à la compilation
        String nomImageBase = espece.getImagePath().replace(".jpg", "");
        int resID = context.getResources().getIdentifier(nomImageBase, "drawable", context.getPackageName());
        if(resID != 0) {
            holder.ivThumbnail.setImageResource(resID);
        }

        // Gestion du clic
        holder.itemView.setOnClickListener(v -> listener.onItemClick(espece));
    }

    @Override
    public int getItemCount() { return values.size(); }

    public static class EspeceViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvBadge;
        ImageView ivThumbnail;

        public EspeceViewHolder(View cellView) {
            super(cellView);
            tvTitle = cellView.findViewById(R.id.tv_title);
            tvSubtitle = cellView.findViewById(R.id.tv_subtitle);
            tvBadge = cellView.findViewById(R.id.tv_badge);
            ivThumbnail = cellView.findViewById(R.id.iv_thumbnail);
        }
    }
}