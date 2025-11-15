import { useEffect, useMemo, useState } from 'react';

const STATUS_OPTIONS = ['PASSED', 'FAILED', 'NOT RUN'];
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

const FIELD_DEFINITIONS = [
  { key: 'testId', label: 'Test ID', editable: false, type: 'text' },
  { key: 'category', label: 'Category / Feature', editable: true, type: 'text' },
  { key: 'shortTitle', label: 'Short Title', editable: true, type: 'text' },
  { key: 'issueLink', label: 'YouTrack Issue Link', editable: true, type: 'text' },
  { key: 'readyDate', label: 'Ready Date', editable: true, type: 'date' },
  { key: 'generalStatus', label: 'General Test Status', editable: true, type: 'text' },
  { key: 'scenario', label: 'Detailed Scenario', editable: true, type: 'textarea' },
  { key: 'notes', label: 'Notes', editable: true, type: 'textarea' }
];

function withBase(path) {
  if (!API_BASE) {
    return path;
  }
  return `${API_BASE}${path}`;
}

function columnLetter(index) {
  let n = index;
  let result = '';
  while (n >= 0) {
    result = String.fromCharCode((n % 26) + 65) + result;
    n = Math.floor(n / 26) - 1;
  }
  return result;
}

export default function App() {
  const [items, setItems] = useState([]);
  const [runs, setRuns] = useState([]);
  const [columnConfig, setColumnConfig] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/tests'));
      if (!response.ok) {
        throw new Error('Failed to load data');
      }
      const data = await response.json();
      setItems(data.items ?? []);
      setRuns(data.runs ?? []);
      setColumnConfig(data.columnConfig ?? {});
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const runColumns = useMemo(() => {
    return runs
      .slice()
      .sort((a, b) => a.runIndex - b.runIndex)
      .map((run) => ({
        key: `run${run.runIndex}`,
        label: `Run #${run.runIndex}${run.runDate ? ` (${run.runDate})` : ''}`,
        runIndex: run.runIndex
      }));
  }, [runs]);

  const handleFieldChange = (testId, key, value) => {
    setItems((prev) =>
      prev.map((item) => (item.testId === testId ? { ...item, [key]: value } : item))
    );
  };

  const sendUpdate = async (testId, payload) => {
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase(`/api/tests/${encodeURIComponent(testId)}`), {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        throw new Error('Failed to save changes');
      }
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleBlur = (item, key) => {
    const value = item[key];
    const sanitizedValue = value === '' ? null : value;
    const payload = { [key]: sanitizedValue };
    sendUpdate(item.testId, payload);
  };

  const handleRunChange = (item, runIndex, value) => {
    const runStatuses = [...(item.runStatuses ?? [])];
    runStatuses[runIndex - 1] = value;
    setItems((prev) =>
      prev.map((row) =>
        row.testId === item.testId ? { ...row, runStatuses } : row
      )
    );
    sendUpdate(item.testId, {
      runIndex,
      runStatus: value === '' ? null : value,
      runDate: value === '' ? null : new Date().toISOString().slice(0, 10)
    });
  };

  const handleExport = async () => {
    setError(null);
    try {
      const response = await fetch(withBase('/api/tests/export/excel'));
      if (!response.ok) {
        throw new Error('Failed to export data');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'test-report.xlsx';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    }
  };

  const columns = [...FIELD_DEFINITIONS, ...runColumns];

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>Test Report</h1>
        <div className="header-actions">
          <button type="button" onClick={handleExport} className="primary-btn">
            Export to Excel
          </button>
          {saving && <span className="status">Saving…</span>}
        </div>
      </header>
      {error && <div className="error-banner">{error}</div>}
      {loading ? (
        <div className="loader">Loading…</div>
      ) : (
        <div className="table-wrapper">
          <table className="report-table">
            <thead>
              <tr>
                <th className="row-index-header">#</th>
                {columns.map((column, idx) => {
                  const width = columnConfig[column.key] ?? (column.type === 'textarea' ? 280 : 160);
                  const letter = columnLetter(idx);
                  return (
                    <th
                      key={column.key}
                      style={{ width: `${width}px`, minWidth: `${width}px` }}
                    >
                      <div className="header-content">
                        <span className="column-letter">{letter}</span>
                        <span>{column.label}</span>
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {items.map((item, rowIndex) => (
                <tr key={item.testId}>
                  <td className="row-index-cell">{rowIndex + 1}</td>
                  {FIELD_DEFINITIONS.map((column) => {
                    const width = columnConfig[column.key] ?? (column.type === 'textarea' ? 280 : 160);
                    const value = item[column.key] ?? '';
                    return (
                      <td
                        key={column.key}
                        style={{ width: `${width}px`, minWidth: `${width}px` }}
                      >
                        {column.editable ? (
                          column.type === 'textarea' ? (
                            <textarea
                              value={value}
                              onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                              onBlur={() => handleBlur(item, column.key)}
                              className="cell-textarea"
                            />
                          ) : column.type === 'date' ? (
                            <input
                              type="date"
                              value={value ? value : ''}
                              onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                              onBlur={() => handleBlur(item, column.key)}
                              className="cell-input"
                            />
                          ) : (
                            <input
                              type="text"
                              value={value}
                              onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                              onBlur={() => handleBlur(item, column.key)}
                              className="cell-input"
                            />
                          )
                        ) : (
                          <span className="readonly-value">{value}</span>
                        )}
                      </td>
                    );
                  })}
                  {runColumns.map((column) => {
                    const width = columnConfig[column.key] ?? 120;
                    const current = item.runStatuses?.[column.runIndex - 1] ?? '';
                    return (
                      <td
                        key={column.key}
                        style={{ width: `${width}px`, minWidth: `${width}px` }}
                      >
                        <select
                          value={current}
                          onChange={(e) => handleRunChange(item, column.runIndex, e.target.value)}
                          className="cell-select"
                        >
                          <option value="">—</option>
                          {STATUS_OPTIONS.map((status) => (
                            <option key={status} value={status}>
                              {status}
                            </option>
                          ))}
                        </select>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
