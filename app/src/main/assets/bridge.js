// bridge.js - Glue between WebView and Java

const CueBridge = window.CueBridge || {
    saveRoutine: (r) => console.log("Mock Save", r),
    getRoutines: () => "[]",
    getApps: () => JSON.stringify([
        {name:'Camera',pkg:'com.android.camera',icon:'📷'},
        {name:'Messages',pkg:'com.android.messages',icon:'💬'}
    ]),
    getSettings: () => "{}",
    saveSettings: (s) => console.log("Mock Save Settings", s),
    requestUsageAccess: () => console.log("Mock Usage Access"),
    requestAccessibilitySettings: () => console.log("Mock Acc Access"),
    requestNotificationPermission: () => console.log("Mock Notif Access"),
    requestBatteryIgnore: () => console.log("Mock Battery Ignore"),
    requestBootStart: () => console.log("Mock Boot Start"),
    rescanApps: () => console.log("Mock Rescan"),
    deleteRoutine: (id) => console.log("Mock Delete", id),
    toggleRoutine: (id, enabled) => console.log("Mock Toggle", id, enabled),
    checkPermissionsStatus: () => '{"usage":false,"accessibility":false,"notifications":false}',
    log: (m) => console.log(m),
    openAppInfo: () => console.log("Mock Open App Info")
};

const V = '1.6.0', MAX_MSG = 100;
const BUILD_DATE = '2026-02-22';
let ST = { routines: [], iconLib: [], settings: { defaultBubble: false, highPriority: true, design: '2', theme: 'default', lastSeqId: 0 } };
let CR = { step: 0, selected: [], multi: false, cond: 'launched', dur: 20, unit: 'm', timeMode: 'session', icon: null, titleOn: false, title: '', msg: '', bubble: false, timeout: 0, cueName: '' };
let APPS = [];

const PRESETS = [
    { e: '🔔', l: 'Bell' }, { e: '🚀', l: 'Launch' }, { e: '⚡', l: 'Flash' }, { e: '🎯', l: 'Target' },
    { e: '🔥', l: 'Fire' }, { e: '💡', l: 'Idea' }, { e: '🎮', l: 'Game' }, { e: '🏃', l: 'Run' },
    { e: '📱', l: 'Phone' }, { e: '🌙', l: 'Night' }, { e: '☀️', l: 'Day' }, { e: '🔕', l: 'Silent' },
    { e: '💼', l: 'Work' }, { e: '🏠', l: 'Home' }, { e: '🎵', l: 'Music' }, { e: '📍', l: 'Place' },
    { e: '⏱️', l: 'Timer' }, { e: '🔐', l: 'Lock' }, { e: '👁️', l: 'Watch' }, { e: '✨', l: 'Shine' },
];

const THEMES = [
    { id: 'default', label: 'Dark', bg: '#111318', a: '#4fc3f7', b: '#7c4dff' },
    { id: 'light', label: 'Light', bg: '#ffffff', a: '#1976d2', b: '#6200ea' },
    { id: 'aurora', label: 'Aurora', bg: '#0f1117', a: '#a78bfa', b: '#34d399' },
    { id: 'sunset', label: 'Sunset', bg: '#150c10', a: '#fb923c', b: '#e11d48' },
    { id: 'ocean', label: 'Ocean', bg: '#0a1620', a: '#22d3ee', b: '#0ea5e9' },
    { id: 'forest', label: 'Forest', bg: '#0c150c', a: '#4ade80', b: '#16a34a' },
    { id: 'candy', label: 'Candy', bg: '#160c18', a: '#f9a8d4', b: '#c084fc' },
    { id: 'lava', label: 'Lava', bg: '#1a0800', a: '#ff6b35', b: '#ff3d00' },
];

const DESIGNS = [
    { id: '1', label: 'Bold', desc: 'Rounded cards, vibrant accents' },
    { id: '2', label: 'Minimal', desc: 'Flat lines, restrained type' },
];

// ── PERSISTENCE (DELEGATED TO JAVA) ──
function save() {
    CueBridge.saveSettings(JSON.stringify(ST.settings));
}

function load() {
    try {
        const routinesJson = CueBridge.getRoutines();
        const settingsJson = CueBridge.getSettings();

        ST.routines = JSON.parse(routinesJson || "[]");
        const settings = JSON.parse(settingsJson || "{}");
        ST.settings = Object.assign(ST.settings, settings);
        // APPS will be loaded via onAppsUpdated callback
    } catch (e) {
        console.warn('Load Error: ' + e);
    }
}

// ── JAVA CALLBACKS ──
window.onRoutinesUpdated = function (json) {
    ST.routines = JSON.parse(json);
    if (curTab === 'list') renderList();
};

window.onAppsUpdated = function (json) {
    APPS = JSON.parse(json);
    window.appsLoading = false;
    if (curTab === 'create' && CR.step === 0) renderCreate();
};

window.checkPermissions = function() {
    try {
        const status = JSON.parse(CueBridge.checkPermissionsStatus());
        if (!status.usage || !status.accessibility) {
            showPermissionWarning(status);
        } else if (status.accessibility && status.lastAccEvent === 0 && status.hasActiveRoutines) {
            showAccessibilityUnusableWarning();
        }
        if (curTab === 'settings') renderSettings();
    } catch(e) { console.error("Error checking permissions: " + e); }
};

function showAccessibilityUnusableWarning() {
    const html = `
    <div style="padding:24px 20px; text-align:center">
        <div style="font-size:48px; margin-bottom:18px">🚩</div>
        <h3 style="margin-bottom:12px; color:var(--danger)">Service Unresponsive</h3>
        <p style="font-size:13px; color:var(--txt2); line-height:1.5; margin-bottom:20px">
            Android shows Accessibility is ON, but it's not sending data. This is a known Android bug.
            To fix this, you must <b>Force Stop</b> the app and then toggle the service OFF and ON again.
        </p>
        <div class="sec">
            <div class="row" onclick="CueBridge.openAppInfo()">
                <div class="row-label">1. Go to App Info</div>
                <span class="badge badge-req">Open</span>
            </div>
            <div class="row" style="opacity:0.6; pointer-events:none">
                <div class="row-label">2. Click "Force Stop"</div>
            </div>
            <div class="row" onclick="CueBridge.requestAccessibilitySettings()">
                <div class="row-label">3. Toggle Service</div>
                <span class="badge badge-opt">Settings</span>
            </div>
        </div>
        <button class="btn btn-primary" style="margin-top:20px" onclick="closeSheet()">I'll try later</button>
    </div>`;
    openSheet('Warning', html);
}

function showPermissionWarning(status) {
    const html = `
    <div style="padding:24px 20px; text-align:center">
        <div style="font-size:48px; margin-bottom:18px">⚠️</div>
        <h3 style="margin-bottom:12px">Permissions Required</h3>
        <p style="font-size:13px; color:var(--txt2); line-height:1.5; margin-bottom:20px">
            CueAside needs certain permissions to watch for app changes and fire notifications.
            Without them, the app will not function correctly.
        </p>
        <div class="sec">
            ${!status.usage ? `
            <div class="row" onclick="CueBridge.requestUsageAccess()">
                <div class="row-label">Usage Access</div>
                <span class="badge badge-req">Grant</span>
            </div>` : ''}
            ${!status.accessibility ? `
            <div class="row" onclick="CueBridge.requestAccessibilitySettings()">
                <div class="row-label">Accessibility Service</div>
                <span class="badge badge-req">Enable</span>
            </div>` : ''}
        </div>
        <button class="btn btn-primary" style="margin-top:20px" onclick="closeSheet()">I'll do it later</button>
    </div>`;
    openSheet('Warning', html);
}

// ── THEME & DESIGN ──
function applyTheme(id) {
    document.documentElement.setAttribute('data-theme', id === 'default' ? '' : id);
    ST.settings.theme = id;
}
function setTheme(id) {
    applyTheme(id);
    save();
    renderSettings();
}

function applyDesign(id) {
    document.documentElement.setAttribute('data-design', id);
    ST.settings.design = id;
    if (id === '1' && !document.getElementById('font-nunito')) {
        const l = document.createElement('link');
        l.id = 'font-nunito'; l.rel = 'stylesheet';
        l.href = 'https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;900&family=JetBrains+Mono:wght@400;700&display=swap';
        document.head.appendChild(l);
    }
}
function switchDesign(id) {
    if (id === ST.settings.design) return;
    const p = document.getElementById('design-plaster');
    p.classList.add('show');
    setTimeout(() => {
        applyDesign(id);
        save();
        renderSettings();
        setTimeout(() => p.classList.remove('show'), 220);
    }, 320);
}

// ── UI UTILS ──

let _st;
function snack(m) {
    const el = document.getElementById('snack');
    el.textContent = m; el.classList.add('show');
    clearTimeout(_st); _st = setTimeout(() => el.classList.remove('show'), 2400);
}

// ── NAVIGATION ──
let curTab = 'create';
function switchTab(t) {
    if (t === curTab) return;
    document.querySelector('.nav-item.active')?.classList.remove('active');
    document.querySelector(`[data-tab="${t}"]`).classList.add('active');
    document.getElementById('scr-' + curTab).classList.remove('active');
    document.getElementById('scr-' + t).classList.add('active');
    curTab = t;
    if (t === 'list') renderList();
    if (t === 'settings') renderSettings();
    hideFab();
}

function showFab(l) { const f = document.getElementById('fab'); f.textContent = l || '→'; f.classList.remove('hidden'); }
function hideFab() { document.getElementById('fab').classList.add('hidden'); }
function onFab() { if (curTab === 'create') { if (CR.step === 0) proceedFn(); else saveRoutine(); } }

function openSheet(title, html) {
    document.getElementById('sheet-title').textContent = title;
    document.getElementById('sheet-body').innerHTML = html;
    document.getElementById('sheet-overlay').classList.add('open');
}
function closeSheet() { document.getElementById('sheet-overlay').classList.remove('open'); }
function closeSheetOutside(e) { if (e.target === document.getElementById('sheet-overlay')) closeSheet(); }

// ── CREATE ROUTINE ──
function renderCreate() {
    document.getElementById('create-content').innerHTML = CR.step === 0 ? renderAppSel() : renderFnCfg();
}

function renderAppSel() {
    const c = CR.selected.length;
    return `<div class="step-bar"><div class="step-dot done"></div><div class="step-dot"></div></div>
  <div style="padding:12px 20px;border-bottom:var(--line);display:flex;gap:8px;align-items:center">
    <span class="chip${!CR.multi ? ' on' : ''}" onclick="setMode(false)" style="cursor:pointer;flex:1;justify-content:center">Single</span>
    <span class="chip${CR.multi ? ' on' : ''}" onclick="setMode(true)" style="cursor:pointer;flex:1;justify-content:center">Multi</span>
    <span style="font-size:11px;color:var(--txt3);flex-shrink:0">${c} selected</span>
  </div>
  <div class="search-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    <input id="app-search" placeholder="Search apps" oninput="filterApps(this.value)" autocomplete="off">
  </div>
  <div id="app-list">${APPS.length ? APPS.map(a => appRow(a)).join('') : (window.appsLoading ? '<div class="empty">Loading apps...</div>' : '<div class="empty">No apps found. Try rescan in settings.</div>')}</div>`;
}

function appRow(a) {
    const sel = CR.selected.some(s => s.pkg === a.pkg);
    return `<div class="app-item${sel ? ' selected' : ''}" onclick="toggleApp('${a.pkg}')" data-pkg="${a.pkg}">
    <div class="app-icon-box">${a.icon.startsWith('data:') ? `<img src="${a.icon}" style="width:28px;height:28px">` : a.icon}</div>
    <div class="app-info"><div class="app-name">${a.name}</div><div class="app-pkg">${a.pkg}</div></div>
    <div class="app-check"><svg class="app-chk-svg" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg></div>
  </div>`;
}

function setMode(m) {
    CR.multi = m;
    if (!m && CR.selected.length > 1) CR.selected = [CR.selected[0]];
    renderCreate(); updateFab();
}

function toggleApp(pkg) {
    const app = APPS.find(a => a.pkg === pkg);
    const idx = CR.selected.findIndex(s => s.pkg === pkg);
    if (idx >= 0) { CR.selected.splice(idx, 1); }
    else {
        if (!CR.multi) CR.selected = [];
        CR.selected.push(app);
        if (!CR.multi) { proceedFn(); return; }
    }
    renderCreate(); updateFab();
}

function filterApps(q) {
    const ql = q.toLowerCase();
    document.querySelectorAll('#app-list .app-item').forEach(r => {
        const name = r.querySelector('.app-name').textContent.toLowerCase();
        const pkg = r.querySelector('.app-pkg').textContent.toLowerCase();
        r.style.display = (name.includes(ql) || pkg.includes(ql)) ? '' : 'none';
    });
}

function updateFab() {
    if (CR.step === 0 && CR.selected.length > 0 && CR.multi) showFab('Next →');
    else if (CR.step === 1) showFab('Save');
    else hideFab();
}

function proceedFn() {
    if (!CR.selected.length) { snack('Select at least one app'); return; }
    CR.step = 1; renderCreate(); showFab('Save');
}

function renderFnCfg() {
    const apps = CR.selected;
    const appIcons = apps.map(a => `
    <div class="icon-opt${CR.icon?.type === 'app' && CR.icon?.pkg === a.pkg ? ' sel' : ''}" onclick="pickIcon('app','${a.pkg}')">
      <div class="ico">${a.icon.startsWith('data:') ? `<img src="${a.icon}" style="width:28px;height:28px">` : a.icon}</div><span>${a.name}</span>
    </div>`).join('');

    return `<div class="step-bar"><div class="step-dot done"></div><div class="step-dot done"></div></div>
  <div style="padding:12px 20px;border-bottom:var(--line)">
    <button onclick="backToSel()" class="btn-ghost" style="width:auto;padding:6px 14px;font-size:11px">← Back to apps</button>
  </div>
  <div style="padding:12px 20px;border-bottom:var(--line);display:flex;flex-wrap:wrap;gap:6px">
    ${apps.map(a => `<span class="chip on">${a.icon.startsWith('data:') ? `<img src="${a.icon}" style="width:16px;height:16px">` : a.icon} ${a.name}</span>`).join('')}
  </div>
  <div style="padding:10px 20px 4px;font-size:10px;font-weight:500;letter-spacing:.1em;text-transform:uppercase;color:var(--txt3)">Condition</div>
  <label class="chk-row${CR.cond === 'launched' ? ' sel' : ''}"><input type="radio" name="cond" value="launched" ${CR.cond === 'launched' ? 'checked' : ''} onchange="setCond('launched')"><span class="row-label">When Launched</span></label>
  <label class="chk-row${CR.cond === 'used' ? ' sel' : ''}"><input type="radio" name="cond" value="used" ${CR.cond === 'used' ? 'checked' : ''} onchange="setCond('used')"><span class="row-label">Time-based</span></label>
  <div id="used-sub" style="display:${CR.cond === 'used' ? 'block' : 'none'}">
    <div style="padding:0 20px 10px;display:flex;gap:6px">
        <div onclick="CR.timeMode='session';renderCreate();showFab('Save')" style="flex:1;padding:10px 8px;border-radius:var(--rad2);border:1px solid ${CR.timeMode === 'session' ? 'var(--acc)' : 'var(--bg4)'};background:${CR.timeMode === 'session' ? 'var(--acc-dim)' : 'var(--bg3)'};cursor:pointer;text-align:center">
            <div style="font-size:12px;font-weight:600;color:${CR.timeMode === 'session' ? 'var(--acc)' : 'var(--txt)'};margin-bottom:3px">Session</div>
        </div>
        <div onclick="CR.timeMode='total';renderCreate();showFab('Save')" style="flex:1;padding:10px 8px;border-radius:var(--rad2);border:1px solid ${CR.timeMode === 'total' ? 'var(--acc)' : 'var(--bg4)'};background:${CR.timeMode === 'total' ? 'var(--acc-dim)' : 'var(--bg3)'};cursor:pointer;text-align:center">
            <div style="font-size:12px;font-weight:600;color:${CR.timeMode === 'total' ? 'var(--acc)' : 'var(--txt)'};margin-bottom:3px">Total (24h)</div>
        </div>
    </div>
    <div style="padding:0 20px 12px;display:flex;align-items:center;gap:8px">
      <input id="dur-v" type="number" min="1" value="${CR.dur}" class="form-input" style="width:68px" oninput="CR.dur=+this.value">
      <select id="dur-u" class="form-input" style="width:76px" onchange="CR.unit=this.value">
        <option value="m" ${CR.unit === 'm' ? 'selected' : ''}>min</option>
        <option value="h" ${CR.unit === 'h' ? 'selected' : ''}>hr</option>
        <option value="s" ${CR.unit === 's' ? 'selected' : ''}>sec</option>
      </select>
    </div>
  </div>
  <label class="chk-row${CR.cond === 'exiting' ? ' sel' : ''}"><input type="radio" name="cond" value="exiting" ${CR.cond === 'exiting' ? 'checked' : ''} onchange="setCond('exiting')"><span class="row-label">When Exiting</span></label>

  <div style="padding:10px 20px 4px;font-size:10px;font-weight:500;letter-spacing:.1em;text-transform:uppercase;color:var(--txt3)">Notification Options</div>
  <label class="chk-row${CR.highPriority ? ' sel' : ''}">
    <input type="checkbox" ${CR.highPriority ? 'checked' : ''} onchange="CR.highPriority=this.checked;renderCreate()">
    <div style="flex:1">
        <div class="row-label">High Priority</div>
        <div style="font-size:11px;color:var(--txt3)">Bypass DND and show at top</div>
    </div>
  </label>
  <div style="padding:0 20px 12px;display:flex;align-items:center;gap:8px">
    <span style="font-size:12px;color:var(--txt2)">Withdraw after</span>
    <input type="number" min="0" value="${CR.timeout || 0}" class="form-input" style="width:60px;padding:6px" oninput="CR.timeout=+this.value">
    <span style="font-size:12px;color:var(--txt2)">sec (0 = never)</span>
  </div>

  <div style="padding:10px 20px 4px;font-size:10px;font-weight:500;letter-spacing:.1em;text-transform:uppercase;color:var(--txt3)">Notification Icon</div>
  <div style="padding:8px 20px">
    <div class="icon-grid">${appIcons}</div>
    <div style="margin-top:10px" class="icon-grid">
      ${PRESETS.map(ic => `<div class="icon-opt${CR.icon?.type === 'preset' && CR.icon?.e === ic.e ? ' sel' : ''}" onclick="pickIcon('preset','${ic.e}')"><div class="ico">${ic.e}</div></div>`).join('')}
    </div>
  </div>
  <div style="padding:12px 20px">
    <div style="font-size:10px;font-weight:500;letter-spacing:.08em;text-transform:uppercase;color:var(--txt3);margin-bottom:6px">Cue Name (optional)</div>
    <input class="form-input" id="cue-name" placeholder="Letters/Numbers only, min 3 chars" maxlength="20" value="${CR.cueName}" oninput="CR.cueName=this.value">

    <div style="font-size:10px;font-weight:500;letter-spacing:.08em;text-transform:uppercase;color:var(--txt3);margin-top:12px;margin-bottom:6px">Notification Content</div>
    <input class="form-input" id="notif-t" placeholder="Title (optional)" maxlength="50" value="${CR.title}" oninput="CR.title=this.value">
    <textarea class="form-input" id="notif-m" rows="3" placeholder="Message" maxlength="${MAX_MSG}" style="margin-top:10px" oninput="CR.msg=this.value">${CR.msg}</textarea>
  </div>
  <label class="chk-row${CR.bubble ? ' sel' : ''}"><input type="checkbox" ${CR.bubble ? 'checked' : ''} onchange="CR.bubble=this.checked;renderCreate()"><span class="row-label">Bubble notification</span></label>
  <div style="height:20px"></div>`;
}

function backToSel() { CR.step = 0; renderCreate(); updateFab(); }
function setCond(c) { CR.cond = c; renderCreate(); }
function pickIcon(type, val) {
    if (type === 'app') CR.icon = { type: 'app', pkg: val, src: APPS.find(a => a.pkg === val)?.icon };
    else if (type === 'preset') CR.icon = { type: 'preset', e: val, src: val };
    renderCreate();
}

function saveRoutine() {
    const msg = CR.msg.trim();
    if (!msg) { snack('Message is required'); return; }

    ST.settings.lastSeqId = (ST.settings.lastSeqId || 0) + 1;
    const seqId = ST.settings.lastSeqId;

    let cueName = CR.cueName.trim();
    if (cueName) {
        if (!/^[a-zA-Z0-9]{3,}$/.test(cueName)) {
            ST.settings.lastSeqId--; // Rollback
            snack('Name must be alphanumeric and at least 3 chars');
            return;
        }
    } else {
        cueName = seqId.toString();
    }

    const r = {
        id: Date.now().toString(),
        seqId: seqId,
        cueName: cueName,
        apps: CR.selected.map(a => ({ name: a.name, pkg: a.pkg, icon: a.icon })),
        cond: CR.cond, dur: CR.dur, unit: CR.unit, timeMode: CR.timeMode,
        icon: CR.icon, title: CR.title, msg,
        bubble: CR.bubble, enabled: true,
        highPriority: CR.highPriority,
        timeout: CR.timeout
    };
    CueBridge.saveRoutine(JSON.stringify(r));
    save(); // Save lastSeqId
    snack('Routine saved!');
    resetCreate(); switchTab('list');
}

function resetCreate() {
    CR = { step: 0, selected: [], multi: false, cond: 'launched', dur: 20, unit: 'm', timeMode: 'session', icon: null, titleOn: false, title: '', msg: '', bubble: false, timeout: 0, highPriority: ST.settings.highPriority, cueName: '' };
    renderCreate(); hideFab();
}

// ── LIST ROUTINES ──
function renderList() {
    const el = document.getElementById('list-content');
    if (!ST.routines.length) {
        el.innerHTML = `<div class="empty"><p>No routines yet.</p></div>`;
        return;
    }
    el.innerHTML = ST.routines.map(r => routineCard(r)).join('');
}

function rIcon(r) {
    if (r.icon && r.icon.src) {
        return r.icon.src.startsWith('data:') ? `<img src="${r.icon.src}" style="width:28px;height:28px">` : r.icon.src;
    }
    return r.apps[0].icon.startsWith('data:') ? `<img src="${r.apps[0].icon}" style="width:28px;height:28px">` : r.apps[0].icon;
}

function routineCard(r) {
    const lbl = r.cond === 'launched' ? 'On launch' : r.cond === 'exiting' ? 'On exit' : `After ${r.dur}${r.unit}`;
    return `<div class="rcard" onclick="openDetail('${r.id}')">
    <div class="rcard-icon">${rIcon(r)}</div>
    <div style="flex:1">
      <div class="rcard-name">[${r.cueName}] ${r.title || r.apps[0].name}</div>
      <div class="rcard-sub">${lbl}</div>
    </div>
    <label class="toggle" onclick="event.stopPropagation()">
      <input type="checkbox" ${r.enabled ? 'checked' : ''} onchange="CueBridge.toggleRoutine('${r.id}', this.checked)">
      <div class="toggle-track"></div>
    </label>
  </div>`;
}

function openDetail(id) {
    const r = ST.routines.find(x => x.id === id); if (!r) return;
    const html = `<div style="padding:20px">
        <h3 style="margin-bottom:10px">${r.cueName}: ${r.title || 'Routine'}</h3>
        <p style="color:var(--txt2); font-size:13px; line-height:1.5; margin-bottom:20px">${r.msg}</p>
        <div class="sec">
            <div class="row"><div class="row-label">Condition</div><div class="row-right">${r.cond}</div></div>
            ${r.timeout > 0 ? `<div class="row"><div class="row-label">Timeout</div><div class="row-right">${r.timeout}s</div></div>` : ''}
            <div class="row"><div class="row-label">High Priority</div><div class="row-right">${r.highPriority ? 'Yes' : 'No'}</div></div>
        </div>
        <button class="btn btn-danger" style="margin-top:20px" onclick="deleteR('${r.id}')">Delete Routine</button>
    </div>`;
    openSheet('Detail', html);
}

function deleteR(id) {
    CueBridge.deleteRoutine(id);
    closeSheet();
}

// ── SETTINGS ──
function renderSettings() {
    const el = document.getElementById('settings-content');
    const cur_d = ST.settings.design || '2';
    let status = { usage: false, accessibility: false, notifications: false };
    try {
        status = JSON.parse(CueBridge.checkPermissionsStatus());
    } catch (e) { console.error(e); }

    const badge = (ok, okText, reqText) => ok ? `<span class="badge badge-ok">${okText}</span>` : `<span class="badge badge-req">${reqText}</span>`;

    el.innerHTML = `
    <div class="sec-title">Appearance</div>
    <div class="sec">
        <div style="padding:14px 20px; display:flex; gap:10px; overflow-x:auto">
            ${THEMES.map(t => `<div class="theme-swatch ${ST.settings.theme === t.id ? 'active' : ''}" style="background:linear-gradient(135deg,${t.b},${t.a}); flex-shrink:0" onclick="setTheme('${t.id}')"></div>`).join('')}
        </div>
        <div class="row" onclick="switchDesign('1')">
            <div class="row-label">Bold Design</div>
            ${cur_d === '1' ? '<span class="badge badge-ok">Active</span>' : ''}
        </div>
        <div class="row" onclick="switchDesign('2')">
            <div class="row-label">Minimal Design</div>
            ${cur_d === '2' ? '<span class="badge badge-ok">Active</span>' : ''}
        </div>
    </div>

    <div class="sec-title">Default Settings</div>
    <div class="sec">
        <div class="row">
            <div class="row-label">Global High Priority</div>
            <label class="toggle"><input type="checkbox" ${ST.settings.highPriority ? 'checked' : ''} onchange="ST.settings.highPriority=this.checked;save()"><div class="toggle-track"></div></label>
        </div>
        <div class="row">
            <div class="row-label">Default Bubble</div>
            <label class="toggle"><input type="checkbox" ${ST.settings.defaultBubble ? 'checked' : ''} onchange="ST.settings.defaultBubble=this.checked;save()"><div class="toggle-track"></div></label>
        </div>
    </div>

    <div class="sec-title">Permissions & System</div>
    <div class="sec">
        <div class="row" onclick="CueBridge.requestUsageAccess()"><div class="row-label">Usage Access</div>${badge(status.usage, 'Granted', 'Grant')}</div>
        <div class="row" onclick="CueBridge.requestAccessibilitySettings()"><div class="row-label">Accessibility Service</div>${badge(status.accessibility, 'Enabled', 'Enable')}</div>
        <div class="row" onclick="CueBridge.requestNotificationPermission()"><div class="row-label">Notifications</div>${badge(status.notifications, 'Granted', 'Config')}</div>
        <div class="row" onclick="CueBridge.requestBatteryIgnore()"><div class="row-label">Unrestrict Battery</div><span class="badge badge-opt">Optimize</span></div>
    </div>

    <div class="sec-title">App Data</div>
    <div class="sec">
        <div class="row" onclick="window.appsLoading=true;renderCreate();CueBridge.rescanApps()"><div class="row-label">Rescan Installed Apps</div><span style="font-size:16px">↻</span></div>
        <div class="row" onclick="exportData()"><div class="row-label">Export Routines (JSON)</div><span style="font-size:16px">↗</span></div>
        <div class="row" onclick="openAbout()"><div class="row-label">About CueAside</div><span style="font-size:16px">›</span></div>
    </div>

    <div class="sec-title">Danger Zone</div>
    <div class="sec">
        <div class="row" onclick="clearAllData()"><div class="row-label" style="color:var(--danger)">Clear All Data</div></div>
    </div>
    <div style="height:30px"></div>`;
}

function exportData() {
    const data = {
        v: V,
        exported: new Date().toISOString(),
        routines: ST.routines,
        settings: ST.settings
    };
    const j = JSON.stringify(data, null, 2);
    const a = document.createElement('a');
    a.href = 'data:application/json;charset=utf-8,' + encodeURIComponent(j);
    a.download = 'cue-aside-export.json';
    a.click();
    snack('Exported routines');
}

function clearAllData() {
    if (confirm('Delete ALL routines and settings? This cannot be undone.')) {
        ST.routines = [];
        ST.settings = { defaultBubble: false, highPriority: true, design: '2', theme: 'default', lastSeqId: 0 };
        CueBridge.clearAllData();
        snack('All data cleared');
        location.reload();
    }
}

function openAbout() {
    const html = `
    <div style="padding:28px 20px 20px;border-bottom:var(--line);display:flex;align-items:center;gap:16px">
      <div style="width:64px;height:64px;border-radius:16px;background:var(--bg3);border:var(--line);display:flex;align-items:center;justify-content:center;font-size:32px;flex-shrink:0">◎</div>
      <div>
        <div style="font-size:18px;font-weight:600;letter-spacing:-.02em">CueAside</div>
        <div style="font-size:12px;color:var(--txt3);margin-top:3px;font-family:'DM Mono',monospace">v${V}</div>
      </div>
    </div>
    <div class="row" style="cursor:default">
      <div style="font-size:10px;letter-spacing:.08em;text-transform:uppercase;color:var(--txt3);width:64px;flex-shrink:0">Build</div>
      <div style="font-size:13px">${BUILD_DATE}</div>
    </div>
    <div class="row" style="cursor:default;align-items:flex-start">
      <div style="font-size:10px;letter-spacing:.08em;text-transform:uppercase;color:var(--txt3);width:64px;flex-shrink:0;padding-top:2px">Info</div>
      <div style="font-size:13px;line-height:1.65;color:var(--txt2)">App-triggered notification routines. Monitors app usage via Accessibility & Usage Stats to fire custom alerts.</div>
    </div>`;
    openSheet('About', html);
}

// ── INIT ──
(function () {
    window.appsLoading = true;
    load();
    applyTheme(ST.settings.theme || 'default');
    applyDesign(ST.settings.design || '2');
    renderCreate();
    setTimeout(window.checkPermissions, 1000);
})();
