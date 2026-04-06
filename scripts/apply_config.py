#!/usr/bin/env python3
"""
apply_config.py — Patch Android source files sebelum Gradle build.
Membaca environment variables yang dikirim dari GitHub Actions workflow_dispatch.
"""

import os, re, sys, base64, hashlib

def env(key, default=""):
    return os.environ.get(key, default).strip()

# ── Ambil semua config ─────────────────────────────────────────────────────
APP_NAME        = env("APP_NAME",        "Emes Exam Browser")
APP_TITLE       = env("APP_TITLE",       "Emes Exam Browser")
APP_DESCRIPTION = env("APP_DESCRIPTION", "Safe browsing untuk ujian online")
EXIT_PIN        = env("EXIT_PIN",        "1234")
HEADER_COLOR    = env("HEADER_COLOR",    "1A3A5C").lstrip("#")
BUTTON_COLOR    = env("BUTTON_COLOR",    "2E7BF6").lstrip("#")
PACKAGE_ID      = env("PACKAGE_ID",      "id.emes.exambrowser")
HEADER_IMAGE    = env("HEADER_IMAGE",    "")
SPLASH_IMAGE    = env("SPLASH_IMAGE",    "")
VERSION_NAME    = env("VERSION_NAME",    "1.1.0")

print("=" * 56)
print("🔧  EzamBro Config Patcher")
print("=" * 56)
print(f"  App Name   : {APP_NAME}")
print(f"  App Title  : {APP_TITLE}")
print(f"  Description: {APP_DESCRIPTION}")
print(f"  Exit PIN   : {'*' * len(EXIT_PIN)}")
print(f"  Header     : #{HEADER_COLOR}")
print(f"  Button     : #{BUTTON_COLOR}")
print(f"  Package ID : {PACKAGE_ID}")
print(f"  Version    : {VERSION_NAME}")
LOGO_IMAGE_env = env("LOGO_IMAGE", "")
print(f"  Logo User  : {'✓ ada' if LOGO_IMAGE_env.strip() else '— pakai ic_launcher'}")
print(f"  Splash Img : {'✓ ada' if SPLASH_IMAGE.strip() else '— pakai warna'}")
print(f"  Header Img : {'✓ ada' if HEADER_IMAGE.strip() else '— pakai warna'}")
print("=" * 56)

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"  ✓ {path}")

def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

def decode_image(b64_data, dest_path, label):
    """Decode base64 image dan simpan ke dest_path. Return True jika berhasil."""
    try:
        b64 = b64_data.strip()
        if "," in b64:
            b64 = b64.split(",", 1)[1]
        img_bytes = base64.b64decode(b64)
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        with open(dest_path, "wb") as f:
            f.write(img_bytes)
        print(f"  ✓ {dest_path} [{label}] ({len(img_bytes)//1024} KB)")
        return True
    except Exception as e:
        print(f"  ⚠ Gagal decode {label}: {e} — pakai warna default")
        return False

def darken_hex(hex_color, factor=0.85):
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2],16), int(h[2:4],16), int(h[4:6],16)
    r = int(r * factor); g = int(g * factor); b = int(b * factor)
    return f"{r:02X}{g:02X}{b:02X}"

def hex_with_alpha(hex_color, alpha_pct):
    """Buat warna dengan alpha dari hex. alpha_pct: 0-100"""
    alpha_hex = hex(int(alpha_pct * 255 / 100))[2:].upper().zfill(2)
    return f"#{alpha_hex}{hex_color}"

HEADER_DARK   = darken_hex(HEADER_COLOR)
BUTTON_DARK   = darken_hex(BUTTON_COLOR)
BUTTON_DARKER = darken_hex(BUTTON_COLOR, 0.75)
BUTTON_SHADOW = darken_hex(BUTTON_COLOR, 0.65)
# Warna background tombol scan: 12% opacity dari warna aksen (semula hardcoded #EFF6FF)
SCAN_BG_ALPHA = hex_with_alpha(BUTTON_COLOR, 12)
# Warna stroke tombol scan ringan: 30% opacity (semula hardcoded #BFDBFE)
SCAN_STROKE_LIGHT = hex_with_alpha(BUTTON_COLOR, 30)

# ── 1. strings.xml ─────────────────────────────────────────────────────────
write("app/src/main/res/values/strings.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{APP_NAME}</string>
    <string name="app_title">{APP_TITLE}</string>
    <string name="app_description">{APP_DESCRIPTION}</string>
</resources>
""")

# ── 2. colors.xml ──────────────────────────────────────────────────────────
write("app/src/main/res/values/colors.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Warna utama — di-patch otomatis oleh apply_config.py saat build -->

    <!-- Header — ikut HEADER_COLOR -->
    <color name="colorHeaderBg">#{HEADER_COLOR}</color>
    <color name="colorHeaderDark">#{HEADER_DARK}</color>

    <!-- Primary / accent — ikut BUTTON_COLOR. SEMUA elemen interaktif mengacu ke sini -->
    <color name="colorPrimary">#{BUTTON_COLOR}</color>
    <color name="colorPrimaryDark">#{BUTTON_DARK}</color>
    <color name="colorAccent">#{BUTTON_COLOR}</color>

    <!-- Tombol utama (Mulai Ujian, Konfirmasi PIN) — ikut BUTTON_COLOR -->
    <color name="colorButtonBg">#{BUTTON_COLOR}</color>
    <color name="colorButtonPressed">#{BUTTON_DARKER}</color>
    <color name="colorButtonShadow">#{BUTTON_SHADOW}</color>

    <!-- Warna tombol scan QR: background 12% opacity dari warna aksen -->
    <color name="colorButtonScanBg">{SCAN_BG_ALPHA}</color>

    <!-- Backgrounds -->
    <color name="colorBackground">#F9FAFB</color>
    <color name="colorCardBg">#FFFFFF</color>
    <color name="colorSurface">#FFFFFF</color>

    <!-- Text -->
    <color name="colorTextPrimary">#111827</color>
    <color name="colorTextSecondary">#6B7280</color>
    <color name="colorTextHint">#9CA3AF</color>

    <!-- White variants -->
    <color name="white">#FFFFFF</color>
    <color name="white80">#CCFFFFFF</color>
    <color name="white60">#99FFFFFF</color>
    <color name="white30">#4DFFFFFF</color>

    <!-- Status -->
    <color name="colorSuccess">#10B981</color>
    <color name="colorDanger">#EF4444</color>
    <color name="colorWarning">#F59E0B</color>

    <!-- Misc -->
    <color name="colorDivider">#F3F4F6</color>
    <color name="colorInputBorder">#E5E7EB</color>
    <color name="colorProgressBar">#{BUTTON_COLOR}</color>
</resources>
""")

# ── 3. bg_button_scan.xml — patch agar ikut warna aksen ───────────────────
write("app/src/main/res/drawable/bg_button_scan.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/colorPrimaryDark" />
            <stroke android:width="1.5dp" android:color="@color/colorPrimary" />
            <corners android:radius="12dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonScanBg" />
            <stroke android:width="1.5dp" android:color="@color/colorPrimary" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</selector>
""")

# ── 4. bg_input.xml — pastikan border focus ikut warna aksen ──────────────
write("app/src/main/res/drawable/bg_input.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <stroke android:width="2dp" android:color="@color/colorPrimary" />
            <corners android:radius="12dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#F9FAFB" />
            <stroke android:width="1.5dp" android:color="@color/colorInputBorder" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</selector>
""")

# ── 5. bg_check.xml — centang fitur ikut warna aksen ──────────────────────
write("app/src/main/res/drawable/bg_check.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/colorPrimary" />
</shape>
""")

# ── 6. bg_button_primary.xml ───────────────────────────────────────────────
write("app/src/main/res/drawable/bg_button_primary.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonPressed"/>
            <corners android:radius="9dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonBg"/>
            <corners android:radius="9dp"/>
        </shape>
    </item>
</selector>
""")

# ── 6b. bg_btn_confirm.xml — tombol konfirmasi PIN ikut colorPrimary ──────
write("app/src/main/res/drawable/bg_btn_confirm.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonPressed" />
            <corners android:radius="9dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonBg" />
            <corners android:radius="9dp" />
        </shape>
    </item>
</selector>
""")

# ── 6c. bg_topbar.xml — top bar halaman ujian ikut colorHeaderBg ──────────
write("app/src/main/res/drawable/bg_topbar.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/colorHeaderBg" />
</shape>
""")

# ── 6d. bg_bottom_bar.xml — bottom bar halaman ujian ikut colorHeaderBg ───
write("app/src/main/res/drawable/bg_bottom_bar.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/colorHeaderBg" />
    <stroke android:width="1dp" android:color="#14FFFFFF" />
</shape>
""")

# ── 6e. bg_progress_track.xml — track progress bar ikut colorHeaderDark ───
write("app/src/main/res/drawable/bg_progress_track.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/colorHeaderDark" />
</shape>
""")

# ── 7. Splash background image ────────────────────────────────────────────
SPLASH_BG_PATH = "app/src/main/res/drawable/splash_bg.png"
if SPLASH_IMAGE.strip():
    if not decode_image(SPLASH_IMAGE, SPLASH_BG_PATH, "splash_bg"):
        if os.path.exists(SPLASH_BG_PATH):
            os.remove(SPLASH_BG_PATH)
else:
    if os.path.exists(SPLASH_BG_PATH):
        os.remove(SPLASH_BG_PATH)
        print(f"  ✓ splash_bg.png dihapus — pakai warna solid")

# ── 8. Header background image (opsional) ────────────────────────────────
HEADER_BG_PATH = "app/src/main/res/drawable/header_bg.png"
if HEADER_IMAGE.strip():
    if not decode_image(HEADER_IMAGE, HEADER_BG_PATH, "header_bg"):
        if os.path.exists(HEADER_BG_PATH):
            os.remove(HEADER_BG_PATH)
else:
    if os.path.exists(HEADER_BG_PATH):
        os.remove(HEADER_BG_PATH)
        print(f"  ✓ header_bg.png dihapus — pakai warna solid")

# ── 9. Patch version di activity_splash.xml ───────────────────────────────
splash_layout_path = "app/src/main/res/layout/activity_splash.xml"
if os.path.exists(splash_layout_path):
    splash_src = read(splash_layout_path)
    # Ganti semua pola versi di splash
    splash_src = re.sub(r'android:text="v[0-9]+\.[0-9]+\.[0-9]+"', f'android:text="v{VERSION_NAME}"', splash_src)
    splash_src = re.sub(r'>v[0-9]+\.[0-9]+\.[0-9]+<', f'>v{VERSION_NAME}<', splash_src)
    write(splash_layout_path, splash_src)

# ── 10. ExamActivity.java — patch EXIT_PIN_HASH (SHA-256) ─────────────────
exam_path = "app/src/main/java/id/emes/exambrowser/ExamActivity.java"
exam_src  = read(exam_path)
pin_hash  = hashlib.sha256(EXIT_PIN.encode("utf-8")).hexdigest()
print(f"  ℹ PIN SHA-256: {pin_hash[:8]}...{pin_hash[-8:]} (PIN tidak disimpan plaintext)")
exam_src  = re.sub(
    r'static String EXIT_PIN_HASH\s*=\s*"[^"]*"',
    f'static String EXIT_PIN_HASH = "{pin_hash}"',
    exam_src
)
write(exam_path, exam_src)

# ── 11. app/build.gradle — patch packageId + versionName ─────────────────
app_gradle_path = "app/build.gradle"
app_gradle = read(app_gradle_path)
app_gradle = re.sub(r'applicationId\s+"[^"]+"', f'applicationId "{PACKAGE_ID}"', app_gradle)
app_gradle = re.sub(r'namespace\s+"[^"]+"', f'namespace "{PACKAGE_ID}"', app_gradle)
app_gradle = re.sub(r'versionName\s+"[^"]+"', f'versionName "{VERSION_NAME}"', app_gradle)
write(app_gradle_path, app_gradle)

# ── 12. AndroidManifest.xml — patch package + label ──────────────────────
manifest_path = "app/src/main/AndroidManifest.xml"
manifest = read(manifest_path)
manifest = re.sub(r'android:label="[^"]+"', f'android:label="{APP_NAME}"', manifest, count=1)
write(manifest_path, manifest)

# ── 13. Rename Java package directory if needed ───────────────────────────
OLD_PACKAGE_ID = "id.emes.exambrowser"
if PACKAGE_ID != OLD_PACKAGE_ID:
    old_dir = f"app/src/main/java/{OLD_PACKAGE_ID.replace('.', '/')}"
    new_dir = f"app/src/main/java/{PACKAGE_ID.replace('.', '/')}"
    if os.path.isdir(old_dir) and not os.path.isdir(new_dir):
        import shutil
        os.makedirs(os.path.dirname(new_dir), exist_ok=True)
        shutil.copytree(old_dir, new_dir)
        java_files = []
        for root, dirs, files in os.walk(new_dir):
            for f in files:
                if f.endswith(".java"):
                    java_files.append(os.path.join(root, f))
        for jf in java_files:
            src = read(jf)
            src = src.replace(f"package {OLD_PACKAGE_ID};", f"package {PACKAGE_ID};")
            src = src.replace(f"import {OLD_PACKAGE_ID}.", f"import {PACKAGE_ID}.")
            write(jf, src)
        manifest2 = read(manifest_path)
        manifest2 = manifest2.replace(OLD_PACKAGE_ID, PACKAGE_ID)
        write(manifest_path, manifest2)
        # Patch build.gradle namespace juga
        app_gradle2 = read(app_gradle_path)
        app_gradle2 = app_gradle2.replace(OLD_PACKAGE_ID, PACKAGE_ID)
        write(app_gradle_path, app_gradle2)
        # Patch layout XML files — custom view references pakai fully-qualified class name
        layout_dir = "app/src/main/res/layout"
        if os.path.isdir(layout_dir):
            for xml_file in os.listdir(layout_dir):
                if xml_file.endswith(".xml"):
                    xml_path = os.path.join(layout_dir, xml_file)
                    xml_src = read(xml_path)
                    if OLD_PACKAGE_ID in xml_src:
                        xml_src = xml_src.replace(OLD_PACKAGE_ID, PACKAGE_ID)
                        write(xml_path, xml_src)
        print(f"  ✓ Package directory renamed: {old_dir} → {new_dir}")

# ── 14. Logo user → app_logo.png ─────────────────────────────────────────
LOGO_IMAGE    = env("LOGO_IMAGE", "")
APP_LOGO_PATH = "app/src/main/res/drawable-nodpi/app_logo.png"
if LOGO_IMAGE.strip():
    if decode_image(LOGO_IMAGE, APP_LOGO_PATH, "app_logo"):
        print(f"  ✓ Logo dari input disimpan ke drawable-nodpi/app_logo.png")
    else:
        print(f"  ⚠ Gagal decode logo dari input, pakai file yang sudah ada di repo")
else:
    if os.path.exists(APP_LOGO_PATH):
        print(f"  ✓ Logo dari repo dipakai: {APP_LOGO_PATH}")
    else:
        print(f"  ℹ Tidak ada logo — APK akan pakai ic_launcher default")

# ── 15. Salin app_logo ke drawable/ juga (agar SplashActivity & MainActivity bisa load) ──
LOGO_DRAWABLE_PATH = "app/src/main/res/drawable/app_logo.png"
if os.path.exists(APP_LOGO_PATH):
    import shutil
    shutil.copy2(APP_LOGO_PATH, LOGO_DRAWABLE_PATH)
    print(f"  ✓ Logo disalin ke drawable/app_logo.png")

print()
print("✅ Semua file berhasil di-patch. Siap untuk Gradle build.")
print("\n📝 Perubahan:")
print("   • colors.xml       : header, tombol, aksen, scan button — semua ter-patch")
print("   • bg_button_primary: tombol Mulai Ujian ikut colorPrimary")
print("   • bg_btn_confirm   : tombol Konfirmasi PIN ikut colorPrimary")
print("   • bg_button_scan   : tombol QR scan ikut colorPrimary")
print("   • bg_topbar        : top bar halaman ujian ikut colorHeaderBg")
print("   • bg_bottom_bar    : bottom bar halaman ujian ikut colorHeaderBg")
print("   • bg_progress_track: track progress bar ikut colorHeaderDark")
print("   • bg_input         : border focus input ikut colorPrimary")
print("   • bg_check         : titik centang fitur ikut colorPrimary")
print("   • Logo disalin ke drawable/ & drawable-nodpi/")
