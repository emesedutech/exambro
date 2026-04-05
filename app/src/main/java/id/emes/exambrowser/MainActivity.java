package id.emes.exambrowser;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int QR_SCAN_REQUEST = 200;
    private EditText etUrl;

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

        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            String ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("V " + ver);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("V 1.0");
        }

        applyHeaderBackground();
        applyAppLogo(R.id.imgAppLogo);

        etUrl = findViewById(R.id.etUrl);

        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> launchExam(etUrl.getText().toString().trim()));

        Button btnScanQrFull = findViewById(R.id.btnScanQrFull);
        btnScanQrFull.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanActivity.class);
            startActivityForResult(intent, QR_SCAN_REQUEST);
        });

        // Bottom bar — Info: hanya tampilkan dialog, tidak buka app lain
        LinearLayout btnInfo = findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(v -> showInfoDialog());

        // DIHAPUS: Rate Us & Share — kedua tombol ini membuka intent ke luar app
        // yang bisa dimanfaatkan siswa untuk keluar ke Play Store / browser / app lain.
        // Tombol diganti dengan dialog About saja.
        LinearLayout btnRate = findViewById(R.id.btnRate);
        if (btnRate != null) btnRate.setVisibility(View.GONE);

        LinearLayout btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) btnShare.setVisibility(View.GONE);

        LinearLayout btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void showInfoDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage(
                "Fitur yang dinonaktifkan selama ujian:\n\n" +
                "✗  Screenshot\n" +
                "✗  Dual Layar\n" +
                "✗  Tombol Back, Home, Recent\n" +
                "✗  Sembunyikan navigasi\n" +
                "✗  Sembunyikan notifikasi\n" +
                "✗  Navigasi ke situs lain\n" +
                "✗  Buka aplikasi lain"
            )
            .setPositiveButton("OK", null)
            .show();
    }

    private void showAboutDialog() {
        String ver = "";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        new AlertDialog.Builder(this)
            .setTitle("Tentang Aplikasi")
            .setMessage(
                getString(R.string.app_name) + " v" + ver + "\n\n" +
                "Aplikasi browser aman untuk pelaksanaan ujian online berbasis komputer (CBT).\n\n" +
                "© 2026 Emes EduTech"
            )
            .setPositiveButton("OK", null)
            .show();
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

    private void applyHeaderBackground() {
        try {
            int resId = getResources().getIdentifier("header_bg", "drawable", getPackageName());
            if (resId == 0) return;
            Drawable d = getResources().getDrawable(resId, getTheme());
            if (d == null) return;

            final ImageView imgBg      = findViewById(R.id.imgHeaderBg);
            final View overlay         = findViewById(R.id.headerOverlay);
            final LinearLayout content = findViewById(R.id.headerContent);

            imgBg.setImageDrawable(d);
            imgBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgBg.setBackgroundColor(Color.TRANSPARENT);
            content.setBackgroundColor(Color.TRANSPARENT);
            imgBg.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            // fallback warna solid
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST && resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("scanned_url");
            if (url != null && !url.isEmpty()) {
                // Validasi: hanya terima hasil QR yang merupakan URL http/https
                if (isValidHttpUrl(url)) {
                    etUrl.setText(url);
                    launchExam(url);
                } else {
                    Toast.makeText(this, "QR code tidak mengandung URL ujian yang valid", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void launchExam(String url) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Masukkan URL ujian terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        // Auto-prefix https jika tidak ada scheme
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        // Validasi format URL sebelum diluncurkan
        if (!isValidHttpUrl(url)) {
            Toast.makeText(this, "Format URL tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ExamActivity.class);
        intent.putExtra("exam_url", url);
        startActivity(intent);
    }

    /**
     * Validasi URL: harus http/https dengan host yang valid.
     */
    private boolean isValidHttpUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return (scheme != null && (scheme.equals("http") || scheme.equals("https")))
                && (host != null && !host.isEmpty())
                && host.contains(".");
        } catch (Exception e) {
            return false;
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
        // Blokir tombol back di halaman home
    }
}
