#!/usr/bin/env python3
"""
apply_config.py — Patch Android source files sebelum Gradle build.
Membaca environment variables yang dikirim dari GitHub Actions workflow_dispatch.
"""

import os, re, sys, base64, struct, zlib

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
LOGO_IMAGE_env  = env("LOGO_IMAGE",      "")

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

def is_valid_png(data):
    """Validasi apakah bytes merupakan PNG yang valid (cek signature + IEND chunk)."""
    PNG_SIG = b'\x89PNG\r\n\x1a\n'
    if len(data) < 8:
        return False
    if data[:8] != PNG_SIG:
        return False
    # Cek ada IEND chunk
    return b'IEND' in data

def is_valid_jpeg(data):
    """Validasi apakah bytes merupakan JPEG yang valid."""
    return len(data) > 2 and data[:2] == b'\xff\xd8' and data[-2:] == b'\xff\xd9'

def is_valid_image(data):
    """Cek apakah data adalah gambar PNG atau JPEG yang valid."""
    return is_valid_png(data) or is_valid_jpeg(data)

def decode_image(b64_data, dest_path, label):
    """
    Decode base64 image, validasi, dan simpan ke dest_path.
    Return True jika berhasil, False jika gagal (termasuk jika gambar tidak valid).
    KRITIS: Jika decode gagal, JANGAN biarkan file rusak tersimpan — hapus saja.
    """
    try:
        b64 = b64_data.strip()
        # Hapus data URI prefix jika ada (misal: data:image/png;base64,...)
        if "," in b64:
            b64 = b64.split(",", 1)[1]
        # Tambahkan padding jika kurang
        missing = len(b64) % 4
        if missing:
            b64 += "=" * (4 - missing)
        img_bytes = base64.b64decode(b64)
        # ── VALIDASI: pastikan file adalah gambar valid sebelum disimpan ──
        if not is_valid_image(img_bytes):
            print(f"  ⚠ {label}: data ter-decode tapi bukan PNG/JPEG valid — pakai warna default")
            return False
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        with open(dest_path, "wb") as f:
            f.write(img_bytes)
        size_kb = len(img_bytes) // 1024
        fmt = "PNG" if is_valid_png(img_bytes) else "JPEG"
        print(f"  ✓ {dest_path} [{label}] ({fmt}, {size_kb} KB)")
        return True
    except base64.binascii.Error as e:
        print(f"  ⚠ Gagal decode base64 {label}: {e} — pakai warna default")
    except Exception as e:
        print(f"  ⚠ Gagal simpan {label}: {e} — pakai warna default")
    # Pastikan file rusak tidak tersisa
    if os.path.exists(dest_path):
        os.remove(dest_path)
        print(f"  🗑 File rusak {dest_path} dihapus")
    return False

# ── 1. strings.xml ─────────────────────────────────────────────────────────
write("app/src/main/res/values/strings.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{APP_NAME}</string>
    <string name="app_title">{APP_TITLE}</string>
    <string name="app_description">{APP_DESCRIPTION}</string>
</resources>
""")

# ── 2. colors.xml ──────────────────────────────────────────────────────────
def darken_hex(hex_color, factor=0.85):
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2],16), int(h[2:4],16), int(h[4:6],16)
    r = int(r * factor); g = int(g * factor); b = int(b * factor)
    return f"{r:02X}{g:02X}{b:02X}"

HEADER_DARK = darken_hex(HEADER_COLOR)
BUTTON_DARK = darken_hex(BUTTON_COLOR)

write("app/src/main/res/values/colors.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Warna utama — di-patch otomatis oleh apply_config.py saat build -->
    <color name="colorHeaderBg">#{HEADER_COLOR}</color>
    <color name="colorHeaderDark">#{HEADER_DARK}</color>
    <color name="colorPrimary">#{BUTTON_COLOR}</color>
    <color name="colorPrimaryDark">#{BUTTON_DARK}</color>
    <color name="colorButtonPressed">#{darken_hex(BUTTON_COLOR, 0.75)}</color>
    <color name="colorButtonShadow">#{darken_hex(BUTTON_COLOR, 0.65)}</color>
    <color name="colorAccent">#{BUTTON_COLOR}</color>
    <color name="colorButtonBg">#{BUTTON_COLOR}</color>
    <!-- Backgrounds -->
    <color name="colorBackground">#F0F4FA</color>
    <color name="colorCardBg">#FFFFFF</color>
    <color name="colorSurface">#F8FAFF</color>
    <!-- Text -->
    <color name="colorTextPrimary">#0F172A</color>
    <color name="colorTextSecondary">#64748B</color>
    <color name="colorTextHint">#94A3B8</color>
    <color name="white">#FFFFFF</color>
    <color name="white80">#CCFFFFFF</color>
    <color name="white60">#99FFFFFF</color>
    <color name="white30">#4DFFFFFF</color>
    <!-- Status -->
    <color name="colorSuccess">#22C55E</color>
    <color name="colorDanger">#EF4444</color>
    <color name="colorWarning">#F59E0B</color>
    <!-- Misc -->
    <color name="colorDivider">#F1F5F9</color>
    <color name="colorInputBorder">#E5E7EB</color>
    <color name="colorProgressBar">#{BUTTON_COLOR}</color>
</resources>
""")

# ── 3. Bersihkan semua PNG opsional di awal (KRITIS: cegah AAPT2 error) ──
# Hapus dulu semua file PNG opsional tanpa syarat. Baru nanti dibuat ulang
# jika ada base64 yang valid. Ini mencegah file corrupt dari run sebelumnya
# atau dari git menyebabkan AAPT2 gagal compile.
OPTIONAL_PNG_FILES = [
    "app/src/main/res/drawable/header_bg.png",
    "app/src/main/res/drawable/splash_bg.png",
    "app/src/main/res/drawable/app_logo.png",
]
print("\n  🧹 Membersihkan PNG opsional lama...")
for _png in OPTIONAL_PNG_FILES:
    if os.path.exists(_png):
        os.remove(_png)
        print(f"     🗑 {_png} dihapus")
    else:
        print(f"     — {_png} tidak ada (oke)")

# ── 4. Splash background image ───────────────────────────────────────────
SPLASH_BG_PATH = "app/src/main/res/drawable/splash_bg.png"
if SPLASH_IMAGE.strip():
    if not decode_image(SPLASH_IMAGE, SPLASH_BG_PATH, "splash_bg"):
        print(f"  ⚠ splash_bg: decode gagal — pakai warna solid")
    # (file sudah dibersihkan di atas, decode_image hanya membuat jika valid)
else:
    print(f"  — splash_bg.png: tidak ada input — pakai warna solid")

# ── 5. Header background image (opsional) ────────────────────────────────
HEADER_BG_PATH = "app/src/main/res/drawable/header_bg.png"
if HEADER_IMAGE.strip():
    if not decode_image(HEADER_IMAGE, HEADER_BG_PATH, "header_bg"):
        print(f"  ⚠ header_bg: decode gagal — pakai warna solid")
    # (file sudah dibersihkan di atas, decode_image hanya membuat jika valid)
else:
    print(f"  — header_bg.png: tidak ada input — pakai warna solid")

# ── 5. bg_button_primary.xml ───────────────────────────────────────────────
write("app/src/main/res/drawable/bg_button_primary.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonBg"/>
            <corners android:radius="12dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/colorButtonBg"/>
            <corners android:radius="12dp"/>
        </shape>
    </item>
</selector>
""")

# ── 6. Patch version di activity_splash.xml ───────────────────────────────
splash_layout_path = "app/src/main/res/layout/activity_splash.xml"
if os.path.exists(splash_layout_path):
    splash_src = read(splash_layout_path)
    splash_src = re.sub(
        r'android:text="v[0-9]+\.[0-9]+\.[0-9]+"(\s+android:id="@\+id/splashVersion")',
        f'android:text="v{VERSION_NAME}"\\1',
        splash_src
    )
    splash_src = re.sub(
        r'(android:id="@\+id/splashVersion"[^>]*\n\s*android:text=")v[0-9]+\.[0-9]+\.[0-9]+"',
        f'\\1v{VERSION_NAME}"',
        splash_src
    )
    splash_src = re.sub(r'>v[0-9]+\.[0-9]+\.[0-9]+<', f'>v{VERSION_NAME}<', splash_src)
    write(splash_layout_path, splash_src)

# ── 7. ExamActivity.java — patch EXIT_PIN ─────────────────────────────────
exam_path = "app/src/main/java/id/emes/exambrowser/ExamActivity.java"
exam_src  = read(exam_path)
exam_src  = re.sub(
    r'static final String EXIT_PIN\s*=\s*"[^"]*"',
    f'static final String EXIT_PIN = "{EXIT_PIN}"',
    exam_src
)
write(exam_path, exam_src)

# ── 8. app/build.gradle — patch packageId + versionName ───────────────────
app_gradle_path = "app/build.gradle"
app_gradle = read(app_gradle_path)
app_gradle = re.sub(r'applicationId\s+"[^"]+"', f'applicationId "{PACKAGE_ID}"', app_gradle)
app_gradle = re.sub(r'versionName\s+"[^"]+"', f'versionName "{VERSION_NAME}"', app_gradle)
write(app_gradle_path, app_gradle)

# ── 9. AndroidManifest.xml — patch package + label ───────────────────────
manifest_path = "app/src/main/AndroidManifest.xml"
manifest = read(manifest_path)
manifest = re.sub(r'package="[^"]+"', f'package="{PACKAGE_ID}"', manifest)
manifest = re.sub(r'android:label="[^"]+"', f'android:label="{APP_NAME}"', manifest, count=1)
write(manifest_path, manifest)

# ── 10. Rename Java package directory if needed ────────────────────────────
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
        print(f"  ✓ Package directory renamed: {old_dir} → {new_dir}")

# ── 11. Logo user → app_logo.png (transparan, bukan ic_launcher) ──────────
# Catatan: app_logo.png sudah dibersihkan di langkah 3 di atas
APP_LOGO_PATH = "app/src/main/res/drawable/app_logo.png"
if LOGO_IMAGE_env.strip():
    if decode_image(LOGO_IMAGE_env, APP_LOGO_PATH, "app_logo"):
        print(f"  ✓ Logo user disimpan sebagai app_logo.png (transparan)")
    else:
        print(f"  — app_logo.png: decode gagal — fallback ke ic_launcher")
else:
    print(f"  — app_logo.png: tidak ada input — fallback ke ic_launcher")

print()
print("✅ Semua file berhasil di-patch. Siap untuk Gradle build.")
print("\n📝 Logo: app_logo.png (transparan) dipakai jika ada, fallback ic_launcher.")
