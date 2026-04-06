package id.emes.exambrowser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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
    private Button btnStart;
    private LinearLayout btnScanQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        applyMainBackground();   // cover image full-layar (main_bg.png) jika ada
        applyAppLogo(R.id.imgAppLogo);

        etUrl     = findViewById(R.id.etUrl);
        btnStart  = findViewById(R.id.btnStart);
        btnScanQr = findViewById(R.id.btnScanQr);

        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                String url = etUrl != null && etUrl.getText() != null
                        ? etUrl.getText().toString().trim() : "";
                launchExam(url);
            });
        }

        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> {
                Intent intent = new Intent(this, QRScanActivity.class);
                startActivityForResult(intent, QR_SCAN_REQUEST);
            });
        }

        if (etUrl != null) {
            etUrl.setOnEditorActionListener((v, actionId, event) -> {
                String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
                launchExam(url);
                return true;
            });
        }
    }

    /**
     * Terapkan gambar cover full-layar (main_bg.png) jika tersedia di drawable.
     * Jika ada, headerContent dijadikan transparan agar background tembus.
     * Overlay gelap (#99000000) otomatis tampil untuk keterbacaan teks.
     * Jika tidak ada, halaman menggunakan warna solid colorHeaderBg.
     */
    private void applyMainBackground() {
        try {
            int resId = getResources().getIdentifier("main_bg", "drawable", getPackageName());
            if (resId == 0) return;

            Drawable d = getResources().getDrawable(resId, getTheme());
            if (d == null) return;

            ImageView imgBg  = findViewById(R.id.imgMainBg);
            View overlay     = findViewById(R.id.mainOverlay);
            LinearLayout headerContent = findViewById(R.id.headerContent);

            if (imgBg == null) return;

            imgBg.setImageDrawable(d);
            imgBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgBg.setVisibility(View.VISIBLE);

            if (overlay != null) overlay.setVisibility(View.VISIBLE);

            // Transparankan background warna solid di header agar gambar tembus
            if (headerContent != null) headerContent.setBackgroundColor(Color.TRANSPARENT);

        } catch (Exception e) {
            // Fallback: warna solid dari colorHeaderBg
        }
    }

    private void applyAppLogo(int viewId) {
        ImageView img = findViewById(viewId);
        if (img == null) return;
        img.setBackground(null);
        img.setBackgroundColor(Color.TRANSPARENT);
        img.setPadding(0, 0, 0, 0);
        img.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        int resId = getResources().getIdentifier("app_logo", "drawable", getPackageName());
        if (resId != 0) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId, opts);
            if (bmp != null) img.setImageBitmap(bmp);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST && resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("scanned_url");
            if (url != null && !url.isEmpty()) {
                if (isValidHttpUrl(url)) {
                    if (etUrl != null) etUrl.setText(url);
                    launchExam(url);
                } else {
                    Toast.makeText(this, "QR code tidak mengandung URL ujian yang valid", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void launchExam(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Masukkan URL ujian terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            String rawHost = url.split("[:/]")[0];
            url = (isPrivateHost(rawHost) ? "http://" : "https://") + url;
        }
        if (!isValidHttpUrl(url)) {
            Toast.makeText(this, "Format URL tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ExamActivity.class);
        intent.putExtra("exam_url", url);
        startActivity(intent);
    }

    private boolean isValidHttpUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            String host   = uri.getHost();
            if (scheme == null || host == null || host.isEmpty()) return false;
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            return isPrivateHost(host) || host.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPrivateHost(String host) {
        if (host == null) return false;
        if (host.equals("localhost")) return true;
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
        try {
            String[] parts = host.split("\\.");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            if (a == 10) return true;
            if (a == 172 && b >= 16 && b <= 31) return true;
            if (a == 192 && b == 168) return true;
            if (a == 127) return true;
        } catch (Exception ignored) {}
        return false;
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
        // Blokir tombol back di halaman home
    }
}
