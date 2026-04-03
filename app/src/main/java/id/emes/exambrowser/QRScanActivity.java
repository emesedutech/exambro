package id.emes.exambrowser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.io.IOException;

public class QRScanActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int CAM_PERM = 101;

    private Camera camera;
    private SurfaceHolder holder;
    private boolean scanning = true;
    private boolean torchOn  = false;
    private final MultiFormatReader reader = new MultiFormatReader();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Views
    private ScannerOverlayView overlayView;
    private TextView tvStatus;
    private ImageButton btnTorch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        setContentView(R.layout.activity_qr_scan);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        overlayView = findViewById(R.id.overlayView);
        tvStatus    = findViewById(R.id.tvStatus);
        btnTorch    = findViewById(R.id.btnTorch);
        ImageButton btnClose = findViewById(R.id.btnClose);

        holder = surfaceView.getHolder();
        holder.addCallback(this);

        btnClose.setOnClickListener(v -> finish());
        btnTorch.setOnClickListener(v -> toggleTorch());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAM_PERM);
        }
    }

    private void toggleTorch() {
        if (camera == null) return;
        try {
            Camera.Parameters p = camera.getParameters();
            torchOn = !torchOn;
            p.setFlashMode(torchOn
                ? Camera.Parameters.FLASH_MODE_TORCH
                : Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(p);
            btnTorch.setImageResource(torchOn
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
        } catch (Exception ignored) {}
    }

    private void startCamera() {
        try {
            camera = Camera.open();
            Camera.Parameters p = camera.getParameters();
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            // Rotasi preview sesuai orientasi
            camera.setDisplayOrientation(90);
            camera.setParameters(p);
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
            overlayView.startAnimation();
        } catch (IOException e) {
            Toast.makeText(this, "Gagal membuka kamera", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera cam) {
        if (!scanning) return;
        Camera.Size size = cam.getParameters().getPreviewSize();
        // Crop hanya area tengah (kotak scanner) supaya lebih akurat
        int w = size.width, h = size.height;
        int cropSize = Math.min(w, h) * 2 / 3;
        int left = (w - cropSize) / 2;
        int top  = (h - cropSize) / 2;
        try {
            PlanarYUVLuminanceSource src = new PlanarYUVLuminanceSource(
                data, w, h, left, top, cropSize, cropSize, false);
            Result result = reader.decode(new BinaryBitmap(new HybridBinarizer(src)));
            scanning = false;
            String url = result.getText();
            mainHandler.post(() -> {
                overlayView.showSuccess();
                // Delay sedikit supaya animasi success kelihatan
                mainHandler.postDelayed(() -> {
                    Intent intent = new Intent();
                    intent.putExtra("scanned_url", url);
                    setResult(RESULT_OK, intent);
                    finish();
                }, 600);
            });
        } catch (NotFoundException ignored) {
            // lanjut scan
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == CAM_PERM && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override public void surfaceCreated(@NonNull SurfaceHolder h) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) startCamera();
    }

    @Override public void surfaceChanged(@NonNull SurfaceHolder h, int f, int w, int hh) {
        if (camera != null) {
            camera.stopPreview();
            try { camera.setPreviewDisplay(h); camera.startPreview(); }
            catch (IOException ignored) {}
        }
    }

    @Override public void surfaceDestroyed(@NonNull SurfaceHolder h) { releaseCamera(); }

    @Override protected void onPause() { super.onPause(); releaseCamera(); }

    private void releaseCamera() {
        if (camera != null) {
            overlayView.stopAnimation();
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }
}
