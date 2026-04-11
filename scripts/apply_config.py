#!/usr/bin/env python3
"""
apply_config.py — Patch Android source files sebelum Gradle build.
Membaca environment variables yang dikirim dari GitHub Actions workflow_dispatch.

PENTING: Gambar opsional (header, splash, logo) disimpan ke assets/ bukan drawable/
karena assets/ tidak diproses AAPT2 — aman untuk semua format gambar.
"""

import os, re, sys, base64

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
print(f"  Logo       : {'✓ ada' if LOGO_IMAGE_env.strip() else '— pakai ic_launcher'}")
print(f"  Splash Img : {'✓ ada' if SPLASH_IMAGE.strip() else '— pakai warna'}")
print(f"  Header Img : {'✓ ada' if HEADER_IMAGE.strip() else '— pakai warna'}")
print("=" * 56)

# ── SAFETY: Hapus SEMUA file gambar opsional di awal ─────────────────────
# Ini mencegah file lama/rusak dari git commit sebelumnya ikut dikompilasi
print("\n🗑  Membersihkan file gambar lama...")
_clean_paths = [
    # drawable/ — diproses AAPT2, file rusak = build gagal
    "app/src/main/res/drawable/header_bg.png",
    "app/src/main/res/drawable/header_bg.jpg",
    "app/src/main/res/drawable/splash_bg.png",
    "app/src/main/res/drawable/splash_bg.jpg",
    "app/src/main/res/drawable/app_logo.png",
    # assets/ — akan dibuat ulang di bawah jika input tersedia
    "app/src/main/assets/header_bg.png",
    "app/src/main/assets/header_bg.jpg",
    "app/src/main/assets/splash_bg.png",
    "app/src/main/assets/splash_bg.jpg",
    "app/src/main/assets/app_logo.png",
]
for _f in _clean_paths:
    if os.path.exists(_f):
        os.remove(_f)
        print(f"  🗑 Dihapus: {_f}")

# Pastikan folder assets/ ada
os.makedirs("app/src/main/assets", exist_ok=True)
print("  ✓ Folder assets/ siap")

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"  ✓ {path}")

def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

def is_valid_png(data):
    """Cek PNG signature."""
    return len(data) >= 8 and data[:8] == b'\x89PNG\r\n\x1a\n'

def is_valid_jpeg(data):
    """Cek JPEG — toleran terhadap trailing bytes."""
    if len(data) < 4:
        return False
    if data[:2] != b'\xff\xd8':
        return False
    # Cari EOI marker di 16 byte terakhir (toleran trailing bytes dari browser)
    return b'\xff\xd9' in data[-16:]

def is_valid_image(data):
    return is_valid_png(data) or is_valid_jpeg(data)

def decode_image(b64_data, dest_path, label):
    """
    Decode base64 → validasi → simpan ke assets/.
    Return True jika berhasil, False jika gagal.
    File TIDAK disimpan ke drawable/ — hanya ke assets/ agar aman dari AAPT2.
    """
    if not b64_data or not b64_data.strip():
        return False
    try:
        b64 = b64_data.strip()
        # Strip data URI prefix (data:image/jpeg;base64,...)
        if "," in b64:
            b64 = b64.split(",", 1)[1]
        # Fix base64 padding
        b64 = b64.strip()
        missing = len(b64) % 4
        if missing:
            b64 += "=" * (4 - missing)
        img_bytes = base64.b64decode(b64)
        if not is_valid_image(img_bytes):
            print(f"  ⚠ {label}: bukan PNG/JPEG valid ({len(img_bytes)} bytes) — pakai warna")
            # Debug info
            if len(img_bytes) >= 4:
                print(f"    Header bytes: {img_bytes[:4].hex()}")
                print(f"    Tail bytes:   {img_bytes[-4:].hex()}")
            return False
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        with open(dest_path, "wb") as f:
            f.write(img_bytes)
        fmt = "PNG" if is_valid_png(img_bytes) else "JPEG"
        print(f"  ✓ {dest_path} [{label}] ({fmt}, {len(img_bytes)//1024} KB)")
        return True
    except Exception as e:
        print(f"  ⚠ Gagal decode {label}: {e}")
        if os.path.exists(dest_path):
            os.remove(dest_path)
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
    return f"{int(r*factor):02X}{int(g*factor):02X}{int(b*factor):02X}"

write("app/src/main/res/values/colors.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="colorHeaderBg">#{HEADER_COLOR}</color>
    <color name="colorHeaderDark">#{darken_hex(HEADER_COLOR)}</color>
    <color name="colorPrimary">#{BUTTON_COLOR}</color>
    <color name="colorPrimaryDark">#{darken_hex(BUTTON_COLOR)}</color>
    <color name="colorButtonPressed">#{darken_hex(BUTTON_COLOR, 0.75)}</color>
    <color name="colorButtonShadow">#{darken_hex(BUTTON_COLOR, 0.65)}</color>
    <color name="colorAccent">#{BUTTON_COLOR}</color>
    <color name="colorButtonBg">#{BUTTON_COLOR}</color>
    <color name="colorBackground">#F0F4FA</color>
    <color name="colorCardBg">#FFFFFF</color>
    <color name="colorSurface">#F8FAFF</color>
    <color name="colorTextPrimary">#0F172A</color>
    <color name="colorTextSecondary">#64748B</color>
    <color name="colorTextHint">#94A3B8</color>
    <color name="white">#FFFFFF</color>
    <color name="white80">#CCFFFFFF</color>
    <color name="white60">#99FFFFFF</color>
    <color name="white30">#4DFFFFFF</color>
    <color name="colorSuccess">#22C55E</color>
    <color name="colorDanger">#EF4444</color>
    <color name="colorWarning">#F59E0B</color>
    <color name="colorDivider">#F1F5F9</color>
    <color name="colorInputBorder">#E5E7EB</color>
    <color name="colorProgressBar">#{BUTTON_COLOR}</color>
</resources>
""")

# ── 3. Splash background → assets/ ────────────────────────────────────────
if SPLASH_IMAGE.strip():
    decode_image(SPLASH_IMAGE, "app/src/main/assets/splash_bg.png", "splash_bg")
else:
    print("  ✓ Splash: pakai warna solid (tidak ada gambar)")

# ── 4. Header background → assets/ ────────────────────────────────────────
if HEADER_IMAGE.strip():
    decode_image(HEADER_IMAGE, "app/src/main/assets/header_bg.png", "header_bg")
else:
    print("  ✓ Header: pakai warna solid (tidak ada gambar)")

# ── 5. bg_button_primary.xml ──────────────────────────────────────────────
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
    splash_src = re.sub(r'>v[0-9]+\.[0-9]+\.[0-9]+<', f'>v{VERSION_NAME}<', splash_src)
    write(splash_layout_path, splash_src)

# ── 7. ExamActivity.java — patch EXIT_PIN ─────────────────────────────────
exam_path = "app/src/main/java/id/emes/exambrowser/ExamActivity.java"
if os.path.exists(exam_path):
    exam_src = read(exam_path)
    exam_src = re.sub(
        r'static final String EXIT_PIN\s*=\s*"[^"]*"',
        f'static final String EXIT_PIN = "{EXIT_PIN}"',
        exam_src
    )
    write(exam_path, exam_src)

# ── 8. app/build.gradle — patch packageId + versionName ──────────────────
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

# ── 10. Rename Java package directory if needed ───────────────────────────
OLD_PACKAGE_ID = "id.emes.exambrowser"
if PACKAGE_ID != OLD_PACKAGE_ID:
    old_dir = f"app/src/main/java/{OLD_PACKAGE_ID.replace('.', '/')}"
    new_dir = f"app/src/main/java/{PACKAGE_ID.replace('.', '/')}"
    if os.path.isdir(old_dir) and not os.path.isdir(new_dir):
        import shutil
        shutil.copytree(old_dir, new_dir)
        for root, dirs, files in os.walk(new_dir):
            for fname in files:
                if fname.endswith(".java"):
                    fp = os.path.join(root, fname)
                    src = read(fp)
                    src = src.replace(f"package {OLD_PACKAGE_ID};", f"package {PACKAGE_ID};")
                    src = src.replace(f"import {OLD_PACKAGE_ID}.", f"import {PACKAGE_ID}.")
                    write(fp, src)
        manifest2 = read(manifest_path)
        manifest2 = manifest2.replace(OLD_PACKAGE_ID, PACKAGE_ID)
        write(manifest_path, manifest2)
        print(f"  ✓ Package renamed: {old_dir} → {new_dir}")

# ── 11. Logo → assets/ ────────────────────────────────────────────────────
if LOGO_IMAGE_env.strip():
    decode_image(LOGO_IMAGE_env, "app/src/main/assets/app_logo.png", "app_logo")
else:
    print("  ✓ Logo: pakai ic_launcher default")

print()
print("✅ Semua file berhasil di-patch.")
print("📝 Gambar disimpan di assets/ — aman dari AAPT2.")
print(f"   assets/: {os.listdir('app/src/main/assets')}")
