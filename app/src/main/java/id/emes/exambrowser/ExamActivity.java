package id.emes.exambrowser;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ExamActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvSiteUrl;
    private boolean lockTaskActive = false;
    private Handler focusHandler = new Handler(Looper.getMainLooper());

    // PIN dibaca dari resources — di-patch otomatis saat build
    private String getExitPin() {
        int resId = getResources().getIdentifier("exit_pin", "string", getPackageName());
        if (resId != 0) return getString(resId);
        return "1234"; // fallback jika resource tidak ditemukan
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Keamanan window ──────────────────────────────────────────
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN        |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON    |
            WindowManager.LayoutParams.FLAG_SECURE            |  // blokir screenshot & screen record
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD  |  // bypass lockscreen
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED     // tetap tampil di atas lockscreen
        );

        hideSystemUI();
        setContentView(R.layout.activity_exam);

        webView     = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        tvSiteUrl   = findViewById(R.id.tvSiteUrl);
        Button btnExit = findViewById(R.id.btnExit);

        setupWebView();

        String url = getIntent().getStringExtra("exam_url");
        if (url != null) {
            tvSiteUrl.setText(extractHost(url));
            webView.loadUrl(url);
        }

        btnExit.setOnClickListener(v -> showExitDialog());

        // ── Kiosk Mode ───────────────────────────────────────────────
        startKioskMode();
    }

    private void startKioskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName admin = new ComponentName(this, ExamDeviceAdmin.class);
                if (dpm != null && dpm.isDeviceOwnerApp(getPackageName())) {
                    // Device Owner: lock task penuh, tidak bisa di-swipe keluar
                    dpm.setLockTaskPackages(admin, new String[]{getPackageName()});
                }
                startLockTask();
                lockTaskActive = true;
            }
        } catch (Exception e) {
            // Bukan device owner, screen pinning tetap aktif tapi bisa di-bypass
            // Mitigasi: focus watcher di bawah akan tarik kembali fokus
        }
    }

    private void stopKioskMode() {
        try {
            if (lockTaskActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
                lockTaskActive = false;
            }
        } catch (Exception e) { /* ignore */ }
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Keamanan WebView
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setGeolocationEnabled(false);
        s.setSaveFormData(false);
        s.setSavePassword(false);

        // Blokir akses ke intent:// dan file:// scheme dari dalam WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView v, String url, Bitmap fav) {
                // Blokir navigasi ke scheme berbahaya
                if (url != null && (
                        url.startsWith("intent://") ||
                        url.startsWith("file://") ||
                        url.startsWith("content://") ||
                        url.startsWith("javascript:") ||
                        url.startsWith("data:"))) {
                    v.stopLoading();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                progressBar.setVisibility(View.GONE);
                tvSiteUrl.setText(extractHost(url));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Hanya izinkan http dan https
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    return false;
                }
                // Blokir semua scheme lain (intent, market, tel, dll)
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progressBar.setProgress(p);
            }
        });
    }

    // ── BLOKIR SEMUA TOMBOL FISIK ────────────────────────────────────
    @Override
    public void onBackPressed() { /* dikunci */ }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_ASSIST:
                return true; // intercept semua
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_ASSIST:
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // ── PAKSA KEMBALI KE FOREGROUND JIKA KEHILANGAN FOKUS ────────────
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
        if (!hasFocus) {
            // Tutup notifikasi panel (Android < 12)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
            // Paksa balik ke foreground setelah 300ms
            focusHandler.postDelayed(() -> {
                try {
                    Intent bring = new Intent(this, ExamActivity.class);
                    bring.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                                   Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(bring);
                } catch (Exception e) { /* ignore */ }
            }, 300);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        // Pastikan lock task tetap aktif
        if (!lockTaskActive) startKioskMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Paksa balik ke app jika bukan karena exit PIN
        if (lockTaskActive) {
            focusHandler.postDelayed(() -> {
                Intent bring = new Intent(this, ExamActivity.class);
                bring.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                               Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(bring);
            }, 200);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Dipanggil saat user coba tekan home / app switcher
        hideSystemUI();
        if (lockTaskActive) {
            focusHandler.postDelayed(() -> {
                Intent bring = new Intent(this, ExamActivity.class);
                bring.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                               Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(bring);
            }, 100);
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN             |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION        |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY       |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE          |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN      |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    private String extractHost(String url) {
        try { return android.net.Uri.parse(url).getHost(); }
        catch (Exception e) { return url; }
    }

    // ── EXIT DIALOG + PIN ────────────────────────────────────────────
    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_exit_pin, null);
        builder.setView(dv);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.show();

        EditText etPin    = dv.findViewById(R.id.etPin);
        Button btnConfirm = dv.findViewById(R.id.btnConfirm);
        Button btnCancel  = dv.findViewById(R.id.btnCancel);
        etPin.setHintTextColor(0x66FFFFFF);

        btnConfirm.setOnClickListener(v -> {
            if (etPin.getText().toString().trim().equals(getExitPin())) {
                dialog.dismiss();
                focusHandler.removeCallbacksAndMessages(null);
                stopKioskMode();
                finish();
            } else {
                etPin.setText("");
                etPin.setError("PIN salah");
                Toast.makeText(this, "PIN tidak valid", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    protected void onDestroy() {
        focusHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
