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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int QR_SCAN_REQUEST = 200;

    // Deklarasi eksplisit sebagai field — mencegah R8 menganggapnya dead code
    private EditText etUrl;
    private Button btnStart;
    private LinearLayout btnScanQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FLAG_SECURE: cegah screenshot di layar home
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        applyHeaderBackground();
        applyAppLogo(R.id.imgAppLogo);

        // Bind views — etUrl dan btnScanQr adalah fitur inti, WAJIB ada
        etUrl     = findViewById(R.id.etUrl);
        btnStart  = findViewById(R.id.btnStart);
        btnScanQr = findViewById(R.id.btnScanQr);

        // Tombol Mulai Ujian
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                String url = etUrl != null && etUrl.getText() != null
                        ? etUrl.getText().toString().trim() : "";
                launchExam(url);
            });
        }

        // Tombol Scan QR
        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> {
                Intent intent = new Intent(this, QRScanActivity.class);
                startActivityForResult(intent, QR_SCAN_REQUEST);
            });
        }

        // IME action "Go" pada keyboard juga trigger mulai ujian
        if (etUrl != null) {
            etUrl.setOnEditorActionListener((v, actionId, event) -> {
                String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
                launchExam(url);
                return true;
            });
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
        // getIdentifier type harus "drawable" bukan "drawable-nodpi" (qualifier bukan type)
        int resId = getResources().getIdentifier("app_logo", "drawable", getPackageName());
        if (resId != 0) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId, opts);
            if (bmp != null) img.setImageBitmap(bmp);
        }
    }

    private void applyHeaderBackground() {
        try {
            int resId = getResources().getIdentifier("header_bg", "drawable", getPackageName());
            if (resId == 0) return;
            Drawable d = getResources().getDrawable(resId, getTheme());
            if (d == null) return;

            final ImageView imgBg      = findViewById(R.id.imgHeaderBg);
            final View overlay         = findViewById(R.id.headerOverlay);
            final LinearLayout content = findViewById(R.id.headerContent);

            if (imgBg == null || overlay == null || content == null) return;

            imgBg.setImageDrawable(d);
            imgBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgBg.setBackgroundColor(Color.TRANSPARENT);
            content.setBackgroundColor(Color.TRANSPARENT);
            imgBg.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            // fallback warna solid — normal jika tidak ada header_bg
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
            // Ekstrak host dari input (sebelum ada scheme) untuk cek apakah lokal
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
            // Izinkan: localhost, IP address, atau domain dengan titik
            return isPrivateHost(host) || host.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    /** Cek apakah host adalah localhost atau IP dalam range privat RFC-1918. */
    private boolean isPrivateHost(String host) {
        if (host == null) return false;
        if (host.equals("localhost")) return true;
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
        try {
            String[] parts = host.split("\\.");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            if (a == 10) return true;                        // 10.0.0.0/8
            if (a == 172 && b >= 16 && b <= 31) return true; // 172.16.0.0/12
            if (a == 192 && b == 168) return true;           // 192.168.0.0/16
            if (a == 127) return true;                       // 127.0.0.0/8 loopback
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
