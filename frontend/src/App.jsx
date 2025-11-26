import { useEffect, useMemo, useState } from 'react';

const STATUS_OPTIONS = ['PASSED', 'FAILED', 'NOT RUN'];
const GENERAL_STATUS_OPTIONS = [
  { value: 'Очередь', color: '#e0e8ff', textColor: '#294a9a' },
  { value: 'В работе', color: '#fff4e0', textColor: '#9a5b29' },
  { value: 'Готово', color: '#e0f4e0', textColor: '#1b6b35' },
  { value: 'Бэклог', color: '#ffe5e5', textColor: '#b3261e' },
  { value: 'Только ручное', color: '#ffecec', textColor: '#b23b34' },
  { value: 'Неактуально', color: '#f2e6ff', textColor: '#6b3fa0' },
  { value: 'Фронт', color: '#e0f7f4', textColor: '#0f766e' }
];
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

const FIELD_DEFINITIONS = [
  { key: 'testId', label: 'Test ID', editable: false, type: 'text' },
  { key: 'category', label: 'Category / Feature', editable: true, type: 'text' },
  { key: 'shortTitle', label: 'Short Title', editable: true, type: 'text' },
  { key: 'issueLink', label: 'YouTrack Issue Link', editable: true, type: 'text' },
  { key: 'readyDate', label: 'Ready Date', editable: true, type: 'date' },
  { key: 'generalStatus', label: 'General Test Status', editable: true, type: 'generalStatus' },
  { key: 'scenario', label: 'Detailed Scenario', editable: true, type: 'textarea' },
  { key: 'notes', label: 'Notes', editable: true, type: 'textarea' }
];

const ACTION_COLUMN = { key: 'actions', label: '', type: 'actions', editable: false };

function createEmptyItem() {
  return FIELD_DEFINITIONS.reduce((acc, field) => {
    acc[field.key] = '';
    return acc;
  }, {});
}

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

function parseTestId(rawId) {
  const trimmed = (rawId ?? '').trim();
  const match = /^([0-9]+)(?:-([0-9]+))?$/.exec(trimmed);

  if (!match) {
    return { original: trimmed, base: null, suffix: null };
  }

  return {
    original: trimmed,
    base: Number(match[1]),
    suffix: match[2] ? Number(match[2]) : null
  };
}

function compareTestIds(a, b) {
  const left = parseTestId(a?.testId);
  const right = parseTestId(b?.testId);

  if (Number.isFinite(left.base) && Number.isFinite(right.base)) {
    const baseDiff = left.base - right.base;
    if (baseDiff !== 0) {
      return baseDiff;
    }

    if (left.suffix == null && right.suffix != null) return -1;
    if (left.suffix != null && right.suffix == null) return 1;

    if (left.suffix != null && right.suffix != null) {
      const suffixDiff = left.suffix - right.suffix;
      if (suffixDiff !== 0) {
        return suffixDiff;
      }
    }
  }

  return left.original.localeCompare(right.original, undefined, { numeric: true, sensitivity: 'base' });
}

function StatusChip({ option }) {
  if (!option) {
    return <span className="status-chip placeholder-chip">—</span>;
  }
  const style = {
    backgroundColor: option.color,
    color: option.textColor
  };
  return (
    <span className="status-chip" style={style}>
      {option.value}
    </span>
  );
}

function StatusDropdown({ value, onChange, disabled = false, allowEmpty = true }) {
  const selectedOption = GENERAL_STATUS_OPTIONS.find((option) => option.value === value);

  const handleSelect = (optionValue, event) => {
    if (disabled) return;
    onChange(optionValue);
    const details = event?.target?.closest('details');
    if (details) {
      details.removeAttribute('open');
    }
  };

  return (
    <div className="status-dropdown">
      <details className="status-dropdown-toggle">
        <summary>
          <StatusChip option={selectedOption} />
        </summary>
        <div className="status-options">
          {allowEmpty && (
            <button
              type="button"
              className="status-option"
              onMouseDown={(e) => e.preventDefault()}
              onClick={(event) => handleSelect('', event)}
              disabled={disabled}
            >
              <StatusChip option={null} />
              <span className="status-option-label">Очистить</span>
            </button>
          )}
          {GENERAL_STATUS_OPTIONS.map((option) => (
            <button
              type="button"
              key={option.value}
              className="status-option"
              onMouseDown={(e) => e.preventDefault()}
              onClick={(event) => handleSelect(option.value, event)}
              disabled={disabled}
            >
              <StatusChip option={option} />
              <span className="status-option-label">{option.value}</span>
            </button>
          ))}
        </div>
      </details>
    </div>
  );
}

export default function App() {
  const [items, setItems] = useState([]);
  const [columnConfig, setColumnConfig] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [newItems, setNewItems] = useState([]);
  const [popup, setPopup] = useState(null);
  const [regressionState, setRegressionState] = useState('idle');

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/tests'));
      if (!response.ok) {
        throw new Error('Failed to load data');
      }
      const data = await response.json();
      const loadedItems = data.items ?? [];
      setItems(loadedItems);
      setColumnConfig(data.columnConfig ?? {});

      const hasStatuses = loadedItems.some((item) => (item.runStatuses?.[0] ?? '') !== '');
      setRegressionState((prev) => {
        if (prev === 'stopped') {
          return hasStatuses ? 'stopped' : 'stopped';
        }
        if (hasStatuses) {
          return 'active';
        }
        return 'idle';
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const regressionColumn = useMemo(
    () => ({ key: 'regression', label: 'Regression', runIndex: 1 }),
    []
  );

  const handleFieldChange = (testId, key, value) => {
    setItems((prev) =>
      prev.map((item) => (item.testId === testId ? { ...item, [key]: value } : item))
    );
  };

  const sendUpdate = async (testId, payload) => {
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/tests'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ testId, ...payload })
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

  const createTest = async (payload) => {
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/tests'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        let message = 'Failed to create test';
        try {
          const data = await response.json();
          message = data.message || data.detail || message;
        } catch (parseError) {
          const text = await response.text();
          message = text || message;
        }
        throw new Error(message);
      }
      await loadData();
    } catch (err) {
      setError(err.message);
      throw err;
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

  const handleNewFieldChange = (index, key, value) => {
    setNewItems((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, [key]: value } : item))
    );
  };

  const startNewRow = () => {
    setNewItems((prev) => [...prev, createEmptyItem()]);
  };

  const cancelNewRow = (index) => {
    setNewItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const isDraftEmpty = (draft) => {
    if (!draft) {
      return true;
    }
    return !FIELD_DEFINITIONS.some((field) => {
      const value = draft[field.key];
      if (field.type === 'date') {
        return !!value;
      }
      if (typeof value === 'string') {
        return value.trim() !== '';
      }
      return value != null && value !== '';
    });
  };

  const hasPristineNewRow = useMemo(
    () => newItems.some((item) => isDraftEmpty(item)),
    [newItems]
  );

  const sortedItems = useMemo(() => {
    return [...items].sort(compareTestIds);
  }, [items]);

  const closePopup = () => setPopup(null);

  const handleCreate = async (index) => {
    const draft = newItems[index];
    if (!draft) {
      return;
    }
    const trimmedId = (draft.testId || '').trim();
    if (!trimmedId) {
      setError('Test ID is required');
      return;
    }

    const isExistingId = items.some((item) => String(item.testId).trim() === trimmedId);
    const isDuplicateDraft = newItems.some(
      (item, idx) => idx !== index && (item.testId || '').trim() === trimmedId
    );

    if (isExistingId || isDuplicateDraft) {
      setPopup({
        title: 'Duplicate Test ID',
        message: `Test case with ID "${trimmedId}" already exists. Please use a unique ID before saving.`
      });
      return;
    }

    const payload = { testId: trimmedId };
    FIELD_DEFINITIONS.forEach((field) => {
      if (field.key === 'testId') {
        return;
      }
      const value = draft[field.key];
      if (field.type === 'date') {
        if (value) {
          payload[field.key] = value;
        }
      } else if (typeof value === 'string') {
        const trimmed = value.trim();
        if (trimmed) {
          payload[field.key] = trimmed;
        }
      } else if (value != null) {
        payload[field.key] = value;
      }
    });

    try {
      await createTest(payload);
      setNewItems((prev) => prev.filter((_, idx) => idx !== index));
    } catch (err) {
      // keep the form for correction
    }
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

  const handleGeneralStatusChange = (item, value) => {
    handleFieldChange(item.testId, 'generalStatus', value);
    sendUpdate(item.testId, { generalStatus: value === '' ? null : value });
  };

  const handleDelete = async (testId) => {
    if (!window.confirm(`Delete test ${testId}?`)) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase(`/api/tests/${encodeURIComponent(testId)}`), {
        method: 'DELETE'
      });
      if (!response.ok) {
        throw new Error('Failed to delete test');
      }
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
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

  const columns = [ACTION_COLUMN, ...FIELD_DEFINITIONS, regressionColumn];

  const getColumnWidth = (column) => {
    if (column.key === ACTION_COLUMN.key) {
      return columnConfig[column.key] ?? 72;
    }
    if (column.key === regressionColumn.key) {
      return columnConfig.run1 ?? columnConfig[column.key] ?? 160;
    }
    return columnConfig[column.key] ?? (column.type === 'textarea' ? 280 : 160);
  };

  const regressionEditable = regressionState === 'active';

  return (
    <div className="app-container">
      {popup && (
        <div className="popup-backdrop" role="alertdialog" aria-modal="true">
          <div className="popup-card">
            <div className="popup-header">
              <div className="popup-icon" aria-hidden="true">⚠️</div>
              <div>
                <div className="popup-title">{popup.title}</div>
                <div className="popup-subtitle">Resolve the issue to continue</div>
              </div>
            </div>
            <p className="popup-message">{popup.message}</p>
            <div className="popup-actions">
              <button type="button" className="primary-btn" onClick={closePopup}>
                Got it
              </button>
            </div>
          </div>
        </div>
      )}
      <header className="app-header">
        <h1>Test Report</h1>
        <div className="header-actions">
          <button
            type="button"
            onClick={startNewRow}
            className="secondary-btn"
            disabled={loading || saving || hasPristineNewRow}
          >
            Add Row
          </button>
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
                  const width = getColumnWidth(column);
                  const letter = columnLetter(idx);
                  const isRegression = column.key === regressionColumn.key;
                  const regressionHeaderState = `regression-header regression-${regressionState}`;
                  return (
                    <th
                      key={column.key}
                      style={{ width: `${width}px`, minWidth: `${width}px` }}
                      className={isRegression ? regressionHeaderState : ''}
                    >
                      <div className="header-content">
                        <span className="column-letter">{letter}</span>
                        <span>{column.label}</span>
                        {isRegression && (
                          <div className="regression-actions">
                            {regressionEditable ? (
                              <button
                                type="button"
                                className="stop-btn"
                                onClick={() => setRegressionState('stopped')}
                              >
                                Stop
                              </button>
                            ) : (
                              <button
                                type="button"
                                className="primary-btn"
                                onClick={() => setRegressionState('active')}
                              >
                                Хотите начать регресс
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {newItems.map((item, index) => (
                <tr className="new-row" key={`new-row-${index}`}>
                  <td className="row-index-cell new-row-actions">
                    <button
                      type="button"
                      className="save-btn"
                      onClick={() => handleCreate(index)}
                      disabled={
                        saving || !(item.testId ? item.testId.trim() : '').length
                      }
                    >
                      Save
                    </button>
                    <button
                      type="button"
                      className="cancel-btn"
                      onClick={() => cancelNewRow(index)}
                      disabled={saving}
                    >
                      Cancel
                    </button>
                  </td>
                  <td
                    className="action-cell"
                    style={{ width: `${getColumnWidth(ACTION_COLUMN)}px`, minWidth: `${getColumnWidth(ACTION_COLUMN)}px` }}
                  >
                    —
                  </td>
                  {FIELD_DEFINITIONS.map((column) => {
                    const width = getColumnWidth(column);
                    const value = item[column.key] ?? '';
                    return (
                      <td
                        key={`new-${index}-${column.key}`}
                        style={{ width: `${width}px`, minWidth: `${width}px` }}
                      >
                        {column.type === 'textarea' ? (
                          <textarea
                            value={value}
                            onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                            className="cell-textarea"
                          />
                        ) : column.type === 'date' ? (
                          <input
                            type="date"
                            value={value}
                            onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                            className="cell-input"
                          />
                        ) : column.type === 'generalStatus' ? (
                          <StatusDropdown
                            value={value}
                            onChange={(newValue) => handleNewFieldChange(index, column.key, newValue)}
                          />
                        ) : (
                          <input
                            type="text"
                            value={value}
                            onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                            className="cell-input"
                          />
                        )}
                      </td>
                    );
                  })}
                  <td
                    key={`new-${index}-${regressionColumn.key}`}
                    style={{
                      width: `${getColumnWidth(regressionColumn)}px`,
                      minWidth: `${getColumnWidth(regressionColumn)}px`
                    }}
                    className={`run-column-cell regression-${regressionState} empty-cell`}
                  >
                    —
                  </td>
                </tr>
              ))}
              {sortedItems.map((item, rowIndex) => (
                <tr key={item.testId}>
                  <td className="row-index-cell">{rowIndex + 1}</td>
                  <td
                    className="action-cell"
                    style={{ width: `${getColumnWidth(ACTION_COLUMN)}px`, minWidth: `${getColumnWidth(ACTION_COLUMN)}px` }}
                  >
                    <button
                      type="button"
                      className="delete-btn"
                      onClick={() => handleDelete(item.testId)}
                      disabled={saving}
                    >
                      ✕
                    </button>
                  </td>
                  {FIELD_DEFINITIONS.map((column) => {
                    const width = getColumnWidth(column);
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
                          ) : column.type === 'generalStatus' ? (
                            <StatusDropdown
                              value={value}
                              onChange={(newValue) => handleGeneralStatusChange(item, newValue)}
                              disabled={saving}
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
                  <td
                    key={regressionColumn.key}
                    style={{
                      width: `${getColumnWidth(regressionColumn)}px`,
                      minWidth: `${getColumnWidth(regressionColumn)}px`
                    }}
                    className={`run-column-cell regression-${regressionState}`}
                  >
                    <select
                      value={item.runStatuses?.[0] ?? ''}
                      onChange={(e) => handleRunChange(item, 1, e.target.value)}
                      className="cell-select"
                      disabled={!regressionEditable || saving}
                    >
                      <option value="">—</option>
                      {STATUS_OPTIONS.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
