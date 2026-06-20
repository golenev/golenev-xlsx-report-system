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
const MULTILINE_MIN_HEIGHT = 120;
const SCENARIO_WIDTH_LIMIT = '90ch';

const FIELD_DEFINITIONS = [
  { key: 'testId', label: 'Test ID', dataTestId: 'Test ID', editable: false, type: 'text' },
  {
    key: 'category',
    label: 'Category / Feature',
    dataTestId: 'Category',
    editable: true,
    type: 'text'
  },
  { key: 'shortTitle', label: 'Short Title', dataTestId: 'Short Title', editable: true, type: 'text' },
  {
    key: 'issueLink',
    label: 'YouTrack Issue Link',
    dataTestId: 'YouTrack Issue Link',
    editable: true,
    type: 'text'
  },
  { key: 'readyDate', label: 'Ready Date', dataTestId: 'Ready Date', editable: false, type: 'readonlyDate' },
  {
    key: 'generalStatus',
    label: 'General Test Status',
    dataTestId: 'General Test Status',
    editable: true,
    type: 'generalStatus'
  },
  { key: 'priority', label: 'Priority', dataTestId: 'Priority', editable: true, type: 'priority' },
  {
    key: 'scenario',
    label: 'Detailed Scenario',
    dataTestId: 'Detailed Scenario',
    editable: true,
    type: 'textarea'
  },
  { key: 'notes', label: 'Notes', dataTestId: 'Notes', editable: true, type: 'textarea' }
];

const REGRESSION_COLUMN = {
  key: 'regressionStatus',
  label: 'Regress Run',
  dataTestId: 'Regress Run',
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

function getColumnDataTestId(column) {
  return column.dataTestId ?? column.label ?? column.key;
}

function escapeHtml(rawText) {
  return (rawText || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function escapeHtmlAttribute(rawText) {
  return escapeHtml(rawText).replace(/`/g, '&#96;');
}

function renderMarkdown(text) {
  if (!text?.trim()) {
    return '';
  }

  const normalized = text.replace(/\r\n/g, '\n');
  const lines = normalized.split('\n');

  const blocks = [];
  let inCodeBlock = false;
  let codeLanguage = '';
  let codeLines = [];
  let paragraphLines = [];
  let listBuffer = null;

  const flushParagraph = () => {
    if (!paragraphLines.length) return;
    blocks.push(`<p>${escapeHtml(paragraphLines.join('\n')).replace(/\n/g, '<br>')}</p>`);
    paragraphLines = [];
  };

  const flushList = () => {
    if (!listBuffer) return;
    const items = listBuffer.items
      .map((item) => `<li>${escapeHtml(item)}</li>`) // preserve raw text inside list
      .join('');
    blocks.push(`<${listBuffer.type}>${items}</${listBuffer.type}>`);
    listBuffer = null;
  };

  const flushCode = () => {
    const escaped = escapeHtml(codeLines.join('\n'));
    const langClass = codeLanguage ? ` class="language-${escapeHtmlAttribute(codeLanguage)}"` : '';
    blocks.push(`<pre><code${langClass}>${escaped}</code></pre>`);
    codeLines = [];
    codeLanguage = '';
  };

  for (const line of lines) {
    const trimmedLine = line.trimStart();

    if (inCodeBlock) {
      if (trimmedLine.startsWith('```')) {
        flushCode();
        inCodeBlock = false;
      } else {
        codeLines.push(line);
      }
      continue;
    }

    if (trimmedLine.startsWith('```')) {
      flushParagraph();
      flushList();
      inCodeBlock = true;
      codeLanguage = trimmedLine.slice(3).trim();
      continue;
    }

    const listMatch = /^\s*([-*+]|\d+\.)\s+(.*)/.exec(line);
    if (listMatch) {
      flushParagraph();
      const listType = listMatch[1].endsWith('.') ? 'ol' : 'ul';
      if (!listBuffer || listBuffer.type !== listType) {
        flushList();
        listBuffer = { type: listType, items: [] };
      }
      listBuffer.items.push(listMatch[2]);
      continue;
    }

    if (!line.trim()) {
      flushParagraph();
      flushList();
      continue;
    }

    paragraphLines.push(line);
  }

  if (inCodeBlock) {
    flushCode();
  }

  flushParagraph();
  flushList();

  return blocks.join('\n');
}


const MIN_SCENARIO_STEPS = 2;

function createScenarioStep(text = '', attachment = '') {
  return { text, attachment, attachmentOpen: Boolean(attachment?.trim()) };
}

function ensureEditableScenarioRows(steps) {
  const normalized = steps.map((step) => ({
    text: step.text ?? '',
    attachment: step.attachment ?? '',
    attachmentOpen: Boolean(step.attachmentOpen || step.attachment?.trim())
  }));

  while (normalized.length < MIN_SCENARIO_STEPS) {
    normalized.push(createScenarioStep());
  }

  const lastStep = normalized[normalized.length - 1];
  if (lastStep && (lastStep.text.trim() || lastStep.attachment.trim())) {
    normalized.push(createScenarioStep());
  }

  return normalized;
}

function parseScenarioSteps(rawText) {
  const normalized = (rawText || '').replace(/\r\n/g, '\n');
  if (!normalized.trim()) {
    return ensureEditableScenarioRows([]);
  }

  const lines = normalized.split('\n');
  const steps = [];
  let current = null;
  let attachmentMode = false;

  const pushCurrent = () => {
    if (current) {
      current.text = current.text.replace(/\n+$/g, '');
      current.attachment = current.attachment.replace(/\n+$/g, '');
      steps.push(current);
    }
  };

  lines.forEach((line) => {
    const titleMatch = /^\*\*[^*]+\*\*:\s*$/.exec(line.trim());
    if (titleMatch) return;

    const numberedMatch = /^\s*(?:[-*+]|•)?\s*(\d+(?:\.\d+)*\.?)\s+(.*)$/.exec(line);
    if (numberedMatch && !attachmentMode) {
      pushCurrent();
      current = createScenarioStep(numberedMatch[2]);
      return;
    }

    if (!current) {
      if (!line.trim() || line.trim() === 'Шаги не найдены') return;
      current = createScenarioStep(line.trim());
      return;
    }

    if (line.trim().startsWith('```')) {
      attachmentMode = !attachmentMode;
      current.attachmentOpen = true;
      return;
    }

    if (/^\s*Attachment:/i.test(line)) {
      current.attachmentOpen = true;
      current.attachment += `${current.attachment ? '\n' : ''}${line.trim()}`;
      return;
    }

    if (attachmentMode || current.attachmentOpen) {
      current.attachment += `${current.attachment ? '\n' : ''}${line.replace(/^\s{2,}/, '')}`;
    } else if (line.trim()) {
      current.text += `${current.text ? '\n' : ''}${line.trim()}`;
    }
  });

  pushCurrent();
  return ensureEditableScenarioRows(steps);
}

function serializeScenarioSteps(steps) {
  const meaningfulSteps = steps.filter((step) => step.text.trim() || step.attachment.trim());
  if (!meaningfulSteps.length) return '';

  return meaningfulSteps
    .map((step, index) => {
      const lines = [`${index + 1}. ${step.text.trim()}`];
      if (step.attachment.trim()) {
        lines.push('   ```');
        step.attachment.trim().split('\n').forEach((line) => lines.push(`   ${line}`));
        lines.push('   ```');
      }
      return lines.join('\n');
    })
    .join('\n');
}


function PaperclipIcon() {
  return (
    <svg
      className="attachment-paperclip-icon"
      viewBox="0 0 16 16"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M5.2 8.4l3.9-3.9a2.1 2.1 0 0 1 3 3l-5 5a3.3 3.3 0 0 1-4.7-4.7l5.1-5.1a4.5 4.5 0 1 1 6.4 6.4l-5.1 5.1"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.35"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ScenarioPreview({ value, previewId, activePreviewId, onActivatePreview }) {
  const [openAttachmentIndexes, setOpenAttachmentIndexes] = useState(() => new Set());
  const previewRef = useRef(null);
  const steps = useMemo(
    () => parseScenarioSteps(value).filter((step) => step.text.trim() || step.attachment.trim()),
    [value]
  );

  useEffect(() => {
    if (activePreviewId === previewId) return;
    setOpenAttachmentIndexes(new Set());
  }, [activePreviewId, previewId]);

  useEffect(() => {
    if (!openAttachmentIndexes.size) return undefined;

    const handlePointerDown = (event) => {
      if (previewRef.current?.contains(event.target)) return;
      setOpenAttachmentIndexes(new Set());
    };

    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [openAttachmentIndexes]);

  const toggleAttachment = (index) => {
    onActivatePreview?.(previewId);
    setOpenAttachmentIndexes((currentIndexes) => {
      const nextIndexes = new Set(activePreviewId === previewId ? currentIndexes : []);
      if (nextIndexes.has(index)) {
        nextIndexes.delete(index);
      } else {
        nextIndexes.add(index);
      }
      return nextIndexes;
    });
  };

  if (!steps.length) {
    return <span className="readonly-value">—</span>;
  }

  return (
    <div className="scenario-preview-list" ref={previewRef}>
      {steps.map((step, index) => {
        const hasAttachment = step.attachment.trim().length > 0;
        const isAttachmentOpen = openAttachmentIndexes.has(index);

        return (
          <div className="scenario-preview-step" key={`${index}-${step.text}`}>
            <div className="scenario-preview-step-header">
              <span className="scenario-preview-number">{index + 1}.</span>
              <span className="scenario-preview-text">{step.text.trim()}</span>
              {hasAttachment && (
                <span className="scenario-preview-attachment">
                  <button
                    type="button"
                    className={`scenario-preview-attachment-button ${isAttachmentOpen ? 'open' : ''}`}
                    title="Показать вложение"
                    aria-label={`Показать вложение шага ${index + 1}`}
                    aria-expanded={isAttachmentOpen}
                    onClick={(event) => {
                      event.stopPropagation();
                      toggleAttachment(index);
                    }}
                  >
                    <PaperclipIcon />
                  </button>
                </span>
              )}
            </div>
            {hasAttachment && isAttachmentOpen && (
              <div
                className="scenario-preview-attachment-panel"
                role="region"
                aria-label={`Вложение шага ${index + 1}`}
                onClick={(event) => event.stopPropagation()}
              >
                <div className="scenario-preview-attachment-title">Вложение шага {index + 1}</div>
                <pre className="scenario-preview-attachment-content">{step.attachment.trim()}</pre>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

function ScenarioStepEditor({ value, onChange, onCommit, onFocus, dataTestId, autoFocus = false }) {
  const [steps, setSteps] = useState(() => parseScenarioSteps(value));
  const [openAttachmentRows, setOpenAttachmentRows] = useState(() => new Set());
  const [attachmentDrafts, setAttachmentDrafts] = useState({});
  const firstInputRef = useRef(null);
  const attachmentInputRefs = useRef({});
  const latestValueRef = useRef(value);
  const focusedInsideRef = useRef(false);

  useEffect(() => {
    if (value === latestValueRef.current) return;
    latestValueRef.current = value;
    setSteps(parseScenarioSteps(value));
    setOpenAttachmentRows(new Set());
    setAttachmentDrafts({});
  }, [value]);

  useEffect(() => {
    if (autoFocus) {
      firstInputRef.current?.focus();
    }
  }, [autoFocus]);

  const updateSteps = (producer) => {
    setSteps((prev) => {
      const next = ensureEditableScenarioRows(producer(prev));
      const serialized = serializeScenarioSteps(next);
      latestValueRef.current = serialized;
      onChange(serialized);
      return next;
    });
  };

  const toggleAttachmentRow = (index, attachmentText) => {
    if (openAttachmentRows.has(index)) {
      closeAttachmentRow(index);
      return;
    }
    openAttachmentRow(index, attachmentText);
  };

  const openAttachmentRow = (index, attachmentText = '') => {
    setAttachmentDrafts((drafts) => ({ ...drafts, [index]: attachmentText }));
    setOpenAttachmentRows((prev) => {
      const next = new Set(prev);
      next.add(index);
      return next;
    });
  };

  const closeAttachmentRow = (index) => {
    setOpenAttachmentRows((prev) => {
      const next = new Set(prev);
      next.delete(index);
      return next;
    });
  };

  const saveAttachmentDraft = (index) => {
    const draft = attachmentDrafts[index] ?? '';
    updateSteps((prev) =>
      prev.map((item, itemIndex) =>
        itemIndex === index ? { ...item, attachment: draft, attachmentOpen: false } : item
      )
    );
    closeAttachmentRow(index);
  };

  const handleFocusCapture = () => {
    if (focusedInsideRef.current) return;
    focusedInsideRef.current = true;
    onFocus?.();
  };

  const handleBlurCapture = (event) => {
    if (event.currentTarget.contains(event.relatedTarget)) return;
    focusedInsideRef.current = false;

    const nextSteps = ensureEditableScenarioRows(
      steps.map((step, index) =>
        openAttachmentRows.has(index)
          ? { ...step, attachment: attachmentDrafts[index] ?? step.attachment, attachmentOpen: false }
          : step
      )
    );
    const serialized = serializeScenarioSteps(nextSteps);
    latestValueRef.current = serialized;
    setSteps(nextSteps);
    setOpenAttachmentRows(new Set());
    setAttachmentDrafts({});
    onChange(serialized);
    onCommit(serialized);
  };

  return (
    <div
      className="scenario-step-editor"
      data-test-id={dataTestId}
      onFocusCapture={handleFocusCapture}
      onBlurCapture={handleBlurCapture}
    >
      {steps.map((step, index) => {
        const hasAttachment = step.attachment.trim().length > 0;
        const isAttachmentOpen = openAttachmentRows.has(index);

        return (
          <div className="scenario-step-row" key={index}>
            <div className="scenario-step-line">
              <div className="scenario-step-number">{index + 1}</div>
              <textarea
                ref={index === 0 ? firstInputRef : undefined}
                value={step.text}
                onChange={(event) =>
                  updateSteps((prev) =>
                    prev.map((item, itemIndex) =>
                      itemIndex === index ? { ...item, text: event.target.value } : item
                    )
                  )
                }
                className="cell-textarea scenario-step-input"
                placeholder={index === steps.length - 1 ? 'Добавьте следующий шаг…' : `Шаг ${index + 1}`}
                rows={1}
                wrap="soft"
              />
              {hasAttachment ? (
                <button
                  type="button"
                  className={`attachment-chip ${isAttachmentOpen ? 'open' : ''}`}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => toggleAttachmentRow(index, step.attachment)}
                  aria-expanded={isAttachmentOpen}
                >
                  <PaperclipIcon />
                  <span>Вложение</span>
                  <span className="attachment-chip-caret">{isAttachmentOpen ? '▾' : '▸'}</span>
                </button>
              ) : (
                <button
                  type="button"
                  className="attachment-inline-action"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => openAttachmentRow(index)}
                >
                  + Вложение
                </button>
              )}
            </div>

            <div className={`scenario-attachment-panel ${isAttachmentOpen ? 'open' : ''}`}>
              <div className="scenario-attachment-content">
                <div className="scenario-attachment-header">
                  <span className="scenario-attachment-title">Вложение</span>
                  <div className="scenario-attachment-actions">
                    <button
                      type="button"
                      className="attachment-text-action primary"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => saveAttachmentDraft(index)}
                    >
                      Сохранить
                    </button>
                    {hasAttachment && (
                      <button
                        type="button"
                        className="attachment-text-action"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => attachmentInputRefs.current[index]?.focus()}
                      >
                        Изменить
                      </button>
                    )}
                    {hasAttachment && (
                      <button
                        type="button"
                        className="attachment-text-action danger"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => {
                          updateSteps((prev) =>
                            prev.map((item, itemIndex) =>
                              itemIndex === index ? { ...item, attachment: '', attachmentOpen: false } : item
                            )
                          );
                          setAttachmentDrafts((drafts) => ({ ...drafts, [index]: '' }));
                          closeAttachmentRow(index);
                        }}
                      >
                        Удалить
                      </button>
                    )}
                  </div>
                </div>
                <textarea
                  ref={(element) => {
                    if (element) {
                      attachmentInputRefs.current[index] = element;
                    } else {
                      delete attachmentInputRefs.current[index];
                    }
                  }}
                  value={attachmentDrafts[index] ?? step.attachment}
                  onChange={(event) =>
                    setAttachmentDrafts((drafts) => ({ ...drafts, [index]: event.target.value }))
                  }
                  className="cell-textarea scenario-attachment-input"
                  placeholder="request / response / json / curl"
                  rows={3}
                  wrap="soft"
                />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function autoResizeTextarea(element) {
  if (!element) return;
  element.style.height = 'auto';
  element.style.overflow = 'hidden';
  const nextHeight = Math.max(element.scrollHeight, MULTILINE_MIN_HEIGHT);
  element.style.height = `${nextHeight}px`;
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

function StatusDropdown({
  value,
  onChange,
  disabled = false,
  allowEmpty = true,
  onFocus,
  onBlur,
  dataTestId
}) {
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
    <div
      className="status-dropdown"
      data-test-id={dataTestId}
      onFocusCapture={onFocus}
      onBlurCapture={onBlur}
    >
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

function RegressionStatusSelect({ value, onChange, disabled, onFocus, onBlur, dataTestId }) {
  return (
    <select
      className="regression-select"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      onFocus={onFocus}
      onBlur={onBlur}
      data-test-id={dataTestId}
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

function PrioritySelect({ value, onChange, disabled = false, onFocus, onBlur, dataTestId }) {
  return (
    <select
      className="cell-input"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      onFocus={onFocus}
      onBlur={onBlur}
      data-test-id={dataTestId}
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
  const [translations, setTranslations] = useState({});
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
  const [editingScenarioIds, setEditingScenarioIds] = useState(new Set());
  const [activeScenarioPreviewId, setActiveScenarioPreviewId] = useState(null);
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
      setTranslations(data.translations ?? {});
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

  useEffect(() => {
    const textareas = document.querySelectorAll('.multiline-textarea');
    textareas.forEach(autoResizeTextarea);
  }, [items, newItems]);

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

    setSelectedUploadFiles(files);

    if (files.length === 0) {
      setUploadSelectionLabel('');
      return;
    }

    const firstPath = files[0].webkitRelativePath || files[0].name;
    const folderName = firstPath.includes('/') ? firstPath.split('/')[0] : '';
    const label = folderName ? `${folderName} (${files.length} файлов)` : `${files.length} файлов`;
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

      const params = new URLSearchParams();
      params.set('isRegressRunning', isRegressionRunning ? 'true' : 'false');

      const response = await fetch(withBase(`/uploadReport?${params.toString()}`), {
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

  const handleScenarioCommit = (item, value) => {
    decrementEditingExisting();
    const sanitizedValue = value === '' ? null : value;
    sendUpdate({ ...item, scenario: value }, { scenario: sanitizedValue });
    setEditingScenarioIds((prev) => {
      const next = new Set(prev);
      next.delete(item.testId);
      return next;
    });
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

  const isRequiredDraftFieldFilled = (draft, key) => {
    const value = draft?.[key];
    return typeof value === 'string' ? value.trim() !== '' : value != null && value !== '';
  };

  const isDraftReadyToSave = (draft) =>
    ['testId', 'category', 'shortTitle', 'scenario'].every((key) =>
      isRequiredDraftFieldFilled(draft, key)
    );

  const hasIncompleteNewRow = useMemo(
    () => newItems.some((item) => !isDraftReadyToSave(item)),
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
  const translate = (text) => translations[text] ?? text;
  const hasSelectedFiles = selectedUploadFiles.length > 0;

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
                <div className="popup-subtitle">{translate('Resolve the issue to continue')}</div>
              </div>
            </div>
            <p className="popup-message">{popup.message}</p>
            <div className="popup-actions">
              <button type="button" className="secondary-btn" onClick={closePopup}>
                {translate('Got it')}
              </button>
            </div>
          </div>
        </div>
      )}
      <header className="app-header">
        <h1>{translate('Test Report')}</h1>
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
            {hasSelectedFiles ? (
              <button
                type="button"
                onClick={resetUploadSelection}
                className="secondary-btn"
                disabled={loading || saving || uploading}
              >
                {translate('Cancel')}
              </button>
            ) : (
              <button
                type="button"
                onClick={openUploadPicker}
                className="secondary-btn"
                disabled={loading || saving || uploading}
              >
                {translate('Upload Test Cases')}
              </button>
            )}
            {hasSelectedFiles && (
              <button
                type="button"
                onClick={handleUploadSubmit}
                className="secondary-btn"
                disabled={uploading || loading || saving}
              >
                {uploading ? translate('Uploading…') : translate('Confirm upload')}
              </button>
            )}
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
                hasIncompleteNewRow ||
                isEditingExistingRow
            }
          >
            {translate('Add Row')}
          </button>
          <button type="button" onClick={handleExport} className="ghost-btn">
            {translate('Export to Excel')}
          </button>
          {saving && <span className="status">{translate('Saving…')}</span>}
        </div>
      </header>
      <ReleaseAnalyticsWidget />
      {error && <div className="error-banner">{error}</div>}
      {loading ? (
        <div className="loader">{translate('Loading…')}</div>
      ) : (
        <div className="table-wrapper">
          <table className="report-table">
            <thead>
              <tr>
                <th className="row-index-header">#</th>
                {columns.map((column, idx) => {
                  const width = getColumnWidth(column);
                  const letter = columnLetter(idx);
                  const columnDataTestId = getColumnDataTestId(column);
                  const isScenarioColumn = column.key === 'scenario';
                  const columnSizing = isScenarioColumn
                    ? {
                        width: `min(${width}px, ${SCENARIO_WIDTH_LIMIT})`,
                        minWidth: `min(${width}px, ${SCENARIO_WIDTH_LIMIT})`,
                        maxWidth: SCENARIO_WIDTH_LIMIT
                      }
                    : { width: `${width}px`, minWidth: `${width}px` };
                  return (
                    <th
                      key={column.key}
                      style={columnSizing}
                      {...(columnDataTestId ? { 'data-test-id': columnDataTestId } : undefined)}
                    >
                      <div
                        className={`header-content ${
                          column.key === 'regressionStatus' ? 'regression-header-content' : ''
                        }`}
                      >
                        <div className="header-title">
                          <span className="column-letter">{letter}</span>
                          <span>{translate(column.label)}</span>
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
                                  {`${translate('Regression on release-version')} ${
                                    regressionState.releaseName || '—'
                                  } ${translate('is in progress. Stop it ?')}`}
                                </button>
                                <button
                                  type="button"
                                  className="secondary-btn"
                                  onClick={handleCancelRegression}
                                  disabled={regressionSaving}
                                >
                                  {translate('Cancel Regression')}
                                </button>
                              </>
                            ) : showReleaseNameInput ? (
                              <div className="regression-start-form">
                                <input
                                  type="text"
                                  value={releaseNameDraft}
                                  onChange={(e) => setReleaseNameDraft(e.target.value)}
                                  placeholder={translate('Release name')}
                                  className="cell-input release-input"
                                  disabled={regressionSaving}
                                />
                                <button
                                  type="button"
                                  className="success-btn"
                                  onClick={handleStartRegression}
                                  disabled={regressionSaving || !releaseNameDraft.trim()}
                                >
                                  {translate('Save')}
                                </button>
                                <button
                                  type="button"
                                  className="secondary-btn"
                                  onClick={() => setShowReleaseNameInput(false)}
                                  disabled={regressionSaving}
                                >
                                  {translate('Cancel')}
                                </button>
                              </div>
                            ) : (
                              <button
                                type="button"
                                className="danger-btn"
                                onClick={() => setShowReleaseNameInput(true)}
                                disabled={loading || saving || regressionSaving || regressionLoading}
                              >
                                {translate('Would you run regress')}
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
                      disabled={saving || !isDraftReadyToSave(item)}
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
                    const isMultilineColumn = MULTILINE_TEXT_KEYS.has(column.key);
                    const isScenarioColumn = column.key === 'scenario';
                    const cellDataAttributes = {};
                    const columnDataTestId = getColumnDataTestId(column);

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
                        style={
                          isScenarioColumn
                            ? {
                                width: `min(${width}px, ${SCENARIO_WIDTH_LIMIT})`,
                                minWidth: `min(${width}px, ${SCENARIO_WIDTH_LIMIT})`,
                                maxWidth: SCENARIO_WIDTH_LIMIT
                              }
                            : { width: `${width}px`, minWidth: `${width}px` }
                        }
                        className={
                          [
                            column.type === 'regression' ? 'regression-cell locked' : undefined,
                            isMultilineColumn ? 'multiline-cell' : undefined,
                            isScenarioColumn ? 'scenario-cell' : undefined
                          ]
                            .filter(Boolean)
                            .join(' ') || undefined
                        }
                        {...cellDataAttributes}
                      >
                        {!isEditable ? (
                          <span
                            className="readonly-value"
                            {...readonlySpanAttributes}
                            data-test-id={columnDataTestId}
                          >
                            {value}
                          </span>
                        ) : column.type === 'textarea' ? (
                          isScenarioColumn ? (
                            <ScenarioStepEditor
                              value={value}
                              onChange={(nextValue) => handleNewFieldChange(index, column.key, nextValue)}
                              onCommit={() => {}}
                              dataTestId={columnDataTestId}
                            />
                          ) : (
                            <div className="textarea-with-preview">
                              <textarea
                                value={value}
                                onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                                className="cell-textarea"
                                data-test-id={columnDataTestId}
                              />
                            </div>
                          )
                        ) : column.type === 'generalStatus' ? (
                          <StatusDropdown
                            value={value}
                            onChange={(newValue) => handleNewFieldChange(index, column.key, newValue)}
                            dataTestId={columnDataTestId}
                          />
                        ) : column.type === 'priority' ? (
                          <PrioritySelect
                            value={value || PRIORITY_OPTIONS[3]}
                            onChange={(newValue) => handleNewFieldChange(index, column.key, newValue)}
                            dataTestId={columnDataTestId}
                          />
                        ) : column.type === 'regression' ? (
                          <div className="regression-cell-content">
                            <RegressionStatusSelect
                              value=""
                              onChange={() => {}}
                              disabled
                              dataTestId={columnDataTestId}
                            />
                          </div>
                        ) : isMultilineColumn ? (
                            <div className="multiline-textarea-wrapper">
                              <textarea
                                value={value}
                                onChange={(e) => {
                                  handleNewFieldChange(index, column.key, e.target.value);
                                  autoResizeTextarea(e.target);
                                }}
                                className="cell-textarea multiline-textarea"
                                data-test-id={columnDataTestId}
                              />
                            </div>
                          ) : (
                          <input
                            type="text"
                            value={value}
                            onChange={(e) => handleNewFieldChange(index, column.key, e.target.value)}
                            className="cell-input"
                            data-test-id={columnDataTestId}
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
                    const isScenarioColumn = column.key === 'scenario';
                    const isEditingScenario =
                      isScenarioColumn && editingScenarioIds.has(item.testId);
                    const isRegressionColumn = column.type === 'regression';
                    const regressionValue = regressionResults[item.testId] ?? '';
                    const isMultilineColumn = MULTILINE_TEXT_KEYS.has(column.key);
                    const cellClassName =
                      [
                        isRegressionColumn
                          ? `regression-cell ${isRegressionRunning ? '' : 'locked'}`.trim()
                          : undefined,
                        isMultilineColumn ? 'multiline-cell' : undefined,
                        isScenarioColumn ? 'scenario-cell' : undefined
                      ]
                        .filter(Boolean)
                        .join(' ') || undefined;
                    const cellDataAttributes = {};
                    const columnDataTestId = getColumnDataTestId(column);

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
                        style={
                          isScenarioColumn
                            ? {
                                width: `min(${width}px, ${SCENARIO_WIDTH_LIMIT})`,
                                minWidth: `min(${width}px, ${SCENARIO_WIDTH_LIMIT})`,
                                maxWidth: SCENARIO_WIDTH_LIMIT
                              }
                            : { width: `${width}px`, minWidth: `${width}px` }
                        }
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
                              dataTestId={columnDataTestId}
                            />
                          </div>
                        ) : column.editable ? (
                          isScenarioColumn ? (
                            isEditingScenario ? (
                              <ScenarioStepEditor
                                value={value}
                                onChange={(nextValue) => handleFieldChange(item.testId, column.key, nextValue)}
                                onCommit={(nextValue) => handleScenarioCommit(item, nextValue)}
                                onFocus={incrementEditingExisting}
                                dataTestId={columnDataTestId}
                                autoFocus
                              />
                            ) : (
                              <div
                                className="markdown-preview-wrapper scenario-preview"
                                data-test-id={columnDataTestId}
                                onClick={() =>
                                  setEditingScenarioIds((prev) => {
                                    const next = new Set(prev);
                                    next.add(item.testId);
                                    return next;
                                  })
                                }
                              >
                                <ScenarioPreview
                                  value={value}
                                  previewId={item.testId}
                                  activePreviewId={activeScenarioPreviewId}
                                  onActivatePreview={setActiveScenarioPreviewId}
                                />
                              </div>
                            )
                          ) : column.type === 'textarea' ? (
                            <div className="textarea-with-preview">
                              <textarea
                                value={value}
                                onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                                onBlur={() => handleBlur(item, column.key)}
                                onFocus={incrementEditingExisting}
                                className="cell-textarea"
                                data-test-id={columnDataTestId}
                              />
                              {column.key === 'scenario' && value.trim() && (
                                <div
                                  className="rich-text-preview markdown-preview"
                                  dangerouslySetInnerHTML={{ __html: renderMarkdown(value) }}
                                />
                              )}
                            </div>
                          ) : column.type === 'generalStatus' ? (
                            <StatusDropdown
                              value={value}
                              onChange={(newValue) => handleGeneralStatusChange(item, newValue)}
                              disabled={saving}
                              onFocus={incrementEditingExisting}
                              onBlur={decrementEditingExisting}
                              dataTestId={columnDataTestId}
                            />
                          ) : column.type === 'priority' ? (
                            <PrioritySelect
                              value={value || PRIORITY_OPTIONS[3]}
                              onChange={(newValue) => handlePriorityChange(item, newValue)}
                              disabled={saving}
                              onFocus={incrementEditingExisting}
                              onBlur={decrementEditingExisting}
                              dataTestId={columnDataTestId}
                            />
                          ) : isMultilineColumn ? (
                            <div className="multiline-textarea-wrapper">
                              <textarea
                                value={value}
                                onChange={(e) => {
                                  handleFieldChange(item.testId, column.key, e.target.value);
                                  autoResizeTextarea(e.target);
                                }}
                                onBlur={() => handleBlur(item, column.key)}
                                onFocus={incrementEditingExisting}
                                className="cell-textarea multiline-textarea"
                                data-test-id={columnDataTestId}
                              />
                            </div>
                          ) : (
                            <input
                              type="text"
                              value={value}
                              onChange={(e) => handleFieldChange(item.testId, column.key, e.target.value)}
                              onBlur={() => handleBlur(item, column.key)}
                              onFocus={incrementEditingExisting}
                              className="cell-input"
                              data-test-id={columnDataTestId}
                            />
                          )
                        ) : (
                          <span
                            className="readonly-value"
                            {...readonlySpanAttributes}
                            data-test-id={columnDataTestId}
                          >
                            {value}
                          </span>
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
