import { useEffect, useMemo, useState } from 'react';
import { ScenarioEditor } from './ScenarioEditor.jsx';

export function TestCaseEditorModal({
  mode,
  value,
  saving,
  generalStatusOptions,
  priorityOptions,
  onSave,
  onClose
}) {
  const [draft, setDraft] = useState(value);
  const [confirmClose, setConfirmClose] = useState(false);
  const initialSnapshot = useMemo(() => JSON.stringify(value), [value]);
  const dirty = JSON.stringify(draft) !== initialSnapshot;
  const canSave = Boolean(draft.testId?.trim() && draft.category?.trim() && draft.shortTitle?.trim() && draft.scenario);

  useEffect(() => {
    setDraft(value);
    setConfirmClose(false);
  }, [value]);

  const requestClose = () => {
    if (dirty) {
      setConfirmClose(true);
      return;
    }
    onClose();
  };

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      if (confirmClose) {
        setConfirmClose(false);
        return;
      }
      requestClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [confirmClose, dirty]);

  useEffect(() => {
    if (!dirty) return undefined;
    const handleBeforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [dirty]);

  const change = (key, nextValue) => {
    setDraft((current) => ({ ...current, [key]: nextValue }));
  };

  return <div className="test-case-modal-layer" role="presentation" data-testid="test-case-editor-modal">
    <div className="test-case-modal-backdrop" aria-hidden="true" onMouseDown={requestClose} />
    <section className="test-case-modal" role="dialog" aria-modal="true" aria-labelledby="test-case-modal-title">
      <header className="test-case-modal-header">
        <div>
          <span className="test-case-modal-eyebrow">{mode === 'create' ? 'Новый тест-кейс' : 'Редактирование тест-кейса'}</span>
          <h2 id="test-case-modal-title">{draft.shortTitle?.trim() || 'Без названия'}</h2>
          <span className="test-case-modal-id">{draft.testId?.trim() || 'Test ID не указан'}</span>
        </div>
        <button type="button" className="test-case-modal-close" aria-label="Закрыть редактор" onClick={requestClose}>×</button>
      </header>

      <div className="test-case-modal-content">
        <div className="test-case-modal-fields">
          <label data-role="field" data-name="Test ID">
            <span>Test ID</span>
            <input value={draft.testId || ''} disabled={mode === 'edit'} onChange={(event) => change('testId', event.target.value)} data-testid="test-id-input" />
          </label>
          <label data-role="field" data-name="Priority">
            <span>Priority</span>
            <select value={draft.priority || priorityOptions[3]} onChange={(event) => change('priority', event.target.value)} data-testid="priority-select">
              {priorityOptions.map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
          </label>
          <label className="test-case-modal-field-wide" data-role="field" data-name="Category / Feature">
            <span>Category / Feature</span>
            <textarea value={draft.category || ''} onChange={(event) => change('category', event.target.value)} rows={2} data-testid="category-input" />
          </label>
          <label className="test-case-modal-field-wide" data-role="field" data-name="Short Title">
            <span>Short Title</span>
            <textarea value={draft.shortTitle || ''} onChange={(event) => change('shortTitle', event.target.value)} rows={2} data-testid="short-title-input" />
          </label>
          <label className="test-case-modal-field-wide" data-role="field" data-name="YouTrack Issue Link">
            <span>YouTrack Issue Link</span>
            <input value={draft.issueLink || ''} onChange={(event) => change('issueLink', event.target.value)} data-testid="youtrack-link" />
          </label>
          <label data-role="field" data-name="General Test Status">
            <span>General Test Status</span>
            <select value={draft.generalStatus || ''} onChange={(event) => change('generalStatus', event.target.value)} data-testid="status-dropdown">
              <option value="">—</option>
              {generalStatusOptions.map((option) => <option key={option.value} value={option.value}>{option.value}</option>)}
            </select>
          </label>
          <label data-role="field" data-name="Ready Date">
            <span>Ready Date</span>
            <input value={draft.readyDate || ''} disabled data-testid="ready-date-input" />
          </label>
        </div>

        <section className="test-case-modal-scenario" aria-labelledby="test-case-scenario-title" data-role="field" data-name="Detailed Scenario">
          <div className="test-case-modal-section-title">
            <div>
              <h3 id="test-case-scenario-title">Детальный сценарий</h3>
              <span>Шаги, подшаги и вложения</span>
            </div>
          </div>
          <ScenarioEditor value={draft.scenario || ''} onChange={(nextValue) => change('scenario', nextValue)} showActions={false} />
        </section>

        <label className="test-case-modal-notes" data-role="field" data-name="Notes">
          <span>Notes</span>
          <textarea value={draft.notes || ''} onChange={(event) => change('notes', event.target.value)} rows={4} data-testid="notes-input" />
        </label>
      </div>

      <footer className="test-case-modal-footer">
        <span className={dirty ? 'test-case-modal-dirty is-dirty' : 'test-case-modal-dirty'}>{dirty ? 'Есть несохранённые изменения' : 'Нет изменений'}</span>
        <button type="button" className="secondary-btn" disabled={saving || !canSave} onClick={() => onSave(draft)} data-testid="save-test-case-button" data-role="button" data-action="save-test-case">
          {saving ? 'Сохранение…' : 'Сохранить'}
        </button>
      </footer>
    </section>

    {confirmClose && <div className="unsaved-confirm-layer" role="presentation">
      <section className="unsaved-confirm" role="alertdialog" aria-modal="true" aria-labelledby="unsaved-confirm-title" aria-describedby="unsaved-confirm-description">
        <h3 id="unsaved-confirm-title">Сохранить изменения?</h3>
        <p id="unsaved-confirm-description">У вас есть несохранённые изменения.</p>
        <div className="unsaved-confirm-actions">
          <button type="button" className="secondary-btn" onClick={() => setConfirmClose(false)}>Продолжить редактирование</button>
          <button type="button" className="ghost-btn unsaved-discard" onClick={onClose}>Не сохранять</button>
        </div>
      </section>
    </div>}
  </div>;
}
