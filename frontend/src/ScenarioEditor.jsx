import { useEffect, useRef, useState } from 'react';
import { createScenarioAttachment, createScenarioStep, MAX_ATTACHMENTS_PER_STEP, normalizeScenario, removeStepAtPath, serializeScenario, updateStepAtPath } from './scenarioModel';

function AttachmentEditor({ attachment, index, onChange, onRemove }) {
  const [expanded, setExpanded] = useState(false);
  const displayName = attachment.name?.trim() || `Вложение ${index + 1}`;

  return <div className={`scenario-editor-attachment ${expanded ? 'expanded' : ''}`} data-testid="scenario-editor-attachment" data-attachment-index={index}>
    <div className="scenario-attachment-summary">
      <span className="scenario-paperclip" aria-hidden="true">↗</span>
      <input className="scenario-attachment-heading" value={attachment.name} aria-label={displayName} placeholder={displayName} onChange={(event) => onChange({ ...attachment, name: event.target.value })} />
      {attachment.mediaType && <span className="scenario-attachment-type">{attachment.mediaType}</span>}
      <button type="button" className={`scenario-chevron scenario-chevron-step ${expanded ? 'expanded' : ''}`} onClick={() => setExpanded((current) => !current)} aria-expanded={expanded} aria-label={`${expanded ? 'Свернуть' : 'Развернуть'} вложение ${displayName}`} />
    </div>
    {expanded && <div className="scenario-attachment-fields">
      <label>
        <span>Содержимое</span>
        <textarea value={attachment.content} data-testid="scenario-attachment-content" aria-label="Содержимое вложения" onChange={(event) => onChange({ ...attachment, content: event.target.value })} rows={5} />
      </label>
      <button type="button" className="scenario-danger-action" onClick={onRemove}>Удалить вложение</button>
    </div>}
  </div>;
}

function StepEditor({ step, path, number, onUpdate, onRemove }) {
  const [expanded, setExpanded] = useState(false);
  const update = (updater) => onUpdate(path, updater);
  const detailCount = step.subSteps.length + step.parameters.length + step.attachments.length;

  return <section className="scenario-editor-node" data-testid="scenario-editor-step" data-scenario-path={path.join('.')}>
    <div className="scenario-step-toggle">
      <span className="scenario-step-index">{number}</span>
      <input className="scenario-step-heading" data-testid="scenario-step-input" value={step.text} onChange={(event) => update((current) => ({ ...current, text: event.target.value }))} aria-label={`Описание шага ${number}`} placeholder="Новый шаг" />
      {detailCount > 0 && <span className="scenario-step-counter">{detailCount}</span>}
      <button type="button" className={`scenario-chevron scenario-chevron-step ${expanded ? 'expanded' : ''}`} onClick={() => setExpanded((current) => !current)} aria-expanded={expanded} aria-label={`${expanded ? 'Свернуть' : 'Развернуть'} шаг ${number}`} />
    </div>
    {expanded && <div className="scenario-step-card-body">
      {step.parameters.length > 0 && <div className="scenario-editor-readonly">Параметры выполнения <strong>{step.parameters.length}</strong> · сохраняются без изменений</div>}

      <div className="scenario-editor-section">
        <div className="scenario-editor-section-title">
          <span>Вложения <strong>{step.attachments.length}</strong></span>
          {step.attachments.length < MAX_ATTACHMENTS_PER_STEP && <button type="button" className="scenario-link-action" data-testid="scenario-attachment-add-button" onClick={() => update((current) => ({ ...current, attachments: [...current.attachments, createScenarioAttachment()] }))}>+ Вложение</button>}
        </div>
        {step.attachments.length > 0 && <div className="scenario-editor-attachments">
          {step.attachments.map((attachment, index) => <AttachmentEditor
            key={index}
            attachment={attachment}
            index={index}
            onChange={(nextAttachment) => update((current) => ({ ...current, attachments: current.attachments.map((item, itemIndex) => itemIndex === index ? nextAttachment : item) }))}
            onRemove={() => update((current) => ({ ...current, attachments: current.attachments.filter((_, itemIndex) => itemIndex !== index) }))}
          />)}
        </div>}
      </div>

      <div className="scenario-step-actions">
        <button type="button" className="scenario-link-action" onClick={() => update((current) => ({ ...current, subSteps: [...current.subSteps, createScenarioStep()] }))}>+ Подшаг</button>
        <button type="button" className="scenario-danger-action" onClick={() => onRemove(path)}>Удалить шаг</button>
      </div>
    </div>}
    {step.subSteps.length > 0 && <div className="scenario-editor-children">{step.subSteps.map((child, index) => <StepEditor key={index} step={child} path={[...path, index]} number={`${number}.${index + 1}`} onUpdate={onUpdate} onRemove={onRemove} />)}</div>}
  </section>;
}

export function ScenarioEditor({ value, onSave, onCancel, onChange, showActions = true }) {
  const editableScenario = (scenarioValue) => {
    const scenario = normalizeScenario(scenarioValue);
    return scenario.steps.length ? scenario : { steps: [createScenarioStep()] };
  };
  const [draft, setDraft] = useState(() => editableScenario(value));
  const lastEmittedValue = useRef(null);
  useEffect(() => {
    const serializedValue = JSON.stringify(value);
    if (serializedValue === lastEmittedValue.current) {
      lastEmittedValue.current = null;
      return;
    }
    setDraft(editableScenario(value));
  }, [value]);
  const commitDraft = (updater) => setDraft((current) => {
    const next = updater(current);
    const serialized = serializeScenario(next);
    lastEmittedValue.current = JSON.stringify(serialized);
    onChange?.(serialized);
    return next;
  });
  const update = (path, updater) => commitDraft((current) => ({ ...current, steps: updateStepAtPath(current.steps, path, updater) }));
  const remove = (path) => commitDraft((current) => ({ ...current, steps: removeStepAtPath(current.steps, path) }));

  return <div className="scenario-editor" data-testid="scenario-editor">
    <div className="scenario-editor-intro">
      <div>
        <strong>Шаги сценария</strong>
        <span>Раскрывайте только тот шаг, с которым работаете.</span>
      </div>
      <span className="scenario-editor-total">{draft.steps.length} корневых</span>
    </div>
    <div className="scenario-editor-steps">
      {draft.steps.map((step, index) => <StepEditor key={index} step={step} path={[index]} number={index + 1} onUpdate={update} onRemove={remove} />)}
    </div>
    <button type="button" className="scenario-add-root" data-testid="scenario-root-add" onClick={() => commitDraft((current) => ({ ...current, steps: [...current.steps, createScenarioStep()] }))}>+ Добавить корневой шаг</button>
    {showActions && <div className="scenario-editor-actions">
      <button type="button" className="ghost-btn" data-testid="scenario-cancel" onClick={onCancel}>Отмена</button>
      <button type="button" className="secondary-btn scenario-save-button" data-testid="scenario-save" onClick={() => onSave(serializeScenario(draft))}>Сохранить сценарий</button>
    </div>}
  </div>;
}
