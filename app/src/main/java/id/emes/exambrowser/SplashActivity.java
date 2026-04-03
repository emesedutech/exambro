package id.emes.exambrowser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2800; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full immersive
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_splash);

        // ── Terapkan background image jika ada ─────────────────────────────
        applySplashBackground();

        // ── Terapkan logo transparan jika app_logo.png tersedia ────────────
        applyAppLogo(R.id.splashLogo);

        // ── Referensi view ──────────────────────────────────────────────────
        ImageView imgLogo     = findViewById(R.id.splashLogo);
        TextView  tvAppName   = findViewById(R.id.splashAppName);
        TextView  tvTagline   = findViewById(R.id.splashTagline);
        View      divider     = findViewById(R.id.splashDivider);
        ProgressBar progress  = findViewById(R.id.splashProgress);
        TextView  tvVersion   = findViewById(R.id.splashVersion);

        // ── Animasi masuk ───────────────────────────────────────────────────
        // Mulai semua tersembunyi
        imgLogo.setAlpha(0f);
        imgLogo.setScaleX(0.3f);
        imgLogo.setScaleY(0.3f);
        tvAppName.setAlpha(0f);
        tvAppName.setTranslationY(30f);
        tvTagline.setAlpha(0f);
        tvTagline.setTranslationY(20f);
        divider.setAlpha(0f);
        divider.setScaleX(0f);
        progress.setAlpha(0f);
        tvVersion.setAlpha(0f);

        // Logo: scale + fade in
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
            ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f).setDuration(600),
            ObjectAnimator.ofFloat(imgLogo, "scaleX", 0.3f, 1f).setDuration(700),
            ObjectAnimator.ofFloat(imgLogo, "scaleY", 0.3f, 1f).setDuration(700)
        );
        logoAnim.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnim.setStartDelay(200);

        // App name: slide up + fade in
        AnimatorSet nameAnim = new AnimatorSet();
        nameAnim.playTogether(
            ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).setDuration(500),
            ObjectAnimator.ofFloat(tvAppName, "translationY", 30f, 0f).setDuration(500)
        );
        nameAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        nameAnim.setStartDelay(700);

        // Tagline: slide up + fade in
        AnimatorSet taglineAnim = new AnimatorSet();
        taglineAnim.playTogether(
            ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f).setDuration(400),
            ObjectAnimator.ofFloat(tvTagline, "translationY", 20f, 0f).setDuration(400)
        );
        taglineAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        taglineAnim.setStartDelay(900);

        // Divider: scale in
        AnimatorSet dividerAnim = new AnimatorSet();
        dividerAnim.playTogether(
            ObjectAnimator.ofFloat(divider, "alpha", 0f, 1f).setDuration(400),
            ObjectAnimator.ofFloat(divider, "scaleX", 0f, 1f).setDuration(400)
        );
        dividerAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        dividerAnim.setStartDelay(1100);

        // Progress + version fade in
        AnimatorSet bottomAnim = new AnimatorSet();
        bottomAnim.playTogether(
            ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f).setDuration(400),
            ObjectAnimator.ofFloat(tvVersion, "alpha", 0f, 0.6f).setDuration(400)
        );
        bottomAnim.setStartDelay(1300);

        // Play all
        AnimatorSet fullAnim = new AnimatorSet();
        fullAnim.playTogether(logoAnim, nameAnim, taglineAnim, dividerAnim, bottomAnim);
        fullAnim.start();

        // ── Lanjut ke MainActivity setelah delay ────────────────────────────
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Fade out keseluruhan
            View root = findViewById(R.id.splashRoot);
            root.animate()
                .alpha(0f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                });
        }, SPLASH_DURATION);
    }

    /**
     * Terapkan app_logo.png (transparan) ke ImageView jika tersedia.
     * Fallback ke ic_launcher jika tidak ada logo user yang di-upload.
     */
    private void applyAppLogo(int viewId) {
        try {
            int resId = getResources().getIdentifier("app_logo", "drawable", getPackageName());
            if (resId == 0) return; // Tidak ada logo → tetap pakai ic_launcher
            android.graphics.drawable.Drawable d = getResources().getDrawable(resId, getTheme());
            if (d == null) return;
            ImageView img = findViewById(viewId);
            img.setImageDrawable(d);
            img.setBackground(null);   // hapus frame/background apapun
            img.setPadding(0, 0, 0, 0);
            img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } catch (Exception e) {
            // Fallback ke ic_launcher
        }
    }

    /**
     * Terapkan gambar background splash jika splash_bg.png tersedia di drawable.
     * Jika tidak ada, layar menggunakan warna solid colorHeaderBg dari colors.xml.
     */
    private void applySplashBackground() {
        try {
            int resId = getResources().getIdentifier("splash_bg", "drawable", getPackageName());
            if (resId == 0) return;

            Drawable d = getResources().getDrawable(resId, getTheme());
            if (d == null) return;

            ImageView imgBg  = findViewById(R.id.splashBgImage);
            View      overlay = findViewById(R.id.splashOverlay);

            imgBg.setImageDrawable(d);
            imgBg.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // Gagal — pakai warna solid
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    public void onBackPressed() {
        // Blokir back di splash
    }
}
