package id.emes.exambrowser;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.content.Intent;
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

    static final String EXIT_PIN = "1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen + keep screen on + secure (no screenshot)
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SECURE
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

        // ── LOCK TASK (Screen Pinning / Kiosk mode) ──────────────────
        startKioskMode();
    }

    private void startKioskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName admin = new ComponentName(this, ExamDeviceAdmin.class);
                if (dpm != null && dpm.isDeviceOwnerApp(getPackageName())) {
                    dpm.setLockTaskPackages(admin, new String[]{getPackageName()});
                }
                startLockTask();
                lockTaskActive = true;
            }
        } catch (Exception e) {
            // Fallback: Screen Pinning manual tidak tersedia, tetap lanjut
        }
    }

    private void stopKioskMode() {
        try {
            if (lockTaskActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
                lockTaskActive = false;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, Bitmap fav) {
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView v, String url) {
                progressBar.setVisibility(View.GONE);
                tvSiteUrl.setText(extractHost(url));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                progressBar.setProgress(p);
            }
        });
    }

    private String extractHost(String url) {
        try { return android.net.Uri.parse(url).getHost(); }
        catch (Exception e) { return url; }
    }

    // ── BLOKIR SEMUA TOMBOL FISIK ────────────────────────────────────
    @Override public void onBackPressed() { /* dikunci */ }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_POWER:
                return true; // intercept
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
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // ── LOCK BALIK SAAT KEHILANGAN FOKUS (mis. notifikasi) ───────────
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
        if (!hasFocus) {
            // Tutup panel notifikasi jika terbuka
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                sendBroadcast(new android.content.Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
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

        EditText etPin     = dv.findViewById(R.id.etPin);
        Button btnConfirm  = dv.findViewById(R.id.btnConfirm);
        Button btnCancel   = dv.findViewById(R.id.btnCancel);
        etPin.setHintTextColor(0x66FFFFFF);

        btnConfirm.setOnClickListener(v -> {
            if (etPin.getText().toString().trim().equals(EXIT_PIN)) {
                dialog.dismiss();
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
}
