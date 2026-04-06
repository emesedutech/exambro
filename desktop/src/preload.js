const { ipcRenderer } = require('electron');

// Expose fungsi exit ke halaman web
window.__exambroExit = function(pin) {
  ipcRenderer.send('exit-request', pin);
};

// Dengarkan balasan PIN salah
ipcRenderer.on('exit-wrong-pin', () => {
  const input = document.getElementById('exambro-pin-input');
  if (input) {
    input.style.borderColor = '#EF4444';
    input.value = '';
    input.placeholder = 'PIN salah!';
    setTimeout(() => {
      input.style.borderColor = 'rgba(255,255,255,0.2)';
      input.placeholder = 'PIN Pengawas';
    }, 1500);
  }
});

// Blokir klik kanan
document.addEventListener('contextmenu', e => e.preventDefault());

// Blokir select all dan copy
document.addEventListener('keydown', e => {
  if (e.ctrlKey && ['a','c','v','x','u','s','p'].includes(e.key.toLowerCase())) {
    e.preventDefault();
  }
});
