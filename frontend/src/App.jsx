import { useEffect, useMemo, useRef, useState } from 'react';
import ReleaseAnalyticsWidget from './ReleaseAnalyticsWidget.tsx';
const GENERAL_STATUS_OPTIONS = [
  { value: 'Очередь', color: '#e0e8ff', textColor: '#294a9a' },
  { value: 'В работе', color: '#fff4e0', textColor: '#9a5b29' },
  { value: 'Готово', color: '#e0f4e0', textColor: '#1b6b35' },
  { value: 'Бэклог', color: '#ffe5e5', textColor: '#b3261e' },
  { value: 'Только ручное', color: '#ffecec', textColor: '#b23b34' },
  { value: 'Неактуально', color: '#f2e6ff', textColor: '#6b3fa0' },
  { value: 'Фронт', color: '#e0f7f4', textColor: '#0f766e' }
];

const REGRESSION_STATUS_OPTIONS = ['PASSED', 'FAILED', 'SKIPPED'];
const PRIORITY_OPTIONS = ['Critical', 'Blocker', 'High', 'Medium', 'Low', 'Trivial'];
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const MULTILINE_TEXT_KEYS = new Set(['category', 'shortTitle']);

const FIELD_DEFINITIONS = [
  { key: 'testId', label: 'Test ID', editable: false, type: 'text' },
  { key: 'category', label: 'Category / Feature', editable: true, type: 'text' },
  { key: 'shortTitle', label: 'Short Title', editable: true, type: 'text' },
  { key: 'issueLink', label: 'YouTrack Issue Link', editable: true, type: 'text' },
  { key: 'readyDate', label: 'Ready Date', editable: false, type: 'readonlyDate' },
  { key: 'generalStatus', label: 'General Test Status', editable: true, type: 'generalStatus' },
  { key: 'priority', label: 'Priority', editable: true, type: 'priority' },
  { key: 'scenario', label: 'Detailed Scenario', editable: true, type: 'textarea' },
  { key: 'notes', label: 'Notes', editable: true, type: 'textarea' }
];

const REGRESSION_COLUMN = {
  key: 'regressionStatus',
  label: 'Regress Run',
  editable: true,
  type: 'regression'
};

const TABLE_COLUMNS = [...FIELD_DEFINITIONS, REGRESSION_COLUMN];

const ACTION_COLUMN = { key: 'actions', label: '', type: 'actions', editable: false };

const DEFAULT_ISSUE_LINK = 'https://youtrackru/issue/';

function todayIsoDate() {
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function createEmptyItem() {
  return FIELD_DEFINITIONS.reduce((acc, field) => {
    if (field.key === 'priority') {
      acc[field.key] = PRIORITY_OPTIONS[3];
    } else if (field.key === 'issueLink') {
      acc[field.key] = DEFAULT_ISSUE_LINK;
    } else if (field.key === 'readyDate') {
      acc[field.key] = todayIsoDate();
    } else {
      acc[field.key] = '';
    }
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

function renderTextWithCodeBlocks(text) {
  if (!text) {
    return null;
  }

  const regex = /```([\s\S]*?)```/g;
  const segments = [];
  let lastIndex = 0;
  let match;

  while ((match = regex.exec(text)) !== null) {
    const preceding = text.slice(lastIndex, match.index);
    if (preceding) {
      segments.push(
        <div key={`text-${lastIndex}`} className="rich-text-segment">
          {preceding}
        </div>
      );
    }

    segments.push(
      <pre key={`code-${match.index}`} className="rich-text-code-block">
        <code>{match[1]}</code>
      </pre>
    );

    lastIndex = regex.lastIndex;
  }

  const trailing = text.slice(lastIndex);
  if (trailing) {
    segments.push(
      <div key={`text-${lastIndex}`} className="rich-text-segment">
        {trailing}
      </div>
    );
  }

  return segments;
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

function StatusDropdown({ value, onChange, disabled = false, allowEmpty = true, onFocus, onBlur }) {
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
    <div className="status-dropdown" onFocusCapture={onFocus} onBlurCapture={onBlur}>
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

function RegressionStatusSelect({ value, onChange, disabled, onFocus, onBlur }) {
  return (
    <select
      className="regression-select"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      onFocus={onFocus}
      onBlur={onBlur}
    >
      <option value="">—</option>
      {REGRESSION_STATUS_OPTIONS.map((option) => (
        <option key={option} value={option}>
          {option}
        </option>
      ))}
    </select>
  );
}

function PrioritySelect({ value, onChange, disabled = false, onFocus, onBlur }) {
  return (
    <select
      className="cell-input"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      onFocus={onFocus}
      onBlur={onBlur}
    >
      {PRIORITY_OPTIONS.map((option) => (
        <option key={option} value={option}>
          {option}
        </option>
      ))}
    </select>
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
  const [regressionState, setRegressionState] = useState({
    status: 'IDLE',
    regressionDate: null,
    results: {},
    releaseName: null
  });
  const [regressionResults, setRegressionResults] = useState({});
  const [regressionLoading, setRegressionLoading] = useState(true);
  const [regressionSaving, setRegressionSaving] = useState(false);
  const [releaseNameDraft, setReleaseNameDraft] = useState('');
  const [showReleaseNameInput, setShowReleaseNameInput] = useState(false);
  const [editingExistingCount, setEditingExistingCount] = useState(0);
  const [selectedUploadFiles, setSelectedUploadFiles] = useState([]);
  const [uploadSelectionLabel, setUploadSelectionLabel] = useState('');
  const [uploading, setUploading] = useState(false);
  const uploadInputRef = useRef(null);

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
      setColumnConfig(data.columnConfig ?? {});
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadRegressionState = async () => {
    setRegressionLoading(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/regressions/current'));
      if (!response.ok) {
        throw new Error('Failed to load regression state');
      }
      const data = await response.json();
      setRegressionState(data);
      setRegressionResults(data.results ?? {});
      if (data.status !== 'RUNNING') {
        setShowReleaseNameInput(false);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setRegressionLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    loadRegressionState();
  }, []);

  const isRegressionRunning = regressionState.status === 'RUNNING';
  const isEditingExistingRow = editingExistingCount > 0;

  const incrementEditingExisting = () => {
    setEditingExistingCount((count) => count + 1);
  };

  const decrementEditingExisting = () => {
    setEditingExistingCount((count) => Math.max(0, count - 1));
  };

  const resetUploadSelection = () => {
    setSelectedUploadFiles([]);
    setUploadSelectionLabel('');
    if (uploadInputRef.current) {
      uploadInputRef.current.value = '';
    }
  };

  const handleUploadFilesChange = (event) => {
    const files = Array.from(event.target.files || []);
    const jsonFiles = files.filter((file) => file.name.toLowerCase().endsWith('.json'));

    setSelectedUploadFiles(jsonFiles);

    if (jsonFiles.length === 0) {
      setUploadSelectionLabel('');
      return;
    }

    const firstPath = jsonFiles[0].webkitRelativePath || jsonFiles[0].name;
    const folderName = firstPath.includes('/') ? firstPath.split('/')[0] : '';
    const label = folderName ? `${folderName} (${jsonFiles.length} файлов)` : `${jsonFiles.length} файлов`;
    setUploadSelectionLabel(label);
  };

  const handleUploadSubmit = async () => {
    if (selectedUploadFiles.length === 0) return;

    setUploading(true);
    setError(null);

    try {
      const formData = new FormData();
      selectedUploadFiles.forEach((file) => {
        formData.append('files', file);
        const relativePath = file.webkitRelativePath || file.name;
        formData.append('paths', relativePath);
      });

      const response = await fetch(withBase('/uploadReport'), {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        let message = 'Failed to upload reports';
        try {
          const data = await response.json();
          message = data.message || data.detail || message;
        } catch (parseError) {
          const text = await response.text();
          message = text || message;
        }
        throw new Error(message);
      }

      resetUploadSelection();
      await Promise.all([loadData(), loadRegressionState()]);
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  };

  const openUploadPicker = () => {
    uploadInputRef.current?.click();
  };

  const handleFieldChange = (testId, key, value) => {
    setItems((prev) =>
      prev.map((item) => (item.testId === testId ? { ...item, [key]: value } : item))
    );
  };

  const sendUpdate = async (item, payload) => {
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/tests?forceUpdate=true'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          testId: item.testId,
          category: item.category,
          shortTitle: item.shortTitle,
          scenario: item.scenario,
          ...payload
        })
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
      const response = await fetch(withBase('/api/tests?forceUpdate=true'), {
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
    decrementEditingExisting();
    const value = item[key];
    const sanitizedValue = value === '' ? null : value;
    const payload = { [key]: sanitizedValue };
    sendUpdate(item, payload);
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

  const missingRegressionStatuses = useMemo(() => {
    if (!isRegressionRunning) {
      return false;
    }
    return sortedItems.some((item) => !(regressionResults[item.testId] || '').trim());
  }, [isRegressionRunning, regressionResults, sortedItems]);

  const handleRegressionStatusChange = (testId, value) => {
    const normalized = (value || '').toUpperCase();
    setRegressionResults((prev) => ({ ...prev, [testId]: normalized }));
  };

  const handleStartRegression = async () => {
    const trimmedReleaseName = releaseNameDraft.trim();
    if (!trimmedReleaseName) {
      setError('Release name is required');
      return;
    }
    setRegressionSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/regressions/start'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ releaseName: trimmedReleaseName })
      });
      if (!response.ok) {
        let message = 'Failed to start regression';
        try {
          const data = await response.json();
          message = data.message || data.detail || message;
        } catch (parseError) {
          const text = await response.text();
          message = text || message;
        }
        throw new Error(message);
      }
      const data = await response.json();
      setRegressionState(data);
      setRegressionResults({});
      setShowReleaseNameInput(false);
      setReleaseNameDraft('');
    } catch (err) {
      setError(err.message);
    } finally {
      setRegressionSaving(false);
    }
  };

  const handleStopRegression = async () => {
    if (!isRegressionRunning) {
      return;
    }

    if (missingRegressionStatuses) {
      setPopup({
        title: 'Не все статусы заполнены',
        message: 'Перед остановкой регресса заполните результаты для всех тест-кейсов.'
      });
      return;
    }

    setRegressionSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/regressions/stop'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ results: regressionResults })
      });
      if (!response.ok) {
        let message = 'Failed to stop regression';
        try {
          const data = await response.json();
          message = data.message || data.detail || message;
        } catch (parseError) {
          const text = await response.text();
          message = text || message;
        }
        throw new Error(message);
      }
      const data = await response.json();
      setRegressionState(data);
      setRegressionResults({});
      setShowReleaseNameInput(false);
      setReleaseNameDraft('');
    } catch (err) {
      setError(err.message);
    } finally {
      setRegressionSaving(false);
    }
  };

  const handleCancelRegression = async () => {
    setRegressionSaving(true);
    setError(null);
    try {
      const response = await fetch(withBase('/api/regressions/cancel'), { method: 'POST' });
      if (!response.ok) {
        throw new Error('Failed to cancel regression');
      }
      const data = await response.json();
      setRegressionState(data);
      setRegressionResults({});
      setShowReleaseNameInput(false);
      setReleaseNameDraft('');
    } catch (err) {
      setError(err.message);
    } finally {
      setRegressionSaving(false);
    }
  };

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
      if (field.key === 'testId' || field.key === 'readyDate') {
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

  const handleGeneralStatusChange = (item, value) => {
    handleFieldChange(item.testId, 'generalStatus', value);
    sendUpdate(item, { generalStatus: value === '' ? null : value });
  };

  const handlePriorityChange = (item, value) => {
    handleFieldChange(item.testId, 'priority', value);
    sendUpdate(item, { priority: value });
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

  const columns = [ACTION_COLUMN, ...TABLE_COLUMNS];

  const getColumnWidth = (column) => {
    if (column.key === ACTION_COLUMN.key) {
      return columnConfig[column.key] ?? 72;
    }
    return columnConfig[column.key] ?? (column.type === 'textarea' ? 280 : 160);
  };

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
          <div className="upload-actions">
            <input
              ref={uploadInputRef}
              type="file"
              style={{ display: 'none' }}
              onChange={handleUploadFilesChange}
              webkitdirectory="true"
              directory=""
              multiple
            />
            <button
              type="button"
              onClick={openUploadPicker}
              className="secondary-btn"
              disabled={loading || saving || uploading}
            >
              Upload Test Cases
            </button>
            <button
              type="button"
              onClick={handleUploadSubmit}
              className="primary-btn"
              disabled={
                uploading || selectedUploadFiles.length === 0 || loading || saving
              }
            >
              {uploading ? 'Uploading…' : 'Загрузить'}
            </button>
            {uploadSelectionLabel && (
              <span className="upload-hint">{uploadSelectionLabel}</span>
            )}
          </div>
          <button
            type="button"
            onClick={startNewRow}
            className="secondary-btn"
            disabled={
              loading ||
                saving ||
                hasPristineNewRow ||
                newItems.length > 0 ||
                isEditingExistingRow
            }
          >
            Add Row
          </button>
          <button type="button" onClick={handleExport} className="primary-btn">
            Export to Excel
          </button>
          {saving && <span className="status">Saving…</span>}
        </div>
      </header>
      <ReleaseAnalyticsWidget />
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
                  return (
                    <th
                      key={column.key}
                      style={{ width: `${width}px`, minWidth: `${width}px` }}
                    >
                      <div
                        className={`header-content ${
                          column.key === 'regressionStatus' ? 'regression-header-content' : ''
                        }`}
                      >
                        <div className="header-title">
                          <span className="column-letter">{letter}</span>
                          <span>{column.label}</span>
                        </div>
                        {column.key === 'regressionStatus' && (
                          <div className="regression-actions">
                            {isRegressionRunning ? (
                              <>
                                <button
                                  type="button"
                                  className="danger-btn"
                                  onClick={handleStopRegression}
                                  disabled={regressionSaving}
                                >
                                  {`Regression on release-version ${
                                    regressionState.releaseName || '—'
                                  } is in progress. Stop it ?`}
                                </button>
                                <button
                                  type="button"
                                  className="secondary-btn"
                                  onClick={handleCancelRegression}
                                  disabled={regressionSaving}
                                >
                                  Отменить регресс
                                </button>
                              </>
                            ) : showReleaseNameInput ? (
                              <div className="regression-start-form">
                                <input
                                  type="text"
                                  value={releaseNameDraft}
                                  onChange={(e) => setReleaseNameDraft(e.target.value)}
                                  placeholder="Release name"
                                  className="cell-input release-input"
                                  disabled={regressionSaving}
                                />
                                <button
                                  type="button"
                                  className="success-btn"
                                  onClick={handleStartRegression}
                                  disabled={regressionSaving || !releaseNameDraft.trim()}
                                >
                                  Save
                                </button>
                                <button
                                  type="button"
                                  className="secondary-btn"
                                  onClick={() => setShowReleaseNameInput(false)}
                                  disabled={regressionSaving}
                                >
                                  Cancel
                                </button>
                              </div>
                            ) : (
                              <button
                                type="button"
                                className="danger-btn"
                                onClick={() => setShowReleaseNameInput(true)}
                                disabled={loading || saving || regressionSaving || regressionLoading}
                              >
                                Would you run regress
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
                  {TABLE_COLUMNS.map((column) => {
                    const width = getColumnWidth(column);
                    const value = item[column.key] ?? '';
                    const isEditable = column.editable || column.key === 'testId';
                    const cellDataAttributes = {};

                    if (column.key === 'testId') {
                      cellDataAttributes['data-column'] = 'test-id';
                      cellDataAttributes['data-row-id'] = item.testId || `new-${index + 1}`;
                    } else if (column.key === 'readyDate') {
                      cellDataAttributes['data-column'] = 'ready-date';
                      cellDataAttributes['data-row-id'] = item.testId || `new-${index + 1}`;
                    }

                    const readonlySpanAttributes =
                      column.key === 'testId'
                        ? { 'data-test-id-value': value }
                        : column.key === 'readyDate'
                          ? { 'data-ready-date-value': value }
                          : undefined;

                    return (
                      <td
                        key={`new-${index}-${column.key}`}
                        style={{ width: `${width}px`, minWidth: `${width}px` }}
                        className={column.type === 'regression' ? 'regression-cell locked' : undefined}
                        {...cellDataAttributes}
                      >
                        {!isEditable ? (
                          <span className="readonly-value" {...readonlySpanAttributes}>{value}</span>
                        ) : column.type === 'textarea' ? (
                          <div className="textarea-with-preview">
                            <textarea
                              value={value}
                              onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                              className="cell-textarea"
                            />
                            {column.key === 'scenario' && value.includes('```') && (
                              <div className="rich-text-preview">{renderTextWithCodeBlocks(value)}</div>
                            )}
                          </div>
                        ) : column.type === 'generalStatus' ? (
                          <StatusDropdown
                            value={value}
                            onChange={(newValue) => handleNewFieldChange(index, column.key, newValue)}
                          />
                        ) : column.type === 'priority' ? (
                          <PrioritySelect
                            value={value || PRIORITY_OPTIONS[3]}
                            onChange={(newValue) => handleNewFieldChange(index, column.key, newValue)}
                          />
                        ) : column.type === 'regression' ? (
                          <div className="regression-cell-content">
                            <RegressionStatusSelect value="" onChange={() => {}} disabled />
                          </div>
                        ) : MULTILINE_TEXT_KEYS.has(column.key) ? (
                          <textarea
                            value={value}
                            onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                            className="cell-textarea multiline-textarea"
                            rows={2}
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
                </tr>
              ))}
              {sortedItems.map((item, rowIndex) => (
                <tr key={item.testId} data-test-id={`tr-data-test-id-${item.testId}`}>
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
                  {TABLE_COLUMNS.map((column) => {
                    const width = getColumnWidth(column);
                    const value = item[column.key] ?? '';
                    const isRegressionColumn = column.type === 'regression';
                    const regressionValue = regressionResults[item.testId] ?? '';
                    const cellClassName = isRegressionColumn
                      ? `regression-cell ${isRegressionRunning ? '' : 'locked'}`.trim()
                      : undefined;
                    const cellDataAttributes = {};

                    if (column.key === 'testId') {
                      cellDataAttributes['data-column'] = 'test-id';
                      cellDataAttributes['data-row-id'] = item.testId;
                    } else if (column.key === 'readyDate') {
                      cellDataAttributes['data-column'] = 'ready-date';
                      cellDataAttributes['data-row-id'] = item.testId;
                    }

                    const readonlySpanAttributes =
                      column.key === 'testId'
                        ? { 'data-test-id-value': value }
                        : column.key === 'readyDate'
                          ? { 'data-ready-date-value': value }
                          : undefined;

                    return (
                      <td
                        key={column.key}
                        style={{ width: `${width}px`, minWidth: `${width}px` }}
                        className={cellClassName}
                        {...cellDataAttributes}
                      >
                        {isRegressionColumn ? (
                          <div className="regression-cell-content">
                            <RegressionStatusSelect
                              value={regressionValue}
                              onChange={(newValue) =>
                                handleRegressionStatusChange(item.testId, newValue)
                              }
                              disabled={!isRegressionRunning || regressionSaving}
                              onFocus={incrementEditingExisting}
                              onBlur={decrementEditingExisting}
                            />
                          </div>
                        ) : column.editable ? (
                          column.type === 'textarea' ? (
                            <div className="textarea-with-preview">
                              <textarea
                                value={value}
                                onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                                onBlur={() => handleBlur(item, column.key)}
                                onFocus={incrementEditingExisting}
                                className="cell-textarea"
                              />
                              {column.key === 'scenario' && value.includes('```') && (
                                <div className="rich-text-preview">{renderTextWithCodeBlocks(value)}</div>
                              )}
                            </div>
                          ) : column.type === 'generalStatus' ? (
                            <StatusDropdown
                              value={value}
                              onChange={(newValue) => handleGeneralStatusChange(item, newValue)}
                              disabled={saving}
                              onFocus={incrementEditingExisting}
                              onBlur={decrementEditingExisting}
                            />
                          ) : column.type === 'priority' ? (
                            <PrioritySelect
                              value={value || PRIORITY_OPTIONS[3]}
                              onChange={(newValue) => handlePriorityChange(item, newValue)}
                              disabled={saving}
                              onFocus={incrementEditingExisting}
                              onBlur={decrementEditingExisting}
                            />
                          ) : MULTILINE_TEXT_KEYS.has(column.key) ? (
                            <textarea
                              value={value}
                              onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                              onBlur={() => handleBlur(item, column.key)}
                              onFocus={incrementEditingExisting}
                              className="cell-textarea multiline-textarea"
                              rows={2}
                            />
                          ) : (
                            <input
                              type="text"
                              value={value}
                              onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                              onBlur={() => handleBlur(item, column.key)}
                              onFocus={incrementEditingExisting}
                              className="cell-input"
                            />
                          )
                        ) : (
                          <span className="readonly-value" {...readonlySpanAttributes}>{value}</span>
                        )}
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
