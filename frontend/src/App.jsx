import React, { useState, useEffect, useRef } from 'react';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Filler,
  Legend
} from 'chart.js';
import { 
  Activity, 
  Database, 
  Settings, 
  Trash2, 
  RefreshCw, 
  Play, 
  Zap, 
  AlertCircle 
} from 'lucide-react';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Filler,
  Legend
);

const API_BASE = '/api/cache';

function App() {
  const [metrics, setMetrics] = useState({
    hitRate: 0, missRate: 0, size: 0, capacity: 0,
    hits: 0, misses: 0, evictions: 0, expirations: 0,
    policy: 'LRU'
  });
  const [entries, setEntries] = useState([]);
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState(null);
  const [hitRateData, setHitRateData] = useState(Array(60).fill(0));
  const [compareResult, setCompareResult] = useState(null);

  // Form states
  const [opKey, setOpKey] = useState('');
  const [opValue, setOpValue] = useState('');
  const [opTtl, setOpTtl] = useState(60);
  const [capacityInput, setCapacityInput] = useState(10);
  const [policyInput, setPolicyInput] = useState('LRU');

  const chartRef = useRef(null);

  const showError = (msg) => {
    setError(msg);
    setTimeout(() => setError(null), 5000);
  };

  const addLog = (op, key, result, evictedKey = null) => {
    setLogs(prev => {
      const newLogs = [{
        id: Date.now() + Math.random(),
        op, key, result, evictedKey,
        time: new Date()
      }, ...prev];
      return newLogs.slice(0, 100); // keep last 100
    });
  };

  const fetchData = async () => {
    try {
      const [mRes, eRes] = await Promise.all([
        fetch(`${API_BASE}/metrics`),
        fetch(`${API_BASE}/entries`)
      ]);

      if (mRes.ok) {
        const m = await mRes.json();
        setMetrics(m);
        setHitRateData(prev => {
          const newData = [...prev.slice(1), m.hitRate];
          return newData;
        });
      }
      if (eRes.ok) {
        const e = await eRes.json();
        setEntries(e);
      }
    } catch (err) {
      console.error("Failed to fetch data", err);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 1000);
    return () => clearInterval(interval);
  }, []);

  // Sync inputs with metrics
  useEffect(() => {
    if (metrics.capacity > 0 && !document.activeElement?.id?.includes('capacity')) {
      setCapacityInput(metrics.capacity);
    }
    if (metrics.policy && !document.activeElement?.id?.includes('policy')) {
      setPolicyInput(metrics.policy);
    }
  }, [metrics.capacity, metrics.policy]);

  const handlePut = async () => {
    if (!opKey || !opValue) return showError("Key and Value are required for PUT");
    try {
      const res = await fetch(`${API_BASE}/entries/${opKey}?ttl=${opTtl}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'text/plain' },
        body: opValue
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "PUT failed");
      addLog('PUT', data.key, data.status, data.evictedKey);
      fetchData();
    } catch (e) { showError(e.message); }
  };

  const handleGet = async () => {
    if (!opKey) return showError("Key is required for GET");
    try {
      const res = await fetch(`${API_BASE}/entries/${opKey}`);
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "GET failed");
      const resLabel = data.status === 'MISS' ? (data.reason || 'MISS') : 'HIT';
      addLog('GET', data.key, resLabel);
      if (data.status === 'HIT') setOpValue(data.value);
      fetchData();
    } catch (e) { showError(e.message); }
  };

  const handleDelete = async () => {
    if (!opKey) return showError("Key is required for DELETE");
    try {
      const res = await fetch(`${API_BASE}/entries/${opKey}`, { method: 'DELETE' });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "DELETE failed");
      addLog('DELETE', data.key, data.status);
      setOpKey(''); setOpValue('');
      fetchData();
    } catch (e) { showError(e.message); }
  };

  const handlePolicyChange = async (e) => {
    const val = e.target.value;
    setPolicyInput(val);
    try {
      const res = await fetch(`${API_BASE}/policy?policy=${val}`, { method: 'POST' });
      if (!res.ok) throw new Error("Failed to update policy");
      fetchData();
    } catch (err) { showError(err.message); }
  };

  const handleCapacitySet = async () => {
    if (capacityInput < 1) return showError("Capacity must be >= 1");
    try {
      const res = await fetch(`${API_BASE}/capacity?value=${capacityInput}`, { method: 'POST' });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Capacity update failed");
      fetchData();
    } catch (e) { showError(e.message); }
  };

  const runDemo = async (endpoint) => {
    try {
      const res = await fetch(`${API_BASE}/demo${endpoint}`, { method: 'POST' });
      const data = await res.json();
      if (data.message) {
        addLog('---', '', data.message);
      }
      if (data.logs) {
        data.logs.forEach(l => addLog(l.op, l.key, l.result, l.evictedKey));
      } else if (Array.isArray(data)) {
        data.forEach(l => addLog(l.op, l.key, l.result, l.evictedKey));
      }
      fetchData();
      return data;
    } catch(e) { showError(e.message); }
  };

  const handleDemoTtl = async () => {
    await runDemo('/ttl');
    addLog('INFO', '', 'Waiting 3.5s for TTL to expire...');
    setTimeout(async () => {
      try {
        const res = await fetch(`${API_BASE}/entries/temp`);
        const data = await res.json();
        const resLabel = data.status === 'MISS' ? (data.reason || 'MISS') : 'HIT';
        addLog('GET', data.key, resLabel);
        fetchData();
      } catch(e) { showError(e.message); }
    }, 3500);
  };

  const handleCompare = async () => {
    const res = await runDemo('/compare?pattern=zipf');
    if (res && res.LRU && res.LFU) {
      setCompareResult(res);
    }
  };

  const handleClearCache = async () => {
    try {
      await fetch(`${API_BASE}/entries`, { method: 'DELETE' });
      fetchData();
    } catch(e) { showError(e.message); }
  };

  const handleResetMetrics = async () => {
    try {
      await fetch(`${API_BASE}/metrics/reset`, { method: 'POST' });
      fetchData();
    } catch(e) { showError(e.message); }
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 0 },
    scales: {
      x: { display: false },
      y: { min: 0, max: 100, display: false }
    },
    plugins: { legend: { display: false }, tooltip: { enabled: false } },
    elements: { point: { radius: 0 } }
  };

  const chartDataConfig = {
    labels: Array(60).fill(''),
    datasets: [{
      data: hitRateData,
      borderColor: '#3b82f6',
      backgroundColor: 'rgba(59, 130, 246, 0.1)',
      borderWidth: 2,
      fill: true,
      tension: 0.1
    }]
  };

  return (
    <div className="app-container">
      <header className="header">
        <div className="logo">
          <h1>CACHEFUSION</h1>
          <p>Adaptive Hybrid Cache Dashboard</p>
        </div>
        {error && (
          <div className="error-banner">
            <AlertCircle size={18} />
            {error}
          </div>
        )}
      </header>

      <div className="glass-panel metrics-section">
        <div>
          <h2 className="panel-title"><Activity size={20} /> System Metrics</h2>
          <div className="metrics-grid">
            <div className="metric-card highlight">
              <span className="metric-label">Hit Rate</span>
              <span className="metric-value">{metrics.hitRate.toFixed(1)}%</span>
            </div>
            <div className="metric-card highlight">
              <span className="metric-label">Miss Rate</span>
              <span className="metric-value">{metrics.missRate.toFixed(1)}%</span>
            </div>
            <div className="metric-card highlight">
              <span className="metric-label">Cache Size</span>
              <span className="metric-value">{metrics.size} / {metrics.capacity}</span>
            </div>
            <div className="metric-card">
              <span className="metric-label">Hits</span>
              <span className="metric-value">{metrics.hits}</span>
            </div>
            <div className="metric-card">
              <span className="metric-label">Misses</span>
              <span className="metric-value">{metrics.misses}</span>
            </div>
            <div className="metric-card">
              <span className="metric-label">Evictions</span>
              <span className="metric-value">{metrics.evictions}</span>
            </div>
            <div className="metric-card">
              <span className="metric-label">Expirations</span>
              <span className="metric-value">{metrics.expirations}</span>
            </div>
          </div>
        </div>
        
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <h2 className="panel-title" style={{ marginBottom: '1rem', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Hit Rate Trend (60s)</h2>
          <div className="chart-container" style={{ flex: 1 }}>
            <Line ref={chartRef} data={chartDataConfig} options={chartOptions} />
          </div>
        </div>
      </div>

      <div className="glass-panel" style={{ marginBottom: '1.5rem' }}>
        <div className="controls-bar">
          <div className="control-group">
            <label id="policy-label"><Settings size={16} /> Eviction Policy</label>
            <div className="input-wrapper">
              <select id="policySelect" value={policyInput} onChange={handlePolicyChange}>
                <option value="LRU">LRU (Least Recently Used)</option>
                <option value="LFU">LFU (Least Frequently Used)</option>
              </select>
            </div>
          </div>
          <div className="control-group">
            <label id="capacity-label"><Database size={16} /> Capacity</label>
            <div className="input-wrapper">
              <input 
                id="capacityInput"
                type="number" 
                min="1" 
                value={capacityInput} 
                onChange={e => setCapacityInput(e.target.value)}
                style={{ width: '80px' }}
              />
            </div>
            <button className="btn btn-primary" onClick={handleCapacitySet}>Set</button>
          </div>
        </div>
      </div>

      <div className="main-grid">
        <div className="left-col" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div className="glass-panel">
            <h2 className="panel-title"><Zap size={20} /> Cache Operations</h2>
            <div className="form-grid">
              <div className="form-group">
                <label>Key</label>
                <input value={opKey} onChange={e => setOpKey(e.target.value)} placeholder="e.g. user1" />
              </div>
              <div className="form-group">
                <label>Value</label>
                <input value={opValue} onChange={e => setOpValue(e.target.value)} placeholder="e.g. Sarvesh" />
              </div>
              <div className="form-group">
                <label>TTL (sec)</label>
                <input type="number" value={opTtl} onChange={e => setOpTtl(e.target.value)} min="1" />
              </div>
            </div>
            <div className="action-buttons">
              <button className="btn btn-primary" onClick={handlePut}>PUT</button>
              <button className="btn btn-secondary" onClick={handleGet}>GET</button>
              <button className="btn btn-danger" onClick={handleDelete}>DELETE</button>
            </div>
          </div>

          <div className="glass-panel">
            <h2 className="panel-title"><Play size={20} /> Demonstrations</h2>
            <div className="demo-grid">
              <button className="btn btn-outline" onClick={() => runDemo('')}>RUN SAMPLE</button>
              <button className="btn btn-outline" onClick={() => runDemo('/eviction')}>LRU vs LFU</button>
              <button className="btn btn-outline" onClick={handleDemoTtl}>TEST TTL</button>
              <button className="btn btn-outline" onClick={handleCompare}>COMPARE</button>
              <button className="btn btn-danger" onClick={handleClearCache}><Trash2 size={16}/> CLEAR</button>
              <button className="btn btn-danger" onClick={handleResetMetrics}><RefreshCw size={16}/> RESET</button>
            </div>

            {compareResult && (
              <div className="compare-results">
                <h3>Comparison: {compareResult.pattern}</h3>
                <table>
                  <thead>
                    <tr>
                      <th>Policy</th>
                      <th>Hit Rate</th>
                      <th>Hits</th>
                      <th>Misses</th>
                      <th>Evict</th>
                    </tr>
                  </thead>
                  <tbody>
                    {['LRU', 'LFU'].map(pol => (
                      <tr key={pol}>
                        <td><strong>{pol}</strong></td>
                        <td>{compareResult[pol].hitRate.toFixed(1)}%</td>
                        <td>{compareResult[pol].hits}</td>
                        <td>{compareResult[pol].misses}</td>
                        <td>{compareResult[pol].evictions}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <h3 style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '1.5rem', marginBottom: '0.8rem' }}>OPERATION LOG</h3>
            <div className="logs-container">
              {logs.map((l) => (
                <div key={l.id} className="log-entry">
                  {l.op === '---' ? (
                    <span className="log-info">{l.result}</span>
                  ) : (
                    <>
                      <span className={
                        ['STORED', 'HIT', 'DELETED'].includes(l.result) ? 'log-success' : 
                        ['MISS', 'NOT_FOUND', 'EXPIRED'].includes(l.result) ? 'log-warning' : 'log-info'
                      }>
                        {['STORED', 'HIT', 'DELETED'].includes(l.result) ? '✓' : '✗'}
                      </span>
                      <span>{l.op} {l.key} &rarr; 
                        <strong className={['STORED', 'HIT', 'DELETED'].includes(l.result) ? 'log-success' : 'log-warning'}>
                          {' '}{l.result}
                        </strong>
                      </span>
                      {l.evictedKey && <span className="log-error">⚡ EVICTED {l.evictedKey}</span>}
                    </>
                  )}
                </div>
              ))}
            </div>
          </div>

        </div>

        <div className="right-col">
          <div className="glass-panel" style={{ height: '100%' }}>
            <h2 className="panel-title"><Database size={20} /> Hybrid Cache Entries (All Tiers)</h2>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Key</th>
                    <th>Value</th>
                    <th>Tier</th>
                    <th>TTL</th>
                    <th>Count</th>
                    <th>Age</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map(e => {
                    const age = Math.floor((Date.now() - e.lastAccessTime) / 1000);
                    return (
                      <tr key={e.key} className={e.status === 'EXPIRED' ? 'expired-row' : ''}>
                        <td>{e.key}</td>
                        <td>{e.value}</td>
                        <td><span className="badge badge-live" style={{background: 'rgba(59, 130, 246, 0.1)', color: '#60A5FA', borderColor: 'rgba(59, 130, 246, 0.3)'}}>{e.tier}</span></td>
                        <td>{e.remainingTtlSeconds}s</td>
                        <td>{e.accessCount}</td>
                        <td>{age}s ago</td>
                        <td>
                          <span className={`badge ${e.status === 'LIVE' ? 'badge-live' : 'badge-expired'}`}>
                            {e.status}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                  {entries.length === 0 && (
                    <tr>
                      <td colSpan="6" style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '2rem' }}>
                        Cache is empty
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}

export default App;
