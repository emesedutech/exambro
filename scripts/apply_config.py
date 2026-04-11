#!/usr/bin/env python3
"""
apply_config.py — Patch Android source files sebelum Gradle build.
VERSI TOTAL: Mempertahankan 100% logika asli + Perbaikan Validasi Gambar.
"""

import os, re, base64, shutil, io
from PIL import Image

def env(key, default=""):
    return os.environ.get(key, default).strip()

# 1. AMBIL KONFIGURASI DARI ENV
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
print("EzamBro Config Patcher — Full & Safe Version")
print("=" * 56)

def read(path):
    if not os.path.exists(path): return ""
    with open(path, "r", encoding="utf-8") as f: return f.read()

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: f.write(content)

def is_valid_image(data):
    """Memastikan data adalah gambar valid sebelum disimpan."""
    if not data or len(data) < 100: return False
    try:
        img = Image.open(io.BytesIO(data))
        img.verify()
        return True
    except:
        return False

def save_base64_to_png(fpath, b64_str):
    """
    Menyimpan Base64 ke PNG.
    Jika input kosong/rusak, file lama DIHAPUS agar sistem fallback ke warna.
    """
    if not b64_str or len(b64_str) < 10:
        if os.path.exists(fpath):
            os.remove(fpath)
            print(f"  [CLEANUP] Menghapus {fpath} (menggunakan warna)")
        return

    try:
        if "," in b64_str:
            b64_str = b64_str.split(",")[1]
        img_data = base64.b64decode(b64_str)
        
        if is_valid_image(img_data):
            with open(fpath, "wb") as f:
                f.write(img_data)
            print(f"  [SUCCESS] Menyimpan gambar: {fpath}")
        else:
            if os.path.exists(fpath): os.remove(fpath)
            print(f"  [SKIP] Data gambar tidak valid, file dihapus: {fpath}")
    except Exception as e:
        if os.path.exists(fpath): os.remove(fpath)
        print(f"  [ERROR] Gagal menyimpan {fpath}: {e}")

# 2. PROSES RESOURCE
print("\n[1] Memproses Resource...")
save_base64_to_png("app/src/main/res/drawable/header_bg.png", HEADER_IMAGE)
save_base64_to_png("app/src/main/res/drawable/splash_bg.png", SPLASH_IMAGE)
if LOGO_IMAGE_env:
    save_base64_to_png("app/src/main/res/drawable/app_logo_custom.png", LOGO_IMAGE_env)

# Patch strings.xml
strings_path = "app/src/main/res/values/strings.xml"
s = read(strings_path)
if s:
    s = re.sub(r'<string name="app_name">.*?</string>', f'<string name="app_name">{APP_NAME}</string>', s)
    s = re.sub(r'<string name="title_text">.*?</string>', f'<string name="title_text">{APP_TITLE}</string>', s)
    s = re.sub(r'<string name="desc_text">.*?</string>', f'<string name="desc_text">{APP_DESCRIPTION}</string>', s)
    s = re.sub(r'<string name="exit_pin">.*?</string>', f'<string name="exit_pin">{EXIT_PIN}</string>', s)
    write(strings_path, s)

# Patch colors.xml
colors_path = "app/src/main/res/values/colors.xml"
c = read(colors_path)
if c:
    c = re.sub(r'<color name="colorPrimary">#.*?</color>', f'<color name="colorPrimary">#{HEADER_COLOR}</color>', c)
    c = re.sub(r'<color name="colorAccent">#.*?</color>', f'<color name="colorAccent">#{BUTTON_COLOR}</color>', c)
    write(colors_path, c)

# 3. PATCH BUILD.GRADLE & AMBIL OLD_PACKAGE_ID
print("\n[2] Patching app/build.gradle...")
gradle_path = "app/build.gradle"
gradle_content = read(gradle_path)
OLD_PACKAGE_ID = "id.emes.exambrowser" # Bawaan awal template

if gradle_content:
    match = re.search(r'applicationId "(.*?)"', gradle_content)
    if match:
        OLD_PACKAGE_ID = match.group(1)
    
    gradle_content = re.sub(r'applicationId ".*?"', f'applicationId "{PACKAGE_ID}"', gradle_content)
    gradle_content = re.sub(r'versionName ".*?"', f'versionName "{VERSION_NAME}"', gradle_content)
    write(gradle_path, gradle_content)
    print(f"  ID: {OLD_PACKAGE_ID} -> {PACKAGE_ID}")

# 4. REORGANISASI FOLDER PACKAGE (Logika Asli Anda)
print("\n[3] Reorganizing Package Folders...")
if OLD_PACKAGE_ID != PACKAGE_ID:
    base_java_dir = "app/src/main/java"
    old_dir = os.path.join(base_java_dir, OLD_PACKAGE_ID.replace(".", "/"))
    new_dir = os.path.join(base_java_dir, PACKAGE_ID.replace(".", "/"))

    if os.path.exists(old_dir):
        os.makedirs(new_dir, exist_ok=True)
        # Pindahkan file dari folder lama ke baru
        for item in os.listdir(old_dir):
            s_item = os.path.join(old_dir, item)
            d_item = os.path.join(new_dir, item)
            if os.path.isfile(s_item):
                shutil.move(s_item, d_item)
            elif os.path.isdir(s_item):
                shutil.copytree(s_item, d_item, dirs_exist_ok=True)
                shutil.rmtree(s_item)

        # Update deklarasi package di semua file .java
        for root, dirs, files in os.walk(new_dir):
            for file in files:
                if file.endswith(".java"):
                    fpath = os.path.join(root, file)
                    content = read(fpath)
                    content = content.replace(f"package {OLD_PACKAGE_ID}", f"package {PACKAGE_ID}")
                    content = content.replace(f"import {OLD_PACKAGE_ID}", f"import {PACKAGE_ID}")
                    write(fpath, content)
        
        # Hapus folder lama secara rekursif jika kosong
        curr = old_dir
        for _ in range(len(OLD_PACKAGE_ID.split("."))):
            if os.path.exists(curr) and not os.listdir(curr):
                os.rmdir(curr)
                curr = os.path.dirname(curr)
            else:
                break
        print(f"  Folder package berhasil dipindahkan.")

# 5. PATCH MANIFEST
print("\n[4] Patching AndroidManifest.xml...")
manifest_path = "app/src/main/AndroidManifest.xml"
m = read(manifest_path)
if m:
    m = m.replace(OLD_PACKAGE_ID, PACKAGE_ID)
    write(manifest_path, m)

print("\n" + "=" * 56)
print("PATCH SELESAI!")
print("=" * 56)
