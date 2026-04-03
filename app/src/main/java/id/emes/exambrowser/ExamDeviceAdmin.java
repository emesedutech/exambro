package id.emes.exambrowser;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Device Admin Receiver — diperlukan agar startLockTask() tanpa perlu
 * user menekan tombol "Pin this screen" secara manual.
 * Jika app bukan Device Owner, lock task tetap berjalan tapi user
 * bisa keluar via hold-back + hold-recent (Screen Pinning biasa).
 */
public class ExamDeviceAdmin extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) { }

    @Override
    public void onDisabled(Context context, Intent intent) { }
}
