#!/usr/bin/env python3
"""
apply_config_desktop.py — Patch desktop Electron app sebelum build.
Membaca environment variables dari GitHub Actions workflow_dispatch.
"""

import os, re, sys, base64, json

def env(key, default=""):
    return os.environ.get(key, default).strip()

APP_NAME        = env("APP_NAME",        "Emes Exam Browser")
APP_TITLE       = env("APP_TITLE",       "Emes Exam Browser")
APP_DESCRIPTION = env("APP_DESCRIPTION", "Safe browsing untuk ujian online")
EXIT_PIN        = env("EXIT_PIN",        "1234")
HEADER_COLOR    = env("HEADER_COLOR",    "1A3A5C").lstrip("#")
BUTTON_COLOR    = env("BUTTON_COLOR",    "2E7BF6").lstrip("#")
VERSION_NAME    = env("VERSION_NAME",    "1.1.0")
PACKAGE_ID      = env("PACKAGE_ID",      "id.emes.exambrowser")
LOGO_IMAGE      = env("LOGO_IMAGE",      "")

print("=" * 56)
print("🖥  ExamBro Desktop Config Patcher")
print("=" * 56)
print(f"  App Name   : {APP_NAME}")
print(f"  Exit PIN   : {'*' * len(EXIT_PIN)}")
print(f"  Header     : #{HEADER_COLOR}")
print(f"  Button     : #{BUTTON_COLOR}")
print(f"  Version    : {VERSION_NAME}")
print("=" * 56)

# ── 1. config.json ─────────────────────────────────────────────────────────
config = {
    "appName": APP_NAME,
    "appTitle": APP_TITLE,
    "appDescription": APP_DESCRIPTION,
    "exitPin": EXIT_PIN,
    "headerColor": f"#{HEADER_COLOR}",
    "buttonColor": f"#{BUTTON_COLOR}",
    "version": VERSION_NAME
}
with open("desktop/config.json", "w") as f:
    json.dump(config, f, indent=2)
print("  ✓ desktop/config.json")

# ── 2. patch package.json — appId + productName + version ──────────────────
pkg_path = "desktop/package.json"
with open(pkg_path) as f:
    pkg = json.load(f)

pkg["version"] = VERSION_NAME
pkg["build"]["appId"] = PACKAGE_ID
pkg["build"]["productName"] = APP_NAME
pkg["build"]["win"]["target"][0]["target"] = "nsis"
pkg["build"]["nsis"]["shortcutName"] = APP_NAME

with open(pkg_path, "w") as f:
    json.dump(pkg, f, indent=2)
print("  ✓ desktop/package.json")

# ── 3. Patch warna di splash.html ──────────────────────────────────────────
splash_path = "desktop/src/splash.html"
with open(splash_path) as f:
    splash = f.read()

splash = re.sub(r'background:\s*CONFIG_HEADER_COLOR', f'background: #{HEADER_COLOR}', splash)
splash = re.sub(r'color:\s*CONFIG_BUTTON_COLOR', f'color: #{BUTTON_COLOR}', splash)
splash = re.sub(r"background:\s*CONFIG_BUTTON_COLOR", f'background: #{BUTTON_COLOR}', splash)

with open(splash_path, "w") as f:
    f.write(splash)
print("  ✓ desktop/src/splash.html")

# ── 4. Logo → assets/logo.png ──────────────────────────────────────────────
os.makedirs("desktop/assets", exist_ok=True)

# Cek apakah logo sudah ada di drawable-nodpi (dari commit web)
nodpi_logo = "app/src/main/res/drawable-nodpi/app_logo.png"
desktop_logo = "desktop/assets/logo.png"

if LOGO_IMAGE.strip():
    try:
        b64 = LOGO_IMAGE.strip()
        if "," in b64:
            b64 = b64.split(",", 1)[1]
        with open(desktop_logo, "wb") as f:
            f.write(base64.b64decode(b64))
        print("  ✓ desktop/assets/logo.png (dari input)")
    except Exception as e:
        print(f"  ⚠ Gagal decode logo: {e}")
elif os.path.exists(nodpi_logo):
    import shutil
    shutil.copy(nodpi_logo, desktop_logo)
    print("  ✓ desktop/assets/logo.png (dari drawable-nodpi)")
else:
    print("  ℹ Tidak ada logo — pakai default")

# ── 5. Convert logo PNG → ICO untuk Windows ────────────────────────────────
ico_path = "desktop/assets/icon.ico"
if os.path.exists(desktop_logo):
    try:
        from PIL import Image
        img = Image.open(desktop_logo).convert("RGBA")
        # ICO butuh beberapa ukuran
        sizes = [(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)]
        icons = [img.resize(s, Image.LANCZOS) for s in sizes]
        icons[0].save(ico_path, format='ICO', sizes=sizes,
                      append_images=icons[1:])
        print(f"  ✓ desktop/assets/icon.ico")
    except Exception as e:
        print(f"  ⚠ Gagal buat ICO: {e}")

print()
print("✅ Desktop config selesai di-patch.")
