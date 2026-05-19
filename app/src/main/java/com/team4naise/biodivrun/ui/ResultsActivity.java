package com.team4naise.biodivrun.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.team4naise.biodivrun.R;
import com.team4naise.biodivrun.data.Espece;
import com.team4naise.biodivrun.ui.adapter.EspeceAdapter;

import java.util.ArrayList;

public class ResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // ✅ Toolbar — flèche retour
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Récupère la liste depuis MainActivity
        ArrayList<Espece> especes = (ArrayList<Espece>) getIntent()
                .getSerializableExtra("especes");

        // ✅ État vide si liste nulle ou vide
        RecyclerView recyclerView = findViewById(R.id.rv_items); // ✅ rv_items
        View layoutEmpty          = findViewById(R.id.layout_empty);

        if (especes == null || especes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // Config RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Adapter avec clic → DetailActivity
        EspeceAdapter adapter = new EspeceAdapter(this, especes, espece -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("espece", espece);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }
}