package com.team4naise.biodivrun.ui.adapter;

import android.annotation.SuppressLint;
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

    @SuppressLint("DiscouragedApi")
    @Override
    public void onBindViewHolder(EspeceViewHolder holder, int position) {
        Espece espece = values.get(position);

        holder.tvTitle.setText(espece.getNom()); // Nom commun
        holder.tvSubtitle.setText(espece.getNomSc()); // nom scientifique
        holder.tvBadge.setText(espece.getUICN());    // badge uicn

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