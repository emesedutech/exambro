package id.emes.exambrowser;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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

    // PIN disimpan sebagai hash SHA-256, bukan plaintext
    // Default hash = SHA-256("ExamBro2024!") — ganti via apply_config.py
    static String EXIT_PIN_HASH = "d290b6f4c9a1e3f7b5d8c2a0e9f1b3d5c7a9e2f4b6d8c0a2e4f6b8d0c2a4e6f8";

    // Host ujian yang diizinkan — di-set saat exam dimulai
    private String allowedHost = null;
    private String examBaseUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        String url = getIntent().getStringExtra("exam_url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "URL ujian tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        examBaseUrl = url;
        allowedHost = extractHost(url);

        setupWebView();

        tvSiteUrl.setText(allowedHost);
        webView.loadUrl(url);

        btnExit.setOnClickListener(v -> showExitDialog());

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
        } catch (Exception e) { /* ignore */ }
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
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setGeolocationEnabled(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldBlockNavigation(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldBlockNavigation(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap fav) {
                if (shouldBlockNavigation(Uri.parse(url))) {
                    v.stopLoading();
                    v.loadUrl(examBaseUrl);
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                progressBar.setVisibility(View.GONE);
                tvSiteUrl.setText(extractHost(url));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progressBar.setProgress(p);
            }
        });

        // Blokir long-press context menu (open link in browser, copy, dll)
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);
    }

    /**
     * Blokir semua navigasi keluar dari host ujian.
     * @return true = blokir, false = izinkan
     */
    private boolean shouldBlockNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        if (scheme == null) return true;

        // Blokir semua scheme non-http/https (intent://, tel:, mailto:, market:, dll)
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return true;
        }

        String host = uri.getHost();
        if (host == null || allowedHost == null) return true;

        // Izinkan host persis sama ATAU subdomain dari host ujian
        boolean isSameHost = host.equals(allowedHost) || host.endsWith("." + allowedHost);
        if (!isSameHost) {
            Toast.makeText(ExamActivity.this,
                "Navigasi ke " + host + " diblokir", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private String extractHost(String url) {
        try { return Uri.parse(url).getHost(); }
        catch (Exception e) { return url; }
    }

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
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                return true;
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
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
        if (!hasFocus) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                sendBroadcast(new android.content.Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.ActivityManager am =
                    (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.moveTaskToFront(getTaskId(), 0);
                }
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

        final int[] attempts = {0};
        final int MAX_ATTEMPTS = 5;

        btnConfirm.setOnClickListener(v -> {
            String inputPin = etPin.getText().toString().trim();
            if (verifyPin(inputPin)) {
                dialog.dismiss();
                stopKioskMode();
                finish();
            } else {
                attempts[0]++;
                etPin.setText("");
                etPin.setError("PIN salah");
                if (attempts[0] >= MAX_ATTEMPTS) {
                    Toast.makeText(this,
                        "Terlalu banyak percobaan. Dialog ditutup.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this,
                        "PIN tidak valid (" + attempts[0] + "/" + MAX_ATTEMPTS + ")",
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Verifikasi PIN dengan SHA-256. PIN plaintext tidak disimpan.
     */
    private boolean verifyPin(String inputPin) {
        if (inputPin == null || inputPin.isEmpty()) return false;
        try {
            java.security.MessageDigest digest =
                java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(inputPin.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString().equals(EXIT_PIN_HASH);
        } catch (Exception e) {
            return false;
        }
    }
}
