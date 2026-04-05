const { app, BrowserWindow, globalShortcut, ipcMain, dialog, session } = require('electron');
const path = require('path');
const fs = require('fs');

// Baca config
const configPath = path.join(__dirname, '..', 'config.json');
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

let mainWindow = null;
let splashWindow = null;
let examUrl = null;
let isLocked = false;

// ── Cegah multiple instance ─────────────────────────────────────────────────
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      mainWindow.focus();
      if (isLocked) lockWindow();
    }
  });
}

// ── Buat splash window ──────────────────────────────────────────────────────
function createSplash() {
  splashWindow = new BrowserWindow({
    width: 480,
    height: 640,
    frame: false,
    alwaysOnTop: true,
    resizable: false,
    center: true,
    skipTaskbar: false,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
    }
  });
  splashWindow.loadFile(path.join(__dirname, 'splash.html'));
}

// ── Buat main exam window ───────────────────────────────────────────────────
function createMainWindow(url) {
  examUrl = url;
  isLocked = true;

  mainWindow = new BrowserWindow({
    width: 1366,
    height: 768,
    frame: false,
    fullscreen: true,
    alwaysOnTop: true,
    skipTaskbar: false,
    kiosk: true,
    resizable: false,
    movable: false,
    minimizable: false,
    maximizable: false,
    closable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
      devTools: false,
      webSecurity: true,
    }
  });

  mainWindow.loadURL(url);
  mainWindow.setFullScreen(true);
  mainWindow.setAlwaysOnTop(true, 'screen-saver');
  mainWindow.setVisibleOnAllWorkspaces(true);

  // Blokir dev tools
  mainWindow.webContents.on('devtools-opened', () => {
    mainWindow.webContents.closeDevTools();
  });

  // Blokir navigation keluar dari URL ujian
  mainWindow.webContents.on('will-navigate', (event, navUrl) => {
    const allowed = navUrl.startsWith(examUrl) ||
                    navUrl.startsWith(new URL(examUrl).origin);
    if (!allowed) event.preventDefault();
  });

  // Blokir new window
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));

  lockWindow();
  registerShortcutBlocker();

  // Tampilkan PIN bar di atas
  showPinBar();
}

// ── Lock window - paksa fullscreen ──────────────────────────────────────────
function lockWindow() {
  if (!mainWindow) return;
  mainWindow.setFullScreen(true);
  mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  mainWindow.focus();
}

// ── Tampilkan PIN overlay di atas webview ───────────────────────────────────
function showPinBar() {
  if (!mainWindow) return;
  mainWindow.webContents.executeJavaScript(`
    (function() {
      if (document.getElementById('exambro-pin-bar')) return;
      const bar = document.createElement('div');
      bar.id = 'exambro-pin-bar';
      bar.style.cssText = \`
        position: fixed; top: 0; left: 0; right: 0; z-index: 2147483647;
        background: rgba(10,20,40,0.92); backdrop-filter: blur(8px);
        padding: 6px 16px; display: flex; align-items: center;
        justify-content: space-between; font-family: sans-serif;
        border-bottom: 1px solid rgba(255,255,255,0.1);
        height: 40px; box-sizing: border-box;
      \`;
      bar.innerHTML = \`
        <span style="color:#94A3B8;font-size:11px;font-weight:600;letter-spacing:1px;">
          🔒 SECURE EXAM MODE
        </span>
        <div style="display:flex;align-items:center;gap:8px;">
          <input id="exambro-pin-input" type="password" maxlength="10"
            placeholder="PIN Pengawas"
            style="background:rgba(255,255,255,0.1);border:1px solid rgba(255,255,255,0.2);
            border-radius:6px;padding:4px 10px;color:#fff;font-size:12px;width:130px;outline:none;"
          />
          <button id="exambro-exit-btn"
            style="background:#EF4444;border:none;border-radius:6px;
            color:#fff;padding:4px 14px;font-size:12px;font-weight:600;cursor:pointer;">
            Keluar
          </button>
        </div>
      \`;
      document.body.appendChild(bar);
      document.body.style.paddingTop = '40px';

      document.getElementById('exambro-exit-btn').onclick = function() {
        const pin = document.getElementById('exambro-pin-input').value;
        window.__exambroExit(pin);
      };
      document.getElementById('exambro-pin-input').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
          window.__exambroExit(this.value);
        }
      });
    })();
  `);
}

// ── Blokir semua shortcut berbahaya ─────────────────────────────────────────
function registerShortcutBlocker() {
  const blocked = [
    'Alt+F4', 'Alt+Tab', 'Alt+F10',
    'Super', 'Meta',
    'Ctrl+W', 'Ctrl+Q', 'Ctrl+R', 'Ctrl+F5',
    'Ctrl+Alt+Delete', 'Ctrl+Alt+Escape',
    'Ctrl+Shift+Esc', 'Ctrl+Shift+I', 'Ctrl+Shift+J',
    'Ctrl+U', 'Ctrl+S', 'Ctrl+P',
    'F1', 'F2', 'F3', 'F4', 'F5', 'F6',
    'F7', 'F8', 'F9', 'F10', 'F11', 'F12',
    'PrintScreen', 'Escape',
  ];

  blocked.forEach(shortcut => {
    try {
      globalShortcut.register(shortcut, () => {
        if (isLocked) lockWindow();
      });
    } catch (e) {}
  });
}

// ── IPC: exit dengan PIN ─────────────────────────────────────────────────────
ipcMain.on('exit-request', (event, pin) => {
  if (pin === String(config.exitPin)) {
    isLocked = false;
    globalShortcut.unregisterAll();
    mainWindow.destroy();
    app.quit();
  } else {
    event.reply('exit-wrong-pin');
  }
});

// ── App ready ────────────────────────────────────────────────────────────────
app.whenReady().then(() => {
  // Disable hardware acceleration issues
  app.commandLine.appendSwitch('disable-features', 'OutOfBlinkCors');

  createSplash();

  // IPC dari splash: user klik mulai ujian
  ipcMain.on('start-exam', (event, url) => {
    if (splashWindow) {
      splashWindow.close();
      splashWindow = null;
    }
    createMainWindow(url);
  });
});

// ── Cegah quit normal ───────────────────────────────────────────────────────
app.on('before-quit', (event) => {
  if (isLocked) event.preventDefault();
});

app.on('window-all-closed', () => {
  if (!isLocked) app.quit();
});

// ── Pastikan window selalu di depan saat locked ──────────────────────────────
setInterval(() => {
  if (isLocked && mainWindow && !mainWindow.isDestroyed()) {
    if (!mainWindow.isFocused()) {
      mainWindow.focus();
      lockWindow();
    }
  }
}, 1000);
