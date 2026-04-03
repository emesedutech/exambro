package id.emes.exambrowser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int QR_SCAN_REQUEST = 200;
    private EditText etUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        // ── Cek apakah ada header background image ────────────────────────
        applyHeaderBackground();

        // ── Terapkan logo transparan jika app_logo.png tersedia ────────────
        applyAppLogo(R.id.imgAppLogo);

        etUrl = findViewById(R.id.etUrl);
        Button btnStart        = findViewById(R.id.btnStart);
        LinearLayout btnScanQr = findViewById(R.id.btnScanQr);

        btnStart.setOnClickListener(v -> launchExam(etUrl.getText().toString().trim()));

        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanActivity.class);
            startActivityForResult(intent, QR_SCAN_REQUEST);
        });
    }

    /**
     * Jika file header_bg ada di drawable dan bukan placeholder warna,
     * tampilkan sebagai background gambar dengan overlay gelap agar teks tetap terbaca.
     */
    /**
     * Load logo secara dinamis:
     *  1. drawable/app_logo.png  → logo custom dari user
     *  2. fallback ic_launcher   → jika tidak ada app_logo
     */
    private void applyAppLogo(int viewId) {
        ImageView img = findViewById(viewId);
        if (img == null) return;
        // Hapus semua background agar PNG transparan tampil apa adanya
        img.setBackground(null);
        img.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        img.setPadding(0, 0, 0, 0);
        // Software rendering wajib agar alpha channel PNG tidak dirender hitam
        img.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
        // Load dengan ARGB_8888 agar alpha channel terjaga
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
        int resId = getResources().getIdentifier("app_logo", "drawable", getPackageName());
        if (resId != 0) {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeResource(getResources(), resId, opts);
            if (bmp != null) img.setImageBitmap(bmp);
        }
    }

    private void applyHeaderBackground() {
        try {
            int resId = getResources().getIdentifier("header_bg", "drawable", getPackageName());
            if (resId == 0) return;

            Drawable d = getResources().getDrawable(resId, getTheme());
            if (d == null) return;

            final ImageView  imgBg   = findViewById(R.id.imgHeaderBg);
            final View       overlay = findViewById(R.id.headerOverlay);
            final LinearLayout content = findViewById(R.id.headerContent);
            final FrameLayout frame  = findViewById(R.id.headerFrame);

            imgBg.setImageDrawable(d);
            imgBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgBg.setAdjustViewBounds(false);
            imgBg.setBackgroundColor(Color.TRANSPARENT);

            // Langsung hapus background headerContent sebelum layout — cegah flash biru
            content.setBackgroundColor(Color.TRANSPARENT);

            // Setelah layout selesai, cocokkan tinggi imgHeaderBg dengan headerContent
            frame.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        frame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int h = content.getHeight();
                        if (h > 0) {
                            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT, h);
                            imgBg.setLayoutParams(lp);
                            overlay.setLayoutParams(new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT, h));
                        }
                        imgBg.setVisibility(View.VISIBLE);
                        overlay.setVisibility(View.VISIBLE);
                        content.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            );

        } catch (Exception e) {
            // Gagal load — pakai warna solid default
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST && resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("scanned_url");
            if (url != null && !url.isEmpty()) {
                etUrl.setText(url);
                launchExam(url);
            }
        }
    }

    private void launchExam(String url) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Masukkan URL ujian terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        Intent intent = new Intent(this, ExamActivity.class);
        intent.putExtra("exam_url", url);
        startActivity(intent);
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
}
