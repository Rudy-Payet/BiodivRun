package com.team4naise.biodivrun.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
            startActivity(intent);
        });

        //  Slider
        setupSlider();

        //  Bouton scan
        View btnScan = findViewById(R.id.btn_scan);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> lancerScan());
        }
    }

    // ============================================================
    // SCAN
    // ============================================================
    private void lancerScan() {
        location.forcerCalculZone();
    }

    // ============================================================
    // SLIDER
    // ============================================================
    private void setupSlider() {
        Slider slider = findViewById(R.id.slider_radius);
        if (slider == null) return;
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                location.setRayonKm((int) value);
            }
        });
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
        if (db != null) db.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoBackground != null) videoBackground.stopPlayback();
    }
}