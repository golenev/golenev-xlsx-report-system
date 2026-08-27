import { useEffect, useMemo, useState } from 'react';
import { buildScenarioStepNumbers, collectScenarioExpansionPaths, countAttachments, countDescendants, formatDuration, formatSize, hasStepContent, normalizeScenario } from './scenarioModel';

function countLabel(count, singular, plural = `${singular}s`) {
  return count ? `${count} ${count === 1 ? singular : plural}` : null;
}

function Chevron({ expanded, disabled = false, onClick, label, testId, variant = 'step' }) {
  return disabled
    ? <span className="scenario-chevron-spacer" aria-hidden="true" />
    : <button type="button" className={`scenario-chevron scenario-chevron-${variant} ${expanded ? 'expanded' : ''}`} aria-label={label} aria-expanded={expanded} onClick={onClick} data-testid={testId} />;
}

function ScenarioAttachment({ attachment, path, expanded, onToggle }) {
  return (
    <div className="scenario-tree-attachment" data-testid="scenario-attachment-item" data-scenario-path={path}>
      <div className="scenario-attachment-line">
        <Chevron expanded={expanded} onClick={() => onToggle(path)} label={`${expanded ? 'Свернуть' : 'Развернуть'} вложение`} testId="scenario-attachment-button" variant="attachment" />
        <span className="scenario-attachment-name">{attachment.name || 'Attachment'}</span>
        {attachment.sizeBytes != null && <span className="scenario-tree-meta">{formatSize(attachment.sizeBytes)}</span>}
      </div>
      <pre hidden={!expanded} className="scenario-tree-attachment-content" data-testid="scenario-attachment-content">{attachment.content}</pre>
    </div>
  );
}

function ScenarioTreeNode({ step, path, stepNumbers, expandedStepPaths, expandedAttachmentPaths, onToggleStep, onToggleAttachment }) {
  const expanded = expandedStepPaths.has(path);
  const subSteps = step.subSteps.filter(hasStepContent);
  const hasDetails = subSteps.length > 0 || step.parameters.length > 0 || step.attachments.length > 0;
  const meta = [
    countLabel(countDescendants(step), 'sub-step'),
    countLabel(step.parameters.length, 'parameter'),
    countLabel(countAttachments(step), 'attachment')
  ].filter(Boolean).join(', ');
  return (
    <div className="scenario-tree-node" data-testid="scenario-step" data-scenario-path={path} data-step-number={stepNumbers.get(path)}>
      <div className="scenario-tree-line" data-testid="scenario-step-header" data-step-number={stepNumbers.get(path)}>
        <Chevron expanded={expanded} disabled={!hasDetails} onClick={() => onToggleStep(path)} label={`${expanded ? 'Свернуть' : 'Развернуть'} шаг`} testId="scenario-step-toggle" />
        <span className="scenario-tree-number" data-testid="scenario-step-number">{stepNumbers.get(path)}.</span>
        <span className="scenario-tree-text" data-testid="scenario-step-text">{step.text}</span>
        {meta && <span className="scenario-tree-meta">{meta}</span>}
        {step.durationMs != null && <span className="scenario-tree-duration">{formatDuration(step.durationMs)}</span>}
      </div>
      {hasDetails && (
        <div hidden={!expanded} className="scenario-tree-children" data-testid="scenario-step-details">
          {step.parameters.length > 0 && <div className="scenario-parameters">{step.parameters.map((parameter, index) => <div className="scenario-parameter" key={`${parameter.name}-${index}`}><span>{parameter.name}</span><span>{parameter.value}</span></div>)}</div>}
          {step.attachments.map((attachment, index) => {
            const attachmentPath = `${path}.a${index}`;
            return <ScenarioAttachment attachment={attachment} path={attachmentPath} expanded={expandedAttachmentPaths.has(attachmentPath)} onToggle={onToggleAttachment} key={attachmentPath} />;
          })}
          {subSteps.map((child, index) => <ScenarioTreeNode step={child} path={`${path}.${index}`} stepNumbers={stepNumbers} expandedStepPaths={expandedStepPaths} expandedAttachmentPaths={expandedAttachmentPaths} onToggleStep={onToggleStep} onToggleAttachment={onToggleAttachment} key={`${path}.${index}`} />)}
        </div>
      )}
    </div>
  );
}

export function ScenarioTree({ value, onEdit }) {
  const [expandedStepPaths, setExpandedStepPaths] = useState(() => new Set());
  const [expandedAttachmentPaths, setExpandedAttachmentPaths] = useState(() => new Set());
  const steps = useMemo(() => normalizeScenario(value).steps.filter(hasStepContent), [value]);
  const stepNumbers = useMemo(() => buildScenarioStepNumbers(steps), [steps]);
  const expansionPaths = useMemo(() => collectScenarioExpansionPaths(steps), [steps]);
  const hasExpandableContent = expansionPaths.stepPaths.length > 0 || expansionPaths.attachmentPaths.length > 0;
  const allExpanded = hasExpandableContent
    && expansionPaths.stepPaths.every((path) => expandedStepPaths.has(path))
    && expansionPaths.attachmentPaths.every((path) => expandedAttachmentPaths.has(path));
  useEffect(() => {
    setExpandedStepPaths(new Set());
    setExpandedAttachmentPaths(new Set());
  }, [value]);
  const togglePath = (setter, path) => setter((current) => {
    const next = new Set(current);
    if (next.has(path)) next.delete(path); else next.add(path);
    return next;
  });
  const toggleAll = () => {
    if (allExpanded) {
      setExpandedStepPaths(new Set());
      setExpandedAttachmentPaths(new Set());
    } else {
      setExpandedStepPaths(new Set(expansionPaths.stepPaths));
      setExpandedAttachmentPaths(new Set(expansionPaths.attachmentPaths));
    }
  };
  if (!steps.length) return <span className="readonly-value">—</span>;
  return <div className="scenario-tree" data-testid="scenario-preview">
    <div className="scenario-tree-toolbar">
      <Chevron expanded={allExpanded} disabled={!hasExpandableContent} onClick={toggleAll} label={allExpanded ? 'Свернуть весь сценарий' : 'Развернуть весь сценарий'} testId="scenario-toggle-all" />
      <button type="button" className="scenario-edit-button" data-testid="scenario-edit" onClick={onEdit}>Изменить</button>
    </div>
    {steps.map((step, index) => <ScenarioTreeNode step={step} path={String(index)} stepNumbers={stepNumbers} expandedStepPaths={expandedStepPaths} expandedAttachmentPaths={expandedAttachmentPaths} onToggleStep={(path) => togglePath(setExpandedStepPaths, path)} onToggleAttachment={(path) => togglePath(setExpandedAttachmentPaths, path)} key={String(index)} />)}
  </div>;
}
