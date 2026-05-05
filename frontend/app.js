// ============================================
// N-Reinas - Búsquedas Heurísticas - App
// ============================================

const API_BASE = '';
let animationTimer = null;
let isRunning = false;

// ===== INIT =====
document.addEventListener('DOMContentLoaded', () => {
    setupTabs();
    setupControls();
    buildBoard(8);
});

// ===== TABS =====
function setupTabs() {
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-section').forEach(s => s.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById('section-' + tab.dataset.tab).classList.add('active');
        });
    });
}

// ===== CONTROLS =====
function setupControls() {
    const nInput = document.getElementById('input-n');
    const nDisplay = document.getElementById('n-display');
    nInput.addEventListener('input', () => { nDisplay.textContent = nInput.value; buildBoard(+nInput.value); });

    const speedInput = document.getElementById('input-speed');
    const speedDisplay = document.getElementById('speed-display');
    speedInput.addEventListener('input', () => { speedDisplay.textContent = speedInput.value; });

    const cmpN = document.getElementById('compare-n');
    const cmpND = document.getElementById('compare-n-display');
    cmpN.addEventListener('input', () => { cmpND.textContent = cmpN.value; });

    const cmpR = document.getElementById('compare-runs');
    const cmpRD = document.getElementById('compare-runs-display');
    cmpR.addEventListener('input', () => { cmpRD.textContent = cmpR.value; });

    document.getElementById('select-algorithm').addEventListener('change', updateAlgoDescription);
    document.getElementById('btn-solve').addEventListener('click', solveProblem);
    document.getElementById('btn-stop').addEventListener('click', stopAnimation);
    document.getElementById('btn-compare').addEventListener('click', runComparison);

    updateAlgoDescription();
}

const ALGO_DESCRIPTIONS = {
    hillclimbing: { icon: '🏔️', name: 'Escalando la Colina (Hill Climbing)', desc: 'Versión pura (Steepest-Ascent). En cada paso, evalúa todos los vecinos y selecciona el que tiene menor número de conflictos. Se detiene al llegar a un óptimo local. Tasa de éxito esperada: ~14-16% para N=8.' },
    simulatedannealing: { icon: '🔥', name: 'Recocido Simulado (Simulated Annealing)', desc: 'Comienza con temperatura alta (T=100) y la enfría gradualmente (α=0.99). Acepta movimientos peores con probabilidad e^(-ΔE/T), lo que permite escapar de óptimos locales. 5000 iteraciones máximo.' },
    tabusearch: { icon: '🚫', name: 'Búsqueda Tabú (Tabu Search)', desc: 'Exploración completa del vecindario. Mantiene una cola de movimientos inversos prohibidos (tamaño 20) para evitar ciclos. 100 iteraciones máximo.' }
};

function updateAlgoDescription() {
    const algo = document.getElementById('select-algorithm').value;
    const info = ALGO_DESCRIPTIONS[algo];
    document.querySelector('.algo-desc-icon').textContent = info.icon;
    document.querySelector('.algo-desc-text').innerHTML = `<strong>${info.name}</strong>: ${info.desc}`;

    const extraLabel = document.getElementById('metric-extra-label');
    if (algo === 'hillclimbing') extraLabel.textContent = 'Estado';
    else if (algo === 'simulatedannealing') extraLabel.textContent = 'Temperatura';
    else extraLabel.textContent = 'Lista Tabú';
}

// ===== BOARD =====
function buildBoard(n) {
    const board = document.getElementById('chessboard');
    board.style.gridTemplateColumns = `repeat(${n}, 1fr)`;
    board.innerHTML = '';
    for (let r = 0; r < n; r++) {
        for (let c = 0; c < n; c++) {
            const cell = document.createElement('div');
            cell.className = 'cell ' + ((r + c) % 2 === 0 ? 'light' : 'dark');
            cell.id = `cell-${r}-${c}`;
            board.appendChild(cell);
        }
    }
}

function renderState(state, n) {
    // Clear board
    for (let r = 0; r < n; r++) {
        for (let c = 0; c < n; c++) {
            const cell = document.getElementById(`cell-${r}-${c}`);
            if (!cell) continue;
            cell.className = 'cell ' + ((r + c) % 2 === 0 ? 'light' : 'dark');
            cell.innerHTML = '';
        }
    }

    // Find conflicts
    const conflictCols = new Set();
    for (let i = 0; i < n; i++) {
        for (let j = i + 1; j < n; j++) {
            if (state[i] === state[j] || Math.abs(state[i] - state[j]) === Math.abs(i - j)) {
                conflictCols.add(i);
                conflictCols.add(j);
            }
        }
    }

    // Place queens
    for (let c = 0; c < n; c++) {
        const r = state[c];
        const cell = document.getElementById(`cell-${r}-${c}`);
        if (!cell) continue;
        cell.classList.add('queen-cell');
        const queen = document.createElement('span');
        queen.className = 'queen' + (conflictCols.has(c) ? ' conflict-queen' : '');
        queen.textContent = '♛';
        cell.appendChild(queen);

        if (conflictCols.has(c)) cell.classList.add('conflict');
    }
}

// ===== SOLVE =====
async function solveProblem() {
    if (isRunning) return;
    const algo = document.getElementById('select-algorithm').value;
    const n = +document.getElementById('input-n').value;

    buildBoard(n);
    setStatus('running', 'Ejecutando ' + ALGO_DESCRIPTIONS[algo].name + '...');
    document.getElementById('btn-solve').disabled = true;
    document.getElementById('btn-stop').disabled = false;
    document.getElementById('result-card').style.display = 'none';
    isRunning = true;

    try {
        const res = await fetch(`${API_BASE}/api/solve?algorithm=${algo}&n=${n}`);
        const data = await res.json();

        if (!isRunning) return; // stopped

        document.getElementById('metric-time').textContent = data.timeMs.toFixed(1);
        animateSteps(data, n, algo);
    } catch (err) {
        setStatus('failed', 'Error de conexión con el servidor');
        resetButtons();
        isRunning = false;
    }
}

function animateSteps(data, n, algo) {
    const steps = data.steps;
    if (!steps || steps.length === 0) { resetButtons(); isRunning = false; return; }

    const speed = +document.getElementById('input-speed').value;
    const delay = Math.max(10, 1000 - speed * 9.5);
    let idx = 0;

    // Prepare convergence data
    const convData = [];

    function nextStep() {
        if (!isRunning || idx >= steps.length) {
            // Show final result
            showResult(data);
            drawConvergenceChart(convData);
            resetButtons();
            isRunning = false;
            return;
        }

        const step = steps[idx];
        renderState(step.state, n);
        document.getElementById('metric-iteration').textContent = step.iteration;
        document.getElementById('metric-conflicts').textContent = step.conflicts;

        if (algo === 'hillclimbing') {
            document.getElementById('metric-extra').textContent = step.conflicts === 0 ? '✅ Resuelto' : 'Buscando...';
        } else if (algo === 'simulatedannealing') {
            document.getElementById('metric-extra').textContent = step.temperature !== undefined ? step.temperature.toFixed(2) : '-';
        } else {
            document.getElementById('metric-extra').textContent = step.tabuListSize !== undefined ? step.tabuListSize : 0;
        }

        convData.push({ x: step.iteration, y: step.conflicts });
        if (convData.length % 5 === 0 || idx === steps.length - 1) drawConvergenceChart(convData);

        idx++;
        animationTimer = setTimeout(nextStep, delay);
    }
    nextStep();
}

function showResult(data) {
    const card = document.getElementById('result-card');
    card.style.display = 'block';
    if (data.solved) {
        card.className = 'result-card glass-card success';
        document.getElementById('result-icon').textContent = '✅';
        document.getElementById('result-text').textContent = '¡Solución encontrada!';
        setStatus('solved', 'Solución encontrada en ' + data.totalIterations + ' iteraciones');
    } else {
        card.className = 'result-card glass-card failure';
        document.getElementById('result-icon').textContent = '❌';
        document.getElementById('result-text').textContent = 'No se encontró solución (' + data.finalConflicts + ' conflictos restantes)';
        setStatus('failed', 'No se encontró solución');
    }
}

function stopAnimation() {
    isRunning = false;
    if (animationTimer) { clearTimeout(animationTimer); animationTimer = null; }
    resetButtons();
    setStatus('', 'Detenido');
}

function resetButtons() {
    document.getElementById('btn-solve').disabled = false;
    document.getElementById('btn-stop').disabled = true;
}

function setStatus(cls, text) {
    const status = document.getElementById('board-status');
    status.className = 'board-status ' + cls;
    document.getElementById('status-text').textContent = text;
}

// ===== CONVERGENCE CHART =====
function drawConvergenceChart(data) {
    const canvas = document.getElementById('convergence-chart');
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.parentElement.getBoundingClientRect();
    const W = rect.width - 48;
    const H = 260;

    canvas.width = W * dpr;
    canvas.height = H * dpr;
    canvas.style.width = W + 'px';
    canvas.style.height = H + 'px';
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, W, H);

    if (data.length < 2) return;

    const pad = { top: 20, right: 20, bottom: 40, left: 55 };
    const cw = W - pad.left - pad.right;
    const ch = H - pad.top - pad.bottom;

    const maxY = Math.max(...data.map(d => d.y), 1);
    const maxX = Math.max(...data.map(d => d.x), 1);

    // Grid
    ctx.strokeStyle = 'rgba(139,92,246,0.1)';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 5; i++) {
        const y = pad.top + ch * i / 5;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(W - pad.right, y); ctx.stroke();
    }

    // Axis labels
    ctx.fillStyle = '#6b7280';
    ctx.font = '11px Inter, sans-serif';
    ctx.textAlign = 'right';
    for (let i = 0; i <= 5; i++) {
        const val = Math.round(maxY * (5 - i) / 5);
        ctx.fillText(val, pad.left - 8, pad.top + ch * i / 5 + 4);
    }
    ctx.textAlign = 'center';
    ctx.fillText('Iteración', W / 2, H - 4);
    ctx.save();
    ctx.translate(14, H / 2);
    ctx.rotate(-Math.PI / 2);
    ctx.fillText('Conflictos', 0, 0);
    ctx.restore();

    // Line
    const grad = ctx.createLinearGradient(pad.left, 0, W - pad.right, 0);
    grad.addColorStop(0, '#8b5cf6');
    grad.addColorStop(1, '#06b6d4');

    ctx.strokeStyle = grad;
    ctx.lineWidth = 2.5;
    ctx.lineJoin = 'round';
    ctx.beginPath();
    data.forEach((d, i) => {
        const x = pad.left + (d.x / maxX) * cw;
        const y = pad.top + ch - (d.y / maxY) * ch;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Fill under curve
    const fillGrad = ctx.createLinearGradient(0, pad.top, 0, pad.top + ch);
    fillGrad.addColorStop(0, 'rgba(139,92,246,0.15)');
    fillGrad.addColorStop(1, 'rgba(139,92,246,0.01)');
    ctx.lineTo(pad.left + (data[data.length - 1].x / maxX) * cw, pad.top + ch);
    ctx.lineTo(pad.left + (data[0].x / maxX) * cw, pad.top + ch);
    ctx.closePath();
    ctx.fillStyle = fillGrad;
    ctx.fill();

    // Last point dot
    const last = data[data.length - 1];
    const lx = pad.left + (last.x / maxX) * cw;
    const ly = pad.top + ch - (last.y / maxY) * ch;
    ctx.beginPath();
    ctx.arc(lx, ly, 5, 0, Math.PI * 2);
    ctx.fillStyle = last.y === 0 ? '#10b981' : '#8b5cf6';
    ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,0.8)';
    ctx.lineWidth = 2;
    ctx.stroke();
}

// ===== COMPARISON =====
async function runComparison() {
    const n = +document.getElementById('compare-n').value;
    const runs = +document.getElementById('compare-runs').value;

    document.getElementById('compare-loading').style.display = 'block';
    document.getElementById('compare-results').style.display = 'none';
    document.getElementById('btn-compare').disabled = true;

    try {
        const res = await fetch(`${API_BASE}/api/compare?n=${n}&runs=${runs}`);
        const data = await res.json();
        displayComparison(data);
    } catch (err) {
        alert('Error de conexión con el servidor. Asegúrate de que el servidor Java está ejecutándose.');
    } finally {
        document.getElementById('compare-loading').style.display = 'none';
        document.getElementById('btn-compare').disabled = false;
    }
}

function displayComparison(data) {
    document.getElementById('compare-results').style.display = 'block';
    const r = data.results;
    const hc = r.hillclimbing, sa = r.simulatedannealing, ts = r.tabusearch;

    // Build table
    const metrics = [
        { label: 'Tasa de éxito (%)', key: 'successRate', fmt: v => v.toFixed(1), higher: true },
        { label: 'Iteraciones promedio', key: 'avgIterations', fmt: v => v.toFixed(1), higher: false },
        { label: 'Tiempo promedio (ms)', key: 'avgTimeMs', fmt: v => v.toFixed(2), higher: false },
        { label: 'Min iteraciones', key: 'minIterations', fmt: v => v, higher: false },
        { label: 'Max iteraciones', key: 'maxIterations', fmt: v => v, higher: false },
        { label: 'Éxitos', key: 'successes', fmt: v => v, higher: true },
    ];

    const tbody = document.getElementById('compare-table-body');
    tbody.innerHTML = '';

    metrics.forEach(m => {
        const vals = [hc[m.key], sa[m.key], ts[m.key]];
        const bestVal = m.higher ? Math.max(...vals) : Math.min(...vals);
        const tr = document.createElement('tr');
        tr.innerHTML = `<td style="font-family:var(--font-primary);color:var(--text-primary)">${m.label}</td>` +
            vals.map(v => {
                const isBest = v === bestVal;
                return `<td class="${isBest ? 'best-value' : ''}">${m.fmt(v)}</td>`;
            }).join('');
        tbody.appendChild(tr);
    });

    // Draw bar charts
    drawBarChart('chart-success', [hc.successRate, sa.successRate, ts.successRate], '%');
    drawBarChart('chart-iterations', [hc.avgIterations, sa.avgIterations, ts.avgIterations], '');
    drawBarChart('chart-time', [hc.avgTimeMs, sa.avgTimeMs, ts.avgTimeMs], 'ms');


}

function drawBarChart(canvasId, values, unit) {
    const canvas = document.getElementById(canvasId);
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.parentElement.getBoundingClientRect();
    const W = rect.width - 48;
    const H = 230;

    canvas.width = W * dpr;
    canvas.height = H * dpr;
    canvas.style.width = W + 'px';
    canvas.style.height = H + 'px';
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, W, H);

    const colors = ['#8b5cf6', '#f87171', '#06b6d4'];
    const labels = ['Hill Climbing', 'Sim. Annealing', 'Tabu Search'];
    const pad = { top: 20, bottom: 55, left: 55, right: 20 };
    const cw = W - pad.left - pad.right;
    const ch = H - pad.top - pad.bottom;
    const maxVal = Math.max(...values, 0.01);

    const barW = Math.min(60, cw / 5);
    const gap = (cw - barW * 3) / 4;

    // Grid lines
    ctx.strokeStyle = 'rgba(139,92,246,0.08)';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = pad.top + ch * i / 4;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(W - pad.right, y); ctx.stroke();
        ctx.fillStyle = '#6b7280';
        ctx.font = '10px Inter';
        ctx.textAlign = 'right';
        ctx.fillText(((4 - i) / 4 * maxVal).toFixed(maxVal < 10 ? 2 : 0), pad.left - 8, y + 4);
    }

    values.forEach((v, i) => {
        const x = pad.left + gap + i * (barW + gap);
        const barH = (v / maxVal) * ch;
        const y = pad.top + ch - barH;

        // Bar gradient
        const grad = ctx.createLinearGradient(0, y, 0, y + barH);
        grad.addColorStop(0, colors[i]);
        grad.addColorStop(1, colors[i] + '66');

        ctx.fillStyle = grad;
        ctx.beginPath();
        const r = 6;
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + barW - r, y);
        ctx.quadraticCurveTo(x + barW, y, x + barW, y + r);
        ctx.lineTo(x + barW, y + barH);
        ctx.lineTo(x, y + barH);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.fill();

        // Value on top
        ctx.fillStyle = '#e8e8f0';
        ctx.font = 'bold 12px JetBrains Mono';
        ctx.textAlign = 'center';
        ctx.fillText(v < 10 ? v.toFixed(2) : Math.round(v), x + barW / 2, y - 6);

        // Label
        ctx.fillStyle = colors[i];
        ctx.font = '10px Inter';
        ctx.fillText(labels[i], x + barW / 2, H - pad.bottom + 18);
    });
}


