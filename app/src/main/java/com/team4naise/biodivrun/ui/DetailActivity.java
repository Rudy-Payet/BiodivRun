package com.team4naise.biodivrun.ui;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.team4naise.biodivrun.R;
import com.team4naise.biodivrun.data.Espece;


public class DetailActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // ✅ Toolbar — flèche retour
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Récupère l'espèce envoyée
        Espece espece = (Espece) getIntent().getSerializableExtra("espece");
        if(espece == null){
            finish();
            return;
        }
        ImageView ivHero = findViewById(R.id.iv_hero);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvSubtitle = findViewById(R.id.tv_subtitle);
        TextView tvBadgeStatus = findViewById(R.id.tv_badge_status);
        TextView tvBadgeUicn = findViewById(R.id.tv_badge_uicn);
        TextView tvSectionTitle = findViewById(R.id.tv_section_title);
        TextView tvSectionBody = findViewById(R.id.tv_section_body);
        TextView tvSectionEnv = findViewById(R.id.tv_section_env);
        TextView tvSectionHabitat = findViewById(R.id.tv_section_habitat);

        String nomImage = espece.getImagePath().replace(".jpg", "");
        // Trouve l'id de l'image dans drawable
        int resId = getResources().getIdentifier(nomImage, "drawable", getPackageName());

        // Si pas de nom commun, on utilise le nom scientifique en titre
        String nomCommun = espece.getNom();
        if (nomCommun == null || nomCommun.isEmpty()) {
            tvTitle.setText(espece.getNomSc());
            tvSubtitle.setVisibility(View.GONE);  // pas de sous-titre redondant
        } else {
            tvTitle.setText(nomCommun);
            tvSubtitle.setText(espece.getNomSc());
        }


        if (resId != 0) {
            ivHero.setImageResource(resId);
        } else {
            ivHero.setImageResource(R.drawable.ic_launcher_background);
        }
        if (espece.getOrigine() != null && !espece.getOrigine().isEmpty()) {
            tvBadgeStatus.setText(espece.getOrigine());
            tvBadgeStatus.setVisibility(View.VISIBLE);
        } else {
            tvBadgeStatus.setVisibility(View.GONE);
        }
        if (espece.getUICN() != null && !espece.getUICN().isEmpty()) {
            tvBadgeUicn.setText(espece.getUicnLabel());           // "Préoccupation mineure (LC)"
            tvBadgeUicn.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, espece.getUicnColor()));
            tvBadgeUicn.setTextColor(
                    ContextCompat.getColor(this, espece.getUicnTextColor()));
            tvBadgeUicn.setVisibility(View.VISIBLE);
        } else {
            tvBadgeUicn.setVisibility(View.GONE);
        }

        //section du bas
        tvSectionTitle.setText("Description");
        if (espece.getIdentification() != null && !espece.getIdentification().isEmpty()) {
            tvSectionBody.setText(espece.getIdentification());
        } else {
            tvSectionBody.setText("Pas de description.");
        }
        tvSectionEnv.setText("Environnement");
        if (espece.getHabitat() != null && !espece.getHabitat().isEmpty()) {
            tvSectionHabitat.setText(espece.getHabitat());
        } else {
            tvSectionHabitat.setText("Pas d'information sur l'environnement.");
        }
    }
}
