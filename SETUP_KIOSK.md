# Setup Kiosk Mode Penuh (Opsional)

Secara default, app sudah menggunakan `startLockTask()` yang akan memblokir:
- Tombol Recent Apps
- Tombol Home  
- Notifikasi
- Status bar

## Mode 1: Screen Pinning (Tanpa ADB - sudah otomatis)
App memanggil `startLockTask()` saat ujian dimulai. Pada perangkat bukan Device Owner,
Android akan menampilkan konfirmasi Screen Pinning ke user (pengawas bisa konfirmasi sekali).

## Mode 2: Device Owner (Full Kiosk - tanpa konfirmasi apapun)
Jalankan perintah ADB berikut SEKALI pada perangkat ujian (sebelum distribusi):

```bash
adb shell dpm set-device-owner id.emes.exambrowser/.ExamDeviceAdmin
```

Setelah itu app akan otomatis masuk kiosk mode PENUH tanpa konfirmasi apapun.
User tidak bisa keluar sama sekali kecuali input PIN pengawas.

## Reset Device Owner (jika diperlukan)
```bash
adb shell dpm remove-active-admin id.emes.exambrowser/.ExamDeviceAdmin
```
