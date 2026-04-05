# Jaga semua kelas utama aplikasi
-keep class id.emes.exambrowser.** { *; }

# PENTING: Jaga semua Activity, View, dan field yang diakses via findViewById
# Tanpa ini R8 bisa strip etUrl, btnScanQr, dll → input URL & tombol QR hilang
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.appcompat.app.AppCompatActivity { *; }
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Jaga semua View yang dipakai via XML / findViewById
-keepclassmembers class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Jaga resource IDs — penting agar etUrl, btnScanQr, btnStart tidak di-strip
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keep class **.R
-keep class **.R$* { *; }

# WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ZXing QR scanner — WAJIB agar fitur scan QR tetap berjalan
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Device Admin
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# AndroidX & Material — jaga agar tidak ada crash saat runtime
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Jaga Enum agar tidak ter-strip (dipakai oleh beberapa library)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Jaga Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Jaga Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
