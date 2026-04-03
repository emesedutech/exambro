# EzamBro APK Builder 🛡🚀

**Safe Exam Browser Android dengan APK Builder berbasis GitHub Pages.**

Atur semua konfigurasi dari halaman web, klik tombol, APK siap dalam ~3 menit — tanpa perlu membuka Android Studio.

---

## ✨ Fitur

### Aplikasi Android
- ✅ Blokir tombol **Back**, **Home**, dan **App Switcher**
- ✅ Mode fullscreen penuh selama ujian
- ✅ Keluar hanya dengan **PIN pengawas** (dikonfigurasi via builder)
- ✅ Scan **QR Code** URL ujian
- ✅ Progress bar loading halaman
- ✅ Layar tidak mati otomatis
- ✅ Tanpa iklan

### APK Builder (Halaman Pengaturan)
- 🎨 Atur warna header dan tombol dengan color picker
- ✏️ Ubah nama aplikasi, judul, dan subtitle
- 🔒 Set PIN pengawas langsung dari browser
- 📦 Ganti Package ID untuk identitas unik APK
- 🚀 **Trigger GitHub Actions build dari browser** — tanpa Android Studio!
- 📊 Monitor status build real-time di halaman yang sama
- 💾 Simpan konfigurasi di browser (localStorage)
- ⬇️ Export `config.json`, `strings.xml`, `colors.xml`

---

## 🚀 Cara Pakai

### 1. Fork & Setup Repository
```
Fork repo ini ke akun GitHub Anda
```

### 2. Aktifkan GitHub Pages
1. Buka **Settings → Pages**
2. Source: **Deploy from a branch → main → /docs**
3. Akses di: `https://<username>.github.io/<repo>/`

### 3. Buat Personal Access Token
1. GitHub → **Settings → Developer settings → Personal access tokens (classic)**
2. **Generate new token (classic)**
3. Centang scope: `repo` (private) atau `public_repo` (public)
4. Salin token

### 4. Gunakan APK Builder
1. Buka halaman GitHub Pages Anda
2. Isi **Token**, **Username**, dan **Nama Repo** di bagian "Koneksi GitHub"
3. Atur nama, warna, dan PIN
4. Klik **🚀 Build APK Sekarang**
5. Pantau status build di halaman yang sama
6. Unduh APK dari link Artifacts setelah selesai (~3 menit)

---

## 📁 Struktur Project

```
ezambro/
├── .github/workflows/
│   └── build.yml              ← Workflow GitHub Actions (terima inputs)
├── scripts/
│   └── apply_config.py        ← Patcher otomatis sebelum Gradle build
├── docs/
│   └── index.html             ← Halaman pengaturan (GitHub Pages)
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/id/emes/exambrowser/
│           ├── MainActivity.java
│           ├── ExamActivity.java      ← EXIT_PIN dipatch di sini
│           ├── QRScanActivity.java
│           └── ExamDeviceAdmin.java
├── build.gradle
└── settings.gradle
```

---

## ⚙️ Cara Kerja Builder

```
Browser (GitHub Pages)
    │
    │  workflow_dispatch + inputs
    ▼
GitHub Actions API
    │
    ▼
build.yml workflow
    │  Jalankan apply_config.py
    │  (patch strings.xml, colors.xml, ExamActivity.java, app/build.gradle)
    │
    ▼
Gradle assembleDebug
    │
    ▼
APK → upload ke Artifacts (unduh dari Actions tab)
```

---

## 🔒 Keamanan Token

- Token GitHub **hanya disimpan di `localStorage` browser Anda**
- Token **tidak** dikirim ke server manapun selain `api.github.com`
- Gunakan token dengan scope minimal yang diperlukan
- Hapus token jika tidak dipakai lagi

---

Dibuat dengan ❤️ untuk kemudahan ujian online tanpa iklan.
