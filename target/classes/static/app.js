const API_BASE = '/api/cache';

// Chart Setup
const canvas = document.getElementById('hitRateCanvas');
const ctx = canvas.getContext('2d');
const hitRateData = Array(60).fill(0);

function drawChart() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const w = canvas.width;
    const h = canvas.height;
    const step = w / 59;
    
    ctx.beginPath();
    ctx.moveTo(0, h - (hitRateData[0] / 100) * h);
    for (let i = 1; i < 60; i++) {
        ctx.lineTo(i * step, h - (hitRateData[i] / 100) * h);
    }
    ctx.strokeStyle = '#3b82f6';
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.lineTo(w, h);
    ctx.lineTo(0, h);
    ctx.fillStyle = 'rgba(59, 130, 246, 0.1)';
    ctx.fill();
}

function updateChart(newVal) {
    hitRateData.shift();
    hitRateData.push(newVal);
    drawChart();
}

// Error handling
function showError(msg) {
    const banner = document.getElementById('error-banner');
    banner.textContent = msg;
    banner.classList.remove('hidden');
    setTimeout(() => banner.classList.add('hidden'), 5000);
}

// Logging
function addLog(op, key, result, evictedKey = null) {
    const logDiv = document.getElementById('operationLog');
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    
    let icon = '✓';
    if (result === 'MISS' || result === 'NOT_FOUND' || result === 'EXPIRED') {
        icon = '✗';
        entry.classList.add('miss');
    } else if (result === 'STORED' || result === 'HIT' || result === 'DELETED') {
        entry.classList.add('success');
    }

    let text = `${icon} ${op} ${key} → ${result}`;
    if (evictedKey) {
        text += ` ⚡ EVICTED ${evictedKey}`;
        entry.classList.add('evict');
        entry.classList.remove('success');
    }
    
    entry.textContent = text;
    logDiv.prepend(entry);
}

// Refresh Data
async function refreshData() {
    try {
        const [metricsRes, entriesRes] = await Promise.all([
            fetch(`${API_BASE}/metrics`),
            fetch(`${API_BASE}/entries`)
        ]);

        if (metricsRes.ok) {
            const m = await metricsRes.json();
            document.getElementById('hitRateValue').textContent = m.hitRate.toFixed(1) + '%';
            document.getElementById('missRateValue').textContent = m.missRate.toFixed(1) + '%';
            document.getElementById('cacheSizeValue').textContent = `${m.size} / ${m.capacity}`;
            document.getElementById('hitsValue').textContent = m.hits;
            document.getElementById('missesValue').textContent = m.misses;
            document.getElementById('evictionsValue').textContent = m.evictions;
            document.getElementById('expirationsValue').textContent = m.expirations;
            
            // Update dropdown and input only if not currently focused to avoid annoying jumps
            if (document.activeElement !== document.getElementById('policySelect')) {
                document.getElementById('policySelect').value = m.policy;
            }
            if (document.activeElement !== document.getElementById('capacityInput')) {
                document.getElementById('capacityInput').value = m.capacity;
            }

            updateChart(m.hitRate);
        }

        if (entriesRes.ok) {
            const entries = await entriesRes.json();
            const tbody = document.getElementById('entriesBody');
            tbody.innerHTML = '';
            
            entries.forEach(e => {
                const tr = document.createElement('tr');
                if (e.status === 'EXPIRED') tr.classList.add('expired-row');
                
                const timeAgo = Math.floor((Date.now() - e.lastAccessTime) / 1000);
                const badgeClass = e.status === 'LIVE' ? 'live' : 'expired';
                
                // Escape textContent via JS element creation
                const kTd = document.createElement('td'); kTd.textContent = e.key;
                const vTd = document.createElement('td'); vTd.textContent = e.value;
                const ttlTd = document.createElement('td'); ttlTd.textContent = e.remainingTtlSeconds + 's';
                const countTd = document.createElement('td'); countTd.textContent = e.accessCount;
                const accessTd = document.createElement('td'); accessTd.textContent = timeAgo + 's ago';
                const statusTd = document.createElement('td');
                
                const badge = document.createElement('span');
                badge.className = `badge ${badgeClass}`;
                badge.textContent = e.status;
                statusTd.appendChild(badge);

                tr.append(kTd, vTd, ttlTd, countTd, accessTd, statusTd);
                tbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error("Refresh failed", e);
    }
}

// Single Operations
document.getElementById('btnPut').addEventListener('click', async () => {
    const key = document.getElementById('opKey').value;
    const value = document.getElementById('opValue').value;
    const ttl = document.getElementById('opTtl').value || 60;
    
    if (!key || !value) {
        showError("Key and Value are required for PUT.");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/entries/${key}?ttl=${ttl}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'text/plain' },
            body: value
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "PUT failed");
        
        addLog('PUT', data.key, data.status, data.evictedKey);
        refreshData();
    } catch (e) {
        showError(e.message);
    }
});

document.getElementById('btnGet').addEventListener('click', async () => {
    const key = document.getElementById('opKey').value;
    if (!key) {
        showError("Key is required for GET.");
        return;
    }
    
    try {
        const res = await fetch(`${API_BASE}/entries/${key}`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "GET failed");
        
        const resLabel = data.status === 'MISS' ? (data.reason || 'MISS') : 'HIT';
        addLog('GET', data.key, resLabel);
        if (data.status === 'HIT') {
            document.getElementById('opValue').value = data.value;
        }
        refreshData();
    } catch (e) {
        showError(e.message);
    }
});

document.getElementById('btnDelete').addEventListener('click', async () => {
    const key = document.getElementById('opKey').value;
    if (!key) {
        showError("Key is required for DELETE.");
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/entries/${key}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "DELETE failed");
        
        addLog('DELETE', data.key, data.status);
        document.getElementById('opKey').value = '';
        document.getElementById('opValue').value = '';
        refreshData();
    } catch (e) {
        showError(e.message);
    }
});

// Policy and Capacity
document.getElementById('policySelect').addEventListener('change', async (e) => {
    try {
        const res = await fetch(`${API_BASE}/policy?policy=${e.target.value}`, { method: 'POST' });
        if (!res.ok) throw new Error("Failed to update policy");
        refreshData();
    } catch(err) {
        showError(err.message);
    }
});

document.getElementById('btnSetCapacity').addEventListener('click', async () => {
    const cap = document.getElementById('capacityInput').value;
    if (cap < 1) {
        showError("Capacity must be >= 1");
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/capacity?value=${cap}`, { method: 'POST' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "Capacity update failed");
        refreshData();
    } catch (e) {
        showError(e.message);
    }
});

// Demos
async function runDemo(endpoint) {
    try {
        const res = await fetch(`${API_BASE}/demo${endpoint}`, { method: 'POST' });
        const data = await res.json();
        if (data.message) {
            const entry = document.createElement('div');
            entry.className = 'log-entry';
            entry.style.color = 'var(--text-secondary)';
            entry.textContent = `--- ${data.message} ---`;
            document.getElementById('operationLog').prepend(entry);
        }
        if (data.logs) {
            data.logs.forEach(l => addLog(l.op, l.key, l.result, l.evictedKey));
        } else if (Array.isArray(data)) {
            data.forEach(l => addLog(l.op, l.key, l.result, l.evictedKey));
        }
        refreshData();
        return data;
    } catch(e) {
        showError(e.message);
    }
}

document.getElementById('btnDemoSample').addEventListener('click', () => runDemo(''));
document.getElementById('btnDemoEviction').addEventListener('click', () => runDemo('/eviction'));

document.getElementById('btnDemoTtl').addEventListener('click', async () => {
    await runDemo('/ttl');
    
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.style.color = 'var(--warning)';
    entry.textContent = "Waiting 3.5s for TTL to expire...";
    document.getElementById('operationLog').prepend(entry);
    
    setTimeout(async () => {
        try {
            const res = await fetch(`${API_BASE}/entries/temp`);
            const data = await res.json();
            const resLabel = data.status === 'MISS' ? (data.reason || 'MISS') : 'HIT';
            addLog('GET', data.key, resLabel);
            refreshData();
        } catch(e) {
            showError(e.message);
        }
    }, 3500);
});

document.getElementById('btnDemoCompare').addEventListener('click', async () => {
    const res = await runDemo('/compare?pattern=zipf');
    if (res && res.LRU && res.LFU) {
        const compDiv = document.getElementById('compareResult');
        compDiv.classList.remove('hidden');
        document.getElementById('comparePattern').textContent = res.pattern;
        
        const tbody = document.getElementById('compareBody');
        tbody.innerHTML = '';
        
        ['LRU', 'LFU'].forEach(pol => {
            const pData = res[pol];
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${pol}</strong></td>
                <td>${pData.hitRate.toFixed(1)}%</td>
                <td>${pData.hits}</td>
                <td>${pData.misses}</td>
                <td>${pData.evictions}</td>
            `;
            tbody.appendChild(tr);
        });
    }
});

document.getElementById('btnClearCache').addEventListener('click', async () => {
    try {
        await fetch(`${API_BASE}/entries`, { method: 'DELETE' });
        refreshData();
    } catch(e) {
        showError(e.message);
    }
});

document.getElementById('btnResetMetrics').addEventListener('click', async () => {
    try {
        await fetch(`${API_BASE}/metrics/reset`, { method: 'POST' });
        refreshData();
    } catch(e) {
        showError(e.message);
    }
});

// Init
setInterval(refreshData, 1000);
// Ensure canvas renders correctly on load
setTimeout(() => {
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
    drawChart();
    refreshData();
}, 100);
