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
    private boolean exitDialogOpen  = false;

    // PIN disimpan sebagai hash SHA-256 — di-patch oleh apply_config.py saat build
    static String EXIT_PIN_HASH = "d290b6f4c9a1e3f7b5d8c2a0e9f1b3d5c7a9e2f4b6d8c0a2e4f6b8d0c2a4e6f8";

    // Host ujian yang diizinkan — dikunci saat exam dimulai
    private String allowedHost = null;
    private String examBaseUrl  = null;

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

    // ── Kiosk mode ────────────────────────────────────────────────────────
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

    // ── WebView setup ─────────────────────────────────────────────────────
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Blokir mixed content (HTTP dalam halaman HTTPS)
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
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

        // Blokir long-press context menu (buka di browser, salin link, dll)
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);
    }

    /** Blokir semua navigasi keluar dari host ujian. @return true = blokir */
    private boolean shouldBlockNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        if (scheme == null) return true;
        // Hanya izinkan http dan https
        if (!scheme.equals("http") && !scheme.equals("https")) return true;
        String host = uri.getHost();
        if (host == null || allowedHost == null) return true;
        // HTTP hanya boleh untuk localhost dan IP privat (10.x, 172.16-31.x, 192.168.x)
        if (scheme.equals("http")) {
            if (!isPrivateHost(host)) {
                Toast.makeText(this, "HTTP ke host publik diblokir", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        // Izinkan host persis sama ATAU subdomain
        boolean ok = host.equals(allowedHost) || host.endsWith("." + allowedHost);
        if (!ok) Toast.makeText(this, "Navigasi ke " + host + " diblokir", Toast.LENGTH_SHORT).show();
        return !ok;
    }

    /** Cek apakah host adalah localhost atau IP dalam range privat RFC-1918. */
    private boolean isPrivateHost(String host) {
        if (host == null) return false;
        if (host.equals("localhost")) return true;
        // Harus berupa IP address dulu
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
        try {
            String[] parts = host.split("\\.");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            // 10.0.0.0/8
            if (a == 10) return true;
            // 172.16.0.0/12  (172.16 - 172.31)
            if (a == 172 && b >= 16 && b <= 31) return true;
            // 192.168.0.0/16
            if (a == 192 && b == 168) return true;
            // 127.0.0.0/8 (loopback)
            if (a == 127) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private String extractHost(String url) {
        try { return Uri.parse(url).getHost(); }
        catch (Exception e) { return url; }
    }

    // ── Blokir semua tombol fisik ─────────────────────────────────────────
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

    // ── Anti task-switch: paksa kembali ke foreground ─────────────────────
    @Override
    protected void onUserLeaveHint() {
        // Dipanggil saat user menekan Home/Overview — paksa balik
        super.onUserLeaveHint();
        if (!exitDialogOpen) {
            bringToFront();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!exitDialogOpen) {
            bringToFront();
        }
    }

    private void bringToFront() {
        try {
            android.app.ActivityManager am =
                (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) am.moveTaskToFront(getTaskId(), 0);
        } catch (Exception ignored) {}
    }

    // ── Fullscreen paksa ketika fokus kembali ─────────────────────────────
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
        if (!hasFocus) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bringToFront();
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

    // ── Dialog PIN keluar ─────────────────────────────────────────────────
    private void showExitDialog() {
        exitDialogOpen = true;
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
        dialog.setOnDismissListener(d -> exitDialogOpen = false);
        dialog.show();

        EditText etPin    = dv.findViewById(R.id.etPin);
        Button btnConfirm = dv.findViewById(R.id.btnConfirm);
        Button btnCancel  = dv.findViewById(R.id.btnCancel);

        final int[] attempts = {0};
        final int MAX_ATTEMPTS = 5;

        btnConfirm.setOnClickListener(v -> {
            String input = etPin.getText().toString().trim();
            if (verifyPin(input)) {
                dialog.dismiss();
                stopKioskMode();
                finish();
            } else {
                attempts[0]++;
                etPin.setText("");
                etPin.setError("PIN salah");
                if (attempts[0] >= MAX_ATTEMPTS) {
                    Toast.makeText(this, "Terlalu banyak percobaan.", Toast.LENGTH_LONG).show();
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

    /** Verifikasi PIN dengan SHA-256 — plaintext tidak pernah disimpan. */
    private boolean verifyPin(String input) {
        if (input == null || input.isEmpty()) return false;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString().equals(EXIT_PIN_HASH);
        } catch (Exception e) { return false; }
    }
}
