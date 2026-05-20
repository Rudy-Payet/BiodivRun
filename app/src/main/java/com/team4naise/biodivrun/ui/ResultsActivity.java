package com.team4naise.biodivrun.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import org.osmdroid.util.BoundingBox;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.team4naise.biodivrun.R;
import com.team4naise.biodivrun.data.Espece;
import com.team4naise.biodivrun.ui.adapter.EspeceAdapter;



import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Random;

public class ResultsActivity extends AppCompatActivity {

    // Bounding box approximatif de la terre de La Réunion (pour clamp anti-mer)
    private static final double LAND_LAT_MIN = -21.36;
    private static final double LAND_LAT_MAX = -20.88;
    private static final double LAND_LON_MIN = 55.23;
    private static final double LAND_LON_MAX = 55.81;

    private RecyclerView recyclerView;
    private MapView mapView;
    private View layoutEmpty;
    private ArrayList<Espece> especes;
    private MenuItem viewToggleItem;
    private boolean isMapMode = false;
    private boolean mapInitialized = false;

    // Zone de scan reçue de MainActivity (sert à positionner les marqueurs)
    private double scanLatMin, scanLatMax, scanLonMin, scanLonMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(getPackageName());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_results);
        viewToggleItem = toolbar.getMenu().findItem(R.id.action_toggle_view);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_toggle_view) {
                toggleViewMode();
                return true;
            }
            return false;
        });

        // Récupère la liste + la zone scannée depuis MainActivity
        especes = (ArrayList<Espece>) getIntent().getSerializableExtra("especes");
        scanLatMin = getIntent().getDoubleExtra("latMin", LAND_LAT_MIN);
        scanLatMax = getIntent().getDoubleExtra("latMax", LAND_LAT_MAX);
        scanLonMin = getIntent().getDoubleExtra("lonMin", LAND_LON_MIN);
        scanLonMax = getIntent().getDoubleExtra("lonMax", LAND_LON_MAX);

        recyclerView = findViewById(R.id.rv_items);
        mapView = findViewById(R.id.map);
        layoutEmpty = findViewById(R.id.layout_empty);

        if (especes == null || especes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            mapView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            viewToggleItem.setVisible(false);
            return;
        }

        // Met à jour le titre de la toolbar avec le nombre de résultats
        String title = getResources().getQuantityString(
                R.plurals.results_title_count, especes.size(), especes.size());
        toolbar.setTitle(title);

        setupList();
    }

    private void setupList() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        EspeceAdapter adapter = new EspeceAdapter(this, especes, espece -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("espece", espece);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void toggleViewMode() {
        isMapMode = !isMapMode;
        if (isMapMode) {
            recyclerView.setVisibility(View.GONE);
            mapView.setVisibility(View.VISIBLE);
            viewToggleItem.setIcon(R.drawable.ic_list);
            viewToggleItem.setTitle(R.string.menu_view_list);
            if (!mapInitialized) {
                setupMap();
                mapInitialized = true;
            }
        } else {
            mapView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            viewToggleItem.setIcon(R.drawable.ic_map);
            viewToggleItem.setTitle(R.string.menu_view_map);
        }
    }

    /**
     * Configure la carte. Centrée sur la zone scannée, marqueurs placés
     * à l'intérieur de cette zone (l'espèce a été trouvée ici, on l'affiche ici).
     */
    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        float density = getResources().getDisplayMetrics().density;
        int strokeWidthPx = (int) (4 * density);

        // Bornes : on part de la zone scannée, et on les étend pour englober
        // tous les marqueurs (au cas où certains débordent à cause du MIN_SPREAD)
        double minLat = scanLatMin, maxLat = scanLatMax;
        double minLon = scanLonMin, maxLon = scanLonMax;

        for (Espece espece : especes) {
            GeoPoint position = positionForSpecies(espece.getNomSc());

            // Étend les bornes pour inclure ce marqueur
            minLat = Math.min(minLat, position.getLatitude());
            maxLat = Math.max(maxLat, position.getLatitude());
            minLon = Math.min(minLon, position.getLongitude());
            maxLon = Math.max(maxLon, position.getLongitude());

            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            String nomAffiche = espece.getNom() == null || espece.getNom().isEmpty()
                    ? espece.getNomSc() : espece.getNom();
            marker.setTitle(nomAffiche);
            marker.setSnippet(espece.getUicnLabel());

            marker.setIcon(createPhotoMarker(espece.getImagePath(), espece.getUicnColor()));

            marker.setOnMarkerClickListener((m, mv) -> {
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("espece", espece);
                startActivity(intent);
                return true;
            });

            mapView.getOverlays().add(marker);
        }

        // Zoom pour englober la zone scannée + tous les marqueurs
        // (post = on attend que la MapView soit mesurée, sinon zoomToBoundingBox plante)
        final double fMinLat = minLat, fMaxLat = maxLat;
        final double fMinLon = minLon, fMaxLon = maxLon;
        mapView.post(() -> {
            BoundingBox bbox = new BoundingBox(fMaxLat, fMaxLon, fMinLat, fMinLon);
            mapView.zoomToBoundingBox(bbox, false, 100);  // 100px de marge
        });

        mapView.invalidate();
    }

    /**
     * Position d'une espèce dans la zone scannée par l'utilisateur.
     * Le décalage est déterministe (basé sur le nom) : la même espèce sera
     * toujours au même endroit pour un scan donné.
     */
    private GeoPoint positionForSpecies(String nomSc) {
        double centerLat = (scanLatMin + scanLatMax) / 2.0;
        double centerLon = (scanLonMin + scanLonMax) / 2.0;

        Random rand = new Random(nomSc.hashCode());

        // Minimum de dispersion (~1 km) pour éviter que les marqueurs
        // se superposent quand la zone de scan est petite ou nulle
        final double MIN_SPREAD = 0.01;  // ~1.1 km en latitude
        double latRange = Math.max(MIN_SPREAD, (scanLatMax - scanLatMin) * 0.7);
        double lonRange = Math.max(MIN_SPREAD, (scanLonMax - scanLonMin) * 0.7);

        double latOffset = (rand.nextDouble() - 0.5) * latRange;
        double lonOffset = (rand.nextDouble() - 0.5) * lonRange;

        double finalLat = centerLat + latOffset;
        double finalLon = centerLon + lonOffset;

        // Clamp sur la terre ferme
        finalLat = Math.max(LAND_LAT_MIN, Math.min(LAND_LAT_MAX, finalLat));
        finalLon = Math.max(LAND_LON_MIN, Math.min(LAND_LON_MAX, finalLon));

        return new GeoPoint(finalLat, finalLon);
    }


    /**
     * Crée un marqueur rond contenant la photo de l'espèce, entouré
     * d'une bordure colorée selon le statut UICN.
     *
     * @param imageName Nom du drawable (ex: "becasseau_tachete")
     * @param uicnColor Couleur de la bordure (ressource @color/uicn_*)
     * @return Drawable prêt à être passé à Marker.setIcon()
     */
    private Drawable createPhotoMarker(String imageName, int uicnColor) {
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (48 * density);          // taille totale du marqueur (48dp)
        int strokePx = (int) (3 * density);          // épaisseur de la bordure
        int photoRadius = (sizePx / 2) - strokePx;   // rayon de la photo dedans

        // Récupère la photo (fallback ic_launcher si introuvable)
        int resId = getResources().getIdentifier(
                imageName.replace(".jpg", ""), "drawable", getPackageName());
        if (resId == 0) resId = R.mipmap.ic_launcher;
        Drawable photoDrawable = ContextCompat.getDrawable(this, resId);

        // Bitmap source de la photo (redimensionnée carrée)
        int photoSize = photoRadius * 2;
        Bitmap photoBitmap = Bitmap.createBitmap(photoSize, photoSize, Bitmap.Config.ARGB_8888);
        Canvas photoCanvas = new Canvas(photoBitmap);
        photoDrawable.setBounds(0, 0, photoSize, photoSize);
        photoDrawable.draw(photoCanvas);

        // Bitmap final = cercle photo + bordure colorée
        Bitmap output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 1. Dessine le cercle de bordure (rempli, fait office de fond)
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(ContextCompat.getColor(this, uicnColor));
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, borderPaint);

        // 2. Dessine la photo en rond par-dessus, centrée
        Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap circularPhoto = makeCircular(photoBitmap);
        canvas.drawBitmap(circularPhoto, strokePx, strokePx, photoPaint);

        return new BitmapDrawable(getResources(), output);
    }

    /** Recadre une bitmap carrée en cercle (transparence autour). */
    private Bitmap makeCircular(Bitmap source) {
        int size = source.getWidth();
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Dessine un cercle plein
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        // Mode "garder uniquement où c'est déjà dessiné"
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // Dessine la photo, sera coupée au cercle
        canvas.drawBitmap(source, new Rect(0, 0, size, size),
                new RectF(0, 0, size, size), paint);
        return output;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}