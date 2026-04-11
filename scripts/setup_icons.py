#!/usr/bin/env python3
"""
setup_icons.py — Generate launcher icons dari app_logo.png (jika ada)
atau buat placeholder dari ic_launcher.png yang sudah ada.
Menggunakan Pillow untuk resize ke semua density yang dibutuhkan.
"""

import os, sys, shutil

DRAWABLE_PATH = "app/src/main/res/drawable"
APP_LOGO_PATH = os.path.join(DRAWABLE_PATH, "app_logo.png")

# Ukuran icon per density (dalam pixel)
DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi":192,
}

try:
    from PIL import Image

    # Tentukan sumber icon
    if os.path.exists(APP_LOGO_PATH):
        src_path = APP_LOGO_PATH
        print(f"  ✓ Pakai logo user: {src_path}")
    else:
        # Fallback: pakai ic_launcher hdpi yang sudah ada
        src_path = "app/src/main/res/mipmap-hdpi/ic_launcher.png"
        if not os.path.exists(src_path):
            print(f"  ⚠ Tidak ada sumber icon — skip resize")
            sys.exit(0)
        print(f"  ✓ Pakai ic_launcher default: {src_path}")

    img = Image.open(src_path).convert("RGBA")

    for density, size in DENSITIES.items():
        out_dir = f"app/src/main/res/{density}"
        os.makedirs(out_dir, exist_ok=True)
        resized = img.resize((size, size), Image.LANCZOS)
        resized.save(f"{out_dir}/ic_launcher.png")
        resized.save(f"{out_dir}/ic_launcher_round.png")
        print(f"  ✓ {density}: {size}x{size}px")

    print("✅ Launcher icons berhasil di-generate.")

except ImportError:
    # Pillow tidak tersedia — fallback ke copy manual
    print("  ⚠ Pillow tidak tersedia — fallback ke copy ic_launcher hdpi")
    src = "app/src/main/res/mipmap-hdpi/ic_launcher.png"
    if os.path.exists(src):
        for density in DENSITIES:
            out_dir = f"app/src/main/res/{density}"
            os.makedirs(out_dir, exist_ok=True)
            shutil.copy2(src, f"{out_dir}/ic_launcher.png")
            shutil.copy2(src, f"{out_dir}/ic_launcher_round.png")
        print("✅ Icons di-copy (tanpa resize).")
    else:
        print("  ⚠ Tidak ada sumber icon sama sekali — skip")

except Exception as e:
    print(f"  ⚠ Error setup icons: {e}")
    sys.exit(1)
