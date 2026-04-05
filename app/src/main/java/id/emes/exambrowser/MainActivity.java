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

        if (etUrl == null || btnStart == null || btnScanQr == null) {
            // Seharusnya tidak pernah terjadi — log untuk debug jika ada masalah layout
            Toast.makeText(this, "Error: layout tidak termuat dengan benar", Toast.LENGTH_LONG).show();
            return;
        }

        // Tombol Mulai Ujian
        btnStart.setOnClickListener(v -> {
            String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            launchExam(url);
        });

        // Tombol Scan QR
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanActivity.class);
            startActivityForResult(intent, QR_SCAN_REQUEST);
        });

        // IME action "Go" pada keyboard juga trigger mulai ujian
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            launchExam(url);
            return true;
        });
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
        // Coba drawable-nodpi dulu, fallback ke drawable
        int resId = getResources().getIdentifier("app_logo", "drawable-nodpi", getPackageName());
        if (resId == 0) {
            resId = getResources().getIdentifier("app_logo", "drawable", getPackageName());
        }
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
            url = "https://" + url;
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
