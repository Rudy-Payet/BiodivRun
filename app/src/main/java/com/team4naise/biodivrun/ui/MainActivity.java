package com.team4naise.biodivrun.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.team4naise.biodivrun.R;

public class MainActivity extends AppCompatActivity {

    private VideoView videoBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Insets appliqués au content_container (pas au main) pour que la
        // vidéo de fond continue d'occuper toute la surface, status bar incluse.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.content_container), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        setupBackgroundVideo();//pour setup le bond d'écran
    }

    private void setupBackgroundVideo() {
        videoBackground = findViewById(R.id.video_background);

        // La vidéo doit être placée dans res/raw/paysages_reunion.mp4
        Uri uri = Uri.parse("android.resource://" + getPackageName()
                + "/" + R.raw.paysages_reunion);
        videoBackground.setVideoURI(uri);

        videoBackground.setOnPreparedListener(mp -> {
            // Recadrage type "centerCrop" : on calcule la taille à donner
            // à la VideoView pour qu'elle remplisse totalement l'écran,
            // quitte à déborder sur un axe (le FrameLayout parent rognera).
            scaleVideoToFillScreen(mp.getVideoWidth(), mp.getVideoHeight());

            mp.setLooping(true);    // boucle infinie
            mp.setVolume(0f, 0f);   // pas de son
            videoBackground.start();
        });

        // Si la vidéo plante (fichier manquant, format non supporté…),
        // on ne fait rien : le voile sombre reste visible et le contenu
        // blanc est lisible sur le fond noir du FrameLayout.
        videoBackground.setOnErrorListener((mp, what, extra) -> true);
    }

    /**
     * Redimensionne la VideoView pour qu'elle couvre tout l'écran sans
     * laisser de bandes vides. La vidéo dépasse sur un axe, l'excès est
     * rogné par le parent FrameLayout (clipChildren).
     */
    private void scaleVideoToFillScreen(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) return;

        int screenWidth = videoBackground.getRootView().getWidth();
        int screenHeight = videoBackground.getRootView().getHeight();
        if (screenWidth == 0 || screenHeight == 0) return;

        float videoAspect = (float) videoWidth / videoHeight;
        float screenAspect = (float) screenWidth / screenHeight;

        int newWidth, newHeight;
        if (videoAspect > screenAspect) {
            // Vidéo plus large que l'écran : on cale sur la hauteur,
            // la vidéo déborde à gauche et à droite.
            newHeight = screenHeight;
            newWidth = (int) (screenHeight * videoAspect);
        } else {
            // Vidéo plus haute (ou même ratio) : on cale sur la largeur,
            // la vidéo déborde en haut et en bas.
            newWidth = screenWidth;
            newHeight = (int) (screenWidth / videoAspect);
        }

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) videoBackground.getLayoutParams();
        params.width = newWidth;
        params.height = newHeight;
        params.gravity = Gravity.CENTER;
        videoBackground.setLayoutParams(params);
    }

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
    protected void onDestroy() {
        super.onDestroy();
        if (videoBackground != null) {
            videoBackground.stopPlayback();
        }
    }
}