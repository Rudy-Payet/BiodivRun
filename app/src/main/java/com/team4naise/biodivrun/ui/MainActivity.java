package com.team4naise.biodivrun.ui;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.VideoView;
import androidx.core.splashscreen.SplashScreen;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.slider.Slider;
import com.team4naise.biodivrun.R;
import com.team4naise.biodivrun.data.Espece;
import com.team4naise.biodivrun.database.BiodivDBAdapter;
import com.team4naise.biodivrun.services.Location;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private VideoView videoBackground;
    private Location location;
    private BiodivDBAdapter db;
    private View scanLoader;//temps chargement

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Splash système (très bref, sans icône visible grâce à transparent_icon)
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Splash custom (logo entier) : visible 1.5s puis fondu
        View launchSplash = findViewById(R.id.launch_splash);
        launchSplash.postDelayed(() -> {
            launchSplash.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> launchSplash.setVisibility(View.GONE))
                    .start();
        }, 1500);
        // Référence à l'overlay de chargement (masqué au démarrage)
        scanLoader = findViewById(R.id.scan_loader);
        // le fond est sombre atm, on voit mal les icones du haut ça
        // c'est pour forcer le fond blanc
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.content_container), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // Vidéo fond
        setupBackgroundVideo();

        // DB
        db = new BiodivDBAdapter(this);
        db.open();
        //  Location + callback
        location = new Location(this);
        location.setOnZoneCalculeeListener((latMin, latMax, lonMin, lonMax) -> {
            ArrayList<Espece> especes = db.getEspecesParIntersectionSpatiale(
                    latMin, latMax, lonMin, lonMax);
            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra("especes", especes);
            intent.putExtra("latMin", latMin);
            intent.putExtra("latMax", latMax);
            intent.putExtra("lonMin", lonMin);
            intent.putExtra("lonMax", lonMax);
            startActivity(intent);
        });

        //  Slider
        setupSlider();

        //  Bouton scan
        View btnScan = findViewById(R.id.btn_scan);
        if (btnScan != null) {
            findViewById(R.id.btn_scan).setOnClickListener(v -> {
                // Ce message apparaîtra en bas de l'écran pour confirmer le clic
                showScanLoader();

                // On lance la recherche
                location.demanderPermission();
            });
        }
    }

    // ============================================================
    // SLIDER
    // ============================================================
    private void setupSlider() {
        Slider slider = findViewById(R.id.slider_radius);
        if (slider == null) return;

        android.widget.TextView tvRadiusValue = findViewById(R.id.tv_radius_value);

        // Affiche la valeur initiale au démarrage
        if (tvRadiusValue != null) {
            tvRadiusValue.setText(formatRadius(slider.getValue()));
        }

        slider.addOnChangeListener((s, value, fromUser) -> {
            // Met à jour l'affichage à chaque changement
            if (tvRadiusValue != null) {
                tvRadiusValue.setText(formatRadius(value));
            }
            if (fromUser) {
                location.setRayonKm(value);
            }
        });
    }

    /** Formate le rayon : "2,5 km" ou "3 km" si entier. */
    private String formatRadius(float value) {
        if (value == (int) value) {
            return ((int) value) + " km";
        } else {
            return String.format(java.util.Locale.FRENCH, "%.1f km", value);
        }
    }

    // ============================================================
    // LOADER (overlay pendant le scan)
    // ============================================================
    private void showScanLoader() {
        if (scanLoader == null) return;
        scanLoader.setVisibility(View.VISIBLE);
        scanLoader.setAlpha(0f);
        scanLoader.animate().alpha(1f).setDuration(200).start();
    }

    private void hideScanLoader() {
        if (scanLoader == null || scanLoader.getVisibility() == View.GONE) return;
        scanLoader.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> scanLoader.setVisibility(View.GONE))
                .start();
    }

    // ============================================================
    // VIDÉO FOND
    // ============================================================
    private void setupBackgroundVideo() {
        videoBackground = findViewById(R.id.video_background);
        if (videoBackground == null) return;

        Uri uri = Uri.parse("android.resource://" + getPackageName()
                + "/" + R.raw.paysages_reunion);
        videoBackground.setVideoURI(uri);

        videoBackground.setOnPreparedListener(mp -> {
            scaleVideoToFillScreen(mp.getVideoWidth(), mp.getVideoHeight());
            mp.setLooping(true);
            mp.setVolume(0f, 0f);
            videoBackground.start();
        });

        videoBackground.setOnErrorListener((mp, what, extra) -> true);
    }

    private void scaleVideoToFillScreen(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) return;

        int screenWidth  = videoBackground.getRootView().getWidth();
        int screenHeight = videoBackground.getRootView().getHeight();
        if (screenWidth == 0 || screenHeight == 0) return;

        float videoAspect  = (float) videoWidth / videoHeight;
        float screenAspect = (float) screenWidth / screenHeight;

        int newWidth, newHeight;
        if (videoAspect > screenAspect) {
            newHeight = screenHeight;
            newWidth  = (int) (screenHeight * videoAspect);
        } else {
            newWidth  = screenWidth;
            newHeight = (int) (screenWidth / videoAspect);
        }

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) videoBackground.getLayoutParams();
        params.width   = newWidth;
        params.height  = newHeight;
        params.gravity = Gravity.CENTER;
        videoBackground.setLayoutParams(params);
    }

    // ============================================================
    // CYCLE DE VIE
    // ============================================================
    @Override
    protected void onResume() {
        super.onResume();
        if (videoBackground != null && !videoBackground.isPlaying()) {
            videoBackground.start();
        }
        hideScanLoader();//cache le loader
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoBackground != null && videoBackground.isPlaying()) {
            videoBackground.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (location != null) location.stop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoBackground != null) videoBackground.stopPlayback();
        if (db != null) db.close();
    }
}