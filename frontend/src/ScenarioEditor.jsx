import { useEffect, useState } from 'react';
import { createScenarioAttachment, createScenarioStep, MAX_ATTACHMENTS_PER_STEP, normalizeScenario, removeStepAtPath, serializeScenario, updateStepAtPath } from './scenarioModel';

function StepEditor({ step, path, onUpdate, onRemove }) {
  const update = (updater) => onUpdate(path, updater);
  return <div className="scenario-editor-node" data-testid="scenario-editor-step" data-scenario-path={path.join('.')}>
    <div className="scenario-editor-line">
      <textarea className="cell-textarea scenario-step-input" data-testid="scenario-step-input" value={step.text} onChange={(event) => update((current) => ({ ...current, text: event.target.value }))} rows={2} />
      <button type="button" className="attachment-inline-action" onClick={() => update((current) => ({ ...current, subSteps: [...current.subSteps, createScenarioStep()] }))}>+ Подшаг</button>
      <button type="button" className="attachment-text-action danger" onClick={() => onRemove(path)}>Удалить</button>
    </div>
    {step.parameters.length > 0 && <div className="scenario-editor-readonly">Параметры выполнения сохраняются без изменений: {step.parameters.length}</div>}
    <div className="scenario-editor-attachments">
      {step.attachments.map((attachment, index) => <div className="scenario-editor-attachment" key={index} data-testid="scenario-editor-attachment" data-attachment-index={index}>
        <input value={attachment.name} aria-label="Название вложения" onChange={(event) => update((current) => ({ ...current, attachments: current.attachments.map((item, itemIndex) => itemIndex === index ? { ...item, name: event.target.value } : item) }))} />
        <textarea value={attachment.content} data-testid="scenario-attachment-content" aria-label="Содержимое вложения" onChange={(event) => update((current) => ({ ...current, attachments: current.attachments.map((item, itemIndex) => itemIndex === index ? { ...item, content: event.target.value } : item) }))} rows={2} />
        <button type="button" className="attachment-text-action danger" onClick={() => update((current) => ({ ...current, attachments: current.attachments.filter((_, itemIndex) => itemIndex !== index) }))}>Удалить вложение</button>
      </div>)}
      {step.attachments.length < MAX_ATTACHMENTS_PER_STEP && <button type="button" className="attachment-inline-action" data-testid="scenario-attachment-add-button" onClick={() => update((current) => ({ ...current, attachments: [...current.attachments, createScenarioAttachment()] }))}>+ Вложение</button>}
    </div>
    {step.subSteps.length > 0 && <div className="scenario-editor-children">{step.subSteps.map((child, index) => <StepEditor key={index} step={child} path={[...path, index]} onUpdate={onUpdate} onRemove={onRemove} />)}</div>}
  </div>;
}

export function ScenarioEditor({ value, onSave, onCancel }) {
  const editableScenario = (scenarioValue) => {
    const scenario = normalizeScenario(scenarioValue);
    return scenario.steps.length ? scenario : { steps: [createScenarioStep()] };
  };
  const [draft, setDraft] = useState(() => editableScenario(value));
  useEffect(() => setDraft(editableScenario(value)), [value]);
  const update = (path, updater) => setDraft((current) => ({ ...current, steps: updateStepAtPath(current.steps, path, updater) }));
  const remove = (path) => setDraft((current) => ({ ...current, steps: removeStepAtPath(current.steps, path) }));
  return <div className="scenario-editor" data-testid="scenario-editor">
    {draft.steps.map((step, index) => <StepEditor key={index} step={step} path={[index]} onUpdate={update} onRemove={remove} />)}
    <div className="scenario-editor-actions">
      <button type="button" className="attachment-inline-action" data-testid="scenario-root-add" onClick={() => setDraft((current) => ({ ...current, steps: [...current.steps, createScenarioStep()] }))}>+ Корневой шаг</button>
      <button type="button" className="secondary-btn" data-testid="scenario-save" onClick={() => onSave(serializeScenario(draft))}>Сохранить</button>
      <button type="button" className="ghost-btn" data-testid="scenario-cancel" onClick={onCancel}>Отмена</button>
    </div>
  </div>;
}
