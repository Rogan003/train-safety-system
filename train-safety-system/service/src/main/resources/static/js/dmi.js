/* DMI front-end. Polls /api/state at 5 Hz and renders the dashboard.
   Mutating actions are POSTed; the next state snapshot reflects them. */

const $ = (id) => document.getElementById(id);
const post = async (url) => fetch(url, { method: 'POST' });
const get  = async (url) => (await fetch(url)).json();

const MAX_SPEED_GAUGE = 300; // km/h — full scale

// --------- bind controls --------------------------------------------------
$('btn-start').onclick = () => post('/api/sim/start');
$('btn-stop').onclick  = () => post('/api/sim/stop');
$('btn-reset').onclick = async () => {
    await post('/api/sim/reset');
    $('throttle-slider').value = 0;
    $('brake-slider').value = 0;
    $('throttle-val').textContent = '0';
    $('brake-val').textContent = '0';
    $('safe-results').innerHTML = '';
    $('lvl-results').innerHTML = '';
    $('log-list').innerHTML = '';
};
$('btn-sifa').onclick  = () => post('/api/driver/alertness');
$('btn-arm-traction').onclick = () => post('/api/driver/arm-traction');

$('throttle-slider').oninput = (e) => {
    $('throttle-val').textContent = (+e.target.value).toFixed(2);
    post(`/api/driver/throttle?value=${e.target.value}`);
};
$('brake-slider').oninput = (e) => {
    $('brake-val').textContent = (+e.target.value).toFixed(2);
    post(`/api/driver/brake?value=${e.target.value}`);
};

document.querySelectorAll('[data-action]').forEach(btn => {
    btn.onclick = () => post(btn.dataset.action);
});

$('btn-safe').onclick = async () => {
    const r = await get('/api/checks/safe-to-depart');
    renderCheck('safe-results', 'SafeToDepart (R15)', r);
};
$('btn-leveltr').onclick = async () => {
    const r = await get('/api/checks/safe-route-tree?startNodeId=Station%20Entry');
    renderCheck('lvl-results', 'SafeRouteTree (R16)', r);
};

function renderCheck(elId, header, r) {
    const ul = $(elId);
    ul.innerHTML = '';
    const head = document.createElement('li');
    head.className = 'head';
    head.textContent = `${header}: ${r.overall ? '✅ ALLOWED' : '❌ BLOCKED'}`;
    ul.appendChild(head);
    for (const [k, v] of Object.entries(r.subgoals)) {
        const li = document.createElement('li');
        li.className = v ? 'ok' : 'fail';
        li.textContent = `${v ? '✓' : '✗'} ${k}`;
        ul.appendChild(li);
    }
}

// --------- speed gauge geometry ------------------------------------------
const ARC_LEN = 2 * Math.PI * 90;  // SVG circumference

function setArc(elId, kmh) {
    const frac = Math.max(0, Math.min(1, kmh / MAX_SPEED_GAUGE));
    $(elId).setAttribute('stroke-dasharray', `${frac * ARC_LEN} ${ARC_LEN}`);
}

function setTickMark(elId, kmh) {
    // 2-degree tick: render a tiny arc segment at the kmh point
    const frac = Math.max(0, Math.min(1, kmh / MAX_SPEED_GAUGE));
    const tick = 4; // arc length px
    const offset = frac * ARC_LEN - tick / 2;
    $(elId).setAttribute('stroke-dasharray', `0 ${offset} ${tick} ${ARC_LEN}`);
}

// --------- canvas: braking curve plot ------------------------------------
const curveCanvas = $('curve-canvas');
function drawCurve(state) {
    const ctx = curveCanvas.getContext('2d');
    const W = curveCanvas.width = curveCanvas.clientWidth;
    const H = curveCanvas.height = 180;
    ctx.clearRect(0, 0, W, H);

    const aEff = state.bc.effectiveDeceleration || 1.0;
    const dToEoa = Math.max(0, state.bc.distanceToEoa);
    const xMax = Math.max(dToEoa, 50);  // plot 0..xMax metres ahead
    const yMax = Math.max(state.activeProfile?.maxV ?? 200, state.bc.vEmergencyBrake + 20, state.train.speed + 20);

    // axes
    ctx.strokeStyle = '#2a3540'; ctx.lineWidth = 1;
    ctx.strokeRect(40, 10, W - 60, H - 30);
    ctx.fillStyle = '#93a1b0'; ctx.font = '10px monospace';
    ctx.fillText('km/h', 6, 16);
    ctx.fillText('m → EOA', W - 60, H - 6);
    ctx.fillText(`${yMax.toFixed(0)}`, 6, 22);
    ctx.fillText('0', 28, H - 22);
    ctx.fillText(xMax.toFixed(0), W - 30, H - 22);

    // curve function v(d) = sqrt(2*a*d) (km/h)
    const curveAt = (d) => Math.sqrt(2 * aEff * Math.max(0, d)) * 3.6;
    const xPx = (d) => 40 + (d / xMax) * (W - 60);
    const yPx = (v) => 10 + (1 - v / yMax) * (H - 30);

    const drawLine = (style, valFn, dash) => {
        ctx.strokeStyle = style; ctx.lineWidth = 2; ctx.setLineDash(dash || []);
        ctx.beginPath();
        const STEP = 4;
        for (let px = 40; px <= W - 20; px += STEP) {
            const d = (px - 40) / (W - 60) * xMax;
            const v = Math.min(valFn(d), yMax);
            if (px === 40) ctx.moveTo(px, yPx(v)); else ctx.lineTo(px, yPx(v));
        }
        ctx.stroke();
        ctx.setLineDash([]);
    };

    drawLine('#e02020', (d) => curveAt(d));                               // EBD
    drawLine('#f08000', (d) => Math.max(0, curveAt(d) - 5));              // SBD
    drawLine('#f4d000', (d) => Math.max(0, curveAt(d) - 10), [4,4]);      // W

    // train marker
    const trainD = dToEoa;       // distance from train to EOA on x axis
    const trainV = state.train.speed;
    ctx.fillStyle = '#3df';
    ctx.beginPath(); ctx.arc(xPx(trainD), yPx(trainV), 5, 0, 2 * Math.PI); ctx.fill();
    ctx.strokeStyle = '#3df88'; ctx.beginPath();
    ctx.moveTo(xPx(trainD), 10); ctx.lineTo(xPx(trainD), H - 20); ctx.stroke();
}

// --------- canvas: track view --------------------------------------------
const trackCanvas = $('track-canvas');
function drawTrack(state) {
    const ctx = trackCanvas.getContext('2d');
    const W = trackCanvas.width = trackCanvas.clientWidth;
    const H = trackCanvas.height = 120;
    ctx.clearRect(0, 0, W, H);

    // Determine plotting span centred on train, ±400m
    const center = state.train.position;
    const span = 1500; // m total
    const left = center - span / 2;

    // rails
    ctx.strokeStyle = '#445b6a'; ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(0, 70); ctx.lineTo(W, 70); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(0, 80); ctx.lineTo(W, 80); ctx.stroke();

    const xOf = (m) => (m - left) / span * W;

    // EOA marker
    ctx.strokeStyle = '#e02020'; ctx.setLineDash([6, 4]); ctx.lineWidth = 3;
    ctx.beginPath(); const eoaX = xOf(state.ma.eoa);
    ctx.moveTo(eoaX, 20); ctx.lineTo(eoaX, 100); ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = '#e02020'; ctx.font = '11px monospace';
    ctx.fillText('EOA ' + state.ma.eoa.toFixed(0), eoaX + 3, 20);

    // balises
    for (const b of state.balises) {
        const x = xOf(b.position);
        if (x < 0 || x > W) continue;
        ctx.fillStyle = b.consumed ? '#445b6a' : '#3df';
        ctx.beginPath(); ctx.arc(x, 92, 5, 0, 2 * Math.PI); ctx.fill();
        ctx.fillStyle = '#93a1b0'; ctx.font = '10px monospace';
        ctx.fillText(b.id, x - 14, 110);
    }

    // train cars — show as boxes at position; loco is first car, others trail behind by 20m each
    const carLen = 20; // m per car
    let offset = 0;
    for (const c of state.cars) {
        const headPos = state.train.position - offset;
        const x = xOf(headPos);
        const xTail = xOf(headPos - carLen);
        const doorAlarm = c.doorStatus !== 'LOCKED';
        const brakeBad = c.brakeTest === 'FAILED';
        ctx.fillStyle = c.locomotive ? '#3df' :
                        doorAlarm ? '#e02020' :
                        brakeBad ? '#f4d000' : '#356';
        ctx.fillRect(xTail, 60, x - xTail, 22);
        ctx.strokeStyle = '#0f1419';
        ctx.strokeRect(xTail, 60, x - xTail, 22);
        ctx.fillStyle = '#fff'; ctx.font = '10px monospace';
        ctx.fillText(c.id, xTail + 3, 75);
        offset += carLen + 1;
    }

    // pos marker text
    ctx.fillStyle = '#3df'; ctx.font = '11px monospace';
    ctx.fillText(`pos = ${state.train.position.toFixed(0)} m`, 8, 16);
}

// --------- main render loop ---------------------------------------------
async function refresh() {
    let state;
    try { state = await get('/api/state'); }
    catch { setTimeout(refresh, 1000); return; }

    // Top-line
    $('status-badge').textContent = state.running ? 'running' :
        (state.emergencyActive ? 'STOPPED — emergency' : 'stopped');
    $('status-badge').style.background = state.emergencyActive ? '#822' : '#2a3540';

    // Speed gauge
    $('speed-val').textContent = state.train.speed.toFixed(0);
    setArc('speed-arc', state.train.speed);
    setTickMark('warn-mark', state.bc.vWarning);
    setTickMark('sbd-mark',  state.bc.vServiceBrake);
    setTickMark('ebd-mark',  state.bc.vEmergencyBrake);

    // MA block
    $('target-speed').textContent = state.ma.targetSpeed.toFixed(0) + ' km/h';
    $('dist-eoa').textContent = state.bc.distanceToEoa.toFixed(0) + ' m';
    $('eoa-pos').textContent = state.ma.eoa.toFixed(0);
    $('pos').textContent = state.train.position.toFixed(0) + ' m';

    // Curve info
    $('vw').textContent   = state.bc.vWarning.toFixed(1);
    $('vsbd').textContent = state.bc.vServiceBrake.toFixed(1);
    $('vebd').textContent = state.bc.vEmergencyBrake.toFixed(1);
    $('aeff').textContent = state.bc.effectiveDeceleration.toFixed(2);
    $('dstop').textContent = state.bc.dStop.toFixed(1);

    // Env
    $('mu-val').textContent = state.activeAdhesion?.mu.toFixed(2) ?? '–';
    $('grad-val').textContent = state.infra.gradient.toFixed(1) + '‰';
    $('mass-val').textContent = (state.train.mass / 1000).toFixed(1) + ' t';
    $('lambda-val').textContent = state.train.brakePercentage.toFixed(1) + ' %';
    $('sifa-val').textContent = state.train.sifaStatus;
    $('traction-val').textContent = state.train.tractionActive ? 'ARMED' : 'CUT / OFF';

    // Warnings
    const warns = state.warnings || [];
    const wl = $('warnings-list');
    if (warns.length === 0) {
        wl.innerHTML = '<div class="empty">No active warnings</div>';
    } else {
        wl.innerHTML = '';
        for (const w of warns) {
            const row = document.createElement('div');
            row.className = 'warning-row' + (
                ['SIFA', 'DOORS_OPEN_WHILE_MOVING'].includes(w.type) ? ' crit' : '');
            row.textContent = `[${w.type}] ${w.message}`;
            wl.appendChild(row);
        }
    }

    // Command pills
    $('cmd-em').classList.toggle('active', state.emergencyActive);
    $('cmd-sb').classList.toggle('active', state.serviceBrakeActive);
    $('cmd-ct').classList.toggle('active', !state.train.tractionActive);
    $('cmd-sand').classList.toggle('active', state.sanding);

    // Track + curve
    drawCurve(state);
    drawTrack(state);

    // Log
    const ll = $('log-list');
    if (state.events) {
        const recent = state.events.slice(-50).reverse();
        ll.innerHTML = recent.map(e => {
            const t = new Date(e.timestamp).toLocaleTimeString();
            return `<div>[${t}] ${e.message}</div>`;
        }).join('');
    }

    setTimeout(refresh, 200);
}

refresh();
