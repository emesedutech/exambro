#!/usr/bin/env python3
"""
apply_config.py — Patch Android source files sebelum Gradle build.
Membaca environment variables yang dikirim dari GitHub Actions workflow_dispatch.
"""

import os, re, sys, base64

def env(key, default=""):
    return os.environ.get(key, default).strip()

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
print("EzamBro Config Patcher")
print("=" * 56)
print(f"  App Name   : {APP_NAME}")
print(f"  App Title  : {APP_TITLE}")
print(f"  Description: {APP_DESCRIPTION}")
print(f"  Exit PIN   : {'*' * len(EXIT_PIN)}")
print(f"  Header     : #{HEADER_COLOR}")
print(f"  Button     : #{BUTTON_COLOR}")
print(f"  Package ID : {PACKAGE_ID}")
print(f"  Version    : {VERSION_NAME}")
print(f"  Logo User  : {'ada' if LOGO_IMAGE_env.strip() else 'pakai ic_launcher'}")
print(f"  Splash Img : {'ada' if SPLASH_IMAGE.strip() else 'pakai warna'}")
print(f"  Header Img : {'ada' if HEADER_IMAGE.strip() else 'pakai warna'}")
print("=" * 56)

# ── LANGKAH PERTAMA: HAPUS SEMUA PNG OPSIONAL TANPA KONDISI ──────────────
# Ini WAJIB dilakukan pertama kali sebelum apapun untuk mencegah
# file PNG corrupt dari run sebelumnya menyebabkan AAPT2 error.
OPTIONAL_PNGS = [
    "app/src/main/res/drawable/header_bg.png",
    "app/src/main/res/drawable/splash_bg.png",
    "app/src/main/res/drawable/app_logo.png",
]
print("\n[1] Bersihkan PNG opsional lama...")
for p in OPTIONAL_PNGS:
    if os.path.exists(p):
        os.remove(p)
        print(f"    hapus: {p}")
    else:
        print(f"    tidak ada: {p} (ok)")

# ── Helper functions ──────────────────────────────────────────────────────

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"  tulis: {path}")

def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

def is_valid_image(data):
    """Validasi PNG atau JPEG."""
    if len(data) < 8:
        return False
    # PNG: signature + IEND chunk
    if data[:8] == b'\x89PNG\r\n\x1a\n' and b'IEND' in data:
        return True
    # JPEG: SOI marker
    if data[:2] == b'\xff\xd8' and b'\xff\xd9' in data:
        return True
    return False

def decode_and_save_image(b64_data, dest_path, label):
    """
    Decode base64, validasi, simpan ke dest_path.
    Return True jika berhasil. File TIDAK disimpan jika tidak valid.
    """
    if not b64_data or not b64_data.strip():
        print(f"  [{label}] kosong - pakai warna solid")
        return False
    try:
        b64 = b64_data.strip()
        # Strip data URI prefix
        if "," in b64:
            b64 = b64.split(",", 1)[1]
        # Fix padding
        remainder = len(b64) % 4
        if remainder:
            b64 += "=" * (4 - remainder)
        img_bytes = base64.b64decode(b64, validate=False)
        if not is_valid_image(img_bytes):
            print(f"  [{label}] bukan PNG/JPEG valid - pakai warna solid")
            return False
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        with open(dest_path, "wb") as f:
            f.write(img_bytes)
        fmt = "PNG" if img_bytes[:4] == b'\x89PNG' else "JPEG"
        print(f"  [{label}] disimpan: {dest_path} ({fmt}, {len(img_bytes)//1024}KB)")
        return True
    except Exception as e:
        print(f"  [{label}] GAGAL decode: {e} - pakai warna solid")
        # Pastikan file tidak tersisa
        if os.path.exists(dest_path):
            os.remove(dest_path)
        return False


# ── 2. strings.xml ───────────────────────────────────────────────────────
print("\n[2] strings.xml...")
write("app/src/main/res/values/strings.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{APP_NAME}</string>
    <string name="app_title">{APP_TITLE}</string>
    <string name="app_description">{APP_DESCRIPTION}</string>
</resources>
""")

# ── 3. colors.xml ────────────────────────────────────────────────────────
print("\n[3] colors.xml...")
def darken_hex(hex_color, factor=0.85):
    h = hex_color.lstrip("#")
    r = int(int(h[0:2], 16) * factor)
    g = int(int(h[2:4], 16) * factor)
    b = int(int(h[4:6], 16) * factor)
    return f"{r:02X}{g:02X}{b:02X}"

HEADER_DARK = darken_hex(HEADER_COLOR)
BUTTON_DARK = darken_hex(BUTTON_COLOR)

write("app/src/main/res/values/colors.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="colorHeaderBg">#{HEADER_COLOR}</color>
    <color name="colorHeaderDark">#{HEADER_DARK}</color>
    <color name="colorPrimary">#{BUTTON_COLOR}</color>
    <color name="colorPrimaryDark">#{BUTTON_DARK}</color>
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

# ── 4. bg_button_primary.xml ─────────────────────────────────────────────
print("\n[4] bg_button_primary.xml...")
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

# ── 5. Gambar header (opsional) ───────────────────────────────────────────
print("\n[5] Header image...")
decode_and_save_image(HEADER_IMAGE, "app/src/main/res/drawable/header_bg.png", "header_bg")

# ── 6. Gambar splash (opsional) ───────────────────────────────────────────
print("\n[6] Splash image...")
decode_and_save_image(SPLASH_IMAGE, "app/src/main/res/drawable/splash_bg.png", "splash_bg")

# ── 7. Logo (opsional) ────────────────────────────────────────────────────
print("\n[7] Logo image...")
decode_and_save_image(LOGO_IMAGE_env, "app/src/main/res/drawable/app_logo.png", "app_logo")

# ── 8. Patch version di activity_splash.xml ──────────────────────────────
print("\n[8] activity_splash.xml...")
splash_layout_path = "app/src/main/res/layout/activity_splash.xml"
if os.path.exists(splash_layout_path):
    splash_src = read(splash_layout_path)
    splash_src = re.sub(r'>v[0-9]+\.[0-9]+\.[0-9]+<', f'>v{VERSION_NAME}<', splash_src)
    splash_src = re.sub(
        r'android:text="v[0-9]+\.[0-9]+\.[0-9]+"',
        f'android:text="v{VERSION_NAME}"',
        splash_src
    )
    write(splash_layout_path, splash_src)

# ── 9. ExamActivity.java — patch EXIT_PIN ────────────────────────────────
print("\n[9] ExamActivity.java...")
exam_path = "app/src/main/java/id/emes/exambrowser/ExamActivity.java"
exam_src = read(exam_path)
exam_src = re.sub(
    r'static final String EXIT_PIN\s*=\s*"[^"]*"',
    f'static final String EXIT_PIN = "{EXIT_PIN}"',
    exam_src
)
write(exam_path, exam_src)

# ── 10. app/build.gradle — patch packageId + versionName ─────────────────
print("\n[10] app/build.gradle...")
app_gradle_path = "app/build.gradle"
app_gradle = read(app_gradle_path)
app_gradle = re.sub(r'applicationId\s+"[^"]+"', f'applicationId "{PACKAGE_ID}"', app_gradle)
app_gradle = re.sub(r'versionName\s+"[^"]+"', f'versionName "{VERSION_NAME}"', app_gradle)
write(app_gradle_path, app_gradle)

# ── 11. AndroidManifest.xml — patch package + label ──────────────────────
print("\n[11] AndroidManifest.xml...")
manifest_path = "app/src/main/AndroidManifest.xml"
manifest = read(manifest_path)
manifest = re.sub(r'package="[^"]+"', f'package="{PACKAGE_ID}"', manifest)
manifest = re.sub(r'android:label="[^"]+"', f'android:label="{APP_NAME}"', manifest, count=1)
write(manifest_path, manifest)

# ── 12. Rename Java package directory if needed ──────────────────────────
print("\n[12] Package rename (jika perlu)...")
OLD_PACKAGE_ID = "id.emes.exambrowser"
if PACKAGE_ID != OLD_PACKAGE_ID:
    old_dir = f"app/src/main/java/{OLD_PACKAGE_ID.replace('.', '/')}"
    new_dir = f"app/src/main/java/{PACKAGE_ID.replace('.', '/')}"
    if os.path.isdir(old_dir) and not os.path.isdir(new_dir):
        import shutil
        os.makedirs(os.path.dirname(new_dir), exist_ok=True)
        shutil.copytree(old_dir, new_dir)
        for root, dirs, files in os.walk(new_dir):
            for fname in files:
                if fname.endswith(".java"):
                    jf = os.path.join(root, fname)
                    src = read(jf)
                    src = src.replace(f"package {OLD_PACKAGE_ID};", f"package {PACKAGE_ID};")
                    src = src.replace(f"import {OLD_PACKAGE_ID}.", f"import {PACKAGE_ID}.")
                    write(jf, src)
        manifest2 = read(manifest_path)
        manifest2 = manifest2.replace(OLD_PACKAGE_ID, PACKAGE_ID)
        write(manifest_path, manifest2)
        print(f"  Package direname: {old_dir} -> {new_dir}")
    else:
        print(f"  Tidak perlu rename")
else:
    print(f"  Package sama, tidak perlu rename")

# ── VERIFIKASI AKHIR: pastikan tidak ada PNG corrupt di drawable ──────────
print("\n[FINAL] Verifikasi PNG di drawable...")
drawable_dir = "app/src/main/res/drawable"
for fname in os.listdir(drawable_dir):
    if fname.endswith(".png"):
        fpath = os.path.join(drawable_dir, fname)
        try:
            data = open(fpath, "rb").read()
            if not is_valid_image(data):
                os.remove(fpath)
                print(f"  HAPUS CORRUPT: {fpath}")
            else:
                print(f"  VALID: {fpath} ({len(data)//1024}KB)")
        except Exception as e:
            if os.path.exists(fpath):
                os.remove(fpath)
            print(f"  HAPUS ERROR: {fpath} - {e}")

print("\n" + "=" * 56)
print("SELESAI: Semua file di-patch. Siap untuk Gradle build.")
print("=" * 56)
