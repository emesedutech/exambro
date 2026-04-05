# Jaga kelas utama aplikasi
-keep class id.emes.exambrowser.** { *; }

# WebView JavaScript interface (jika dipakai di masa depan)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ZXing QR scanner
-keep class com.google.zxing.** { *; }

# Device Admin
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Jaga nama method yang dipanggil via reflection oleh Android
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Hindari warning dari library
-dontwarn com.google.zxing.**
-dontwarn androidx.**
