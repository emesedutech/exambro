#!/usr/bin/env python3
"""Setup launcher icons dari app_logo.png atau fallback ic_launcher"""
import os
from PIL import Image

nodpi_logo = "app/src/main/res/drawable-nodpi/app_logo.png"
default_logo = "app/src/main/res/mipmap-hdpi/ic_launcher.png"

print(f"  drawable-nodpi exists: {os.path.exists('app/src/main/res/drawable-nodpi/')}")
print(f"  app_logo.png exists: {os.path.exists(nodpi_logo)}")
if os.path.exists(nodpi_logo):
    print(f"  app_logo.png size: {os.path.getsize(nodpi_logo)//1024}KB")

src_path = nodpi_logo if os.path.exists(nodpi_logo) else default_logo
print(f"  Using: {src_path}")

img = Image.open(src_path).convert("RGBA")
print(f"  Image mode: {img.mode}, size: {img.size}")

sizes = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}
for folder, size in sizes.items():
    base = f"app/src/main/res/{folder}"
    os.makedirs(base, exist_ok=True)
    resized = img.resize((size, size), Image.LANCZOS)
    resized.save(f"{base}/ic_launcher.png")
    resized.save(f"{base}/ic_launcher_round.png")
    print(f"  ✓ {folder}/ic_launcher.png ({size}x{size})")
