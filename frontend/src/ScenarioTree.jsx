import { useMemo, useState } from 'react';
import { countAttachments, countDescendants, formatDuration, formatSize, hasStepContent, normalizeScenario } from './scenarioModel';

function countLabel(count, singular, plural = `${singular}s`) {
  return count ? `${count} ${count === 1 ? singular : plural}` : null;
}

function Chevron({ expanded, disabled = false, onClick, label, testId }) {
  return disabled
    ? <span className="scenario-chevron-spacer" aria-hidden="true" />
    : <button type="button" className={`scenario-chevron ${expanded ? 'expanded' : ''}`} aria-label={label} aria-expanded={expanded} onClick={onClick} data-testid={testId} />;
}

function ScenarioAttachment({ attachment, path }) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div className="scenario-tree-attachment" data-testid="scenario-attachment-item" data-scenario-path={path}>
      <div className="scenario-attachment-line">
        <Chevron expanded={expanded} onClick={() => setExpanded((value) => !value)} label={`${expanded ? 'Свернуть' : 'Развернуть'} вложение`} testId="scenario-attachment-button" />
        <span className="scenario-attachment-name">{attachment.name || 'Attachment'}</span>
        {attachment.sizeBytes != null && <span className="scenario-tree-meta">{formatSize(attachment.sizeBytes)}</span>}
      </div>
      <pre hidden={!expanded} className="scenario-tree-attachment-content" data-testid="scenario-attachment-content">{attachment.content}</pre>
    </div>
  );
}

function ScenarioTreeNode({ step, path }) {
  const [expanded, setExpanded] = useState(false);
  const subSteps = step.subSteps.filter(hasStepContent);
  const hasDetails = subSteps.length > 0 || step.parameters.length > 0 || step.attachments.length > 0;
  const meta = [
    countLabel(countDescendants(step), 'sub-step'),
    countLabel(step.parameters.length, 'parameter'),
    countLabel(countAttachments(step), 'attachment')
  ].filter(Boolean).join(', ');
  return (
    <div className="scenario-tree-node" data-testid="scenario-step" data-scenario-path={path}>
      <div className="scenario-tree-line" data-testid="scenario-step-header">
        <Chevron expanded={expanded} disabled={!hasDetails} onClick={() => setExpanded((value) => !value)} label={`${expanded ? 'Свернуть' : 'Развернуть'} шаг`} testId="scenario-step-toggle" />
        <span className="scenario-tree-text" data-testid="scenario-step-text">{step.text}</span>
        {meta && <span className="scenario-tree-meta">{meta}</span>}
        {step.durationMs != null && <span className="scenario-tree-duration">{formatDuration(step.durationMs)}</span>}
      </div>
      {hasDetails && (
        <div hidden={!expanded} className="scenario-tree-children" data-testid="scenario-step-details">
          {step.parameters.length > 0 && <div className="scenario-parameters">{step.parameters.map((parameter, index) => <div className="scenario-parameter" key={`${parameter.name}-${index}`}><span>{parameter.name}</span><span>{parameter.value}</span></div>)}</div>}
          {step.attachments.map((attachment, index) => <ScenarioAttachment attachment={attachment} path={`${path}.a${index}`} key={`${path}.a${index}`} />)}
          {subSteps.map((child, index) => <ScenarioTreeNode step={child} path={`${path}.${index}`} key={`${path}.${index}`} />)}
        </div>
      )}
    </div>
  );
}

export function ScenarioTree({ value }) {
  const steps = useMemo(() => normalizeScenario(value).steps.filter(hasStepContent), [value]);
  if (!steps.length) return <span className="readonly-value">—</span>;
  return <div className="scenario-tree" data-testid="scenario-preview">{steps.map((step, index) => <ScenarioTreeNode step={step} path={String(index)} key={String(index)} />)}</div>;
}
