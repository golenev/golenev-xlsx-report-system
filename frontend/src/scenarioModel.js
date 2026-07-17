export const MAX_ATTACHMENTS_PER_STEP = 10;

export function createScenarioAttachment(attachment = {}) {
  return {
    name: attachment.name ?? attachment.type ?? 'Attachment',
    mediaType: attachment.mediaType ?? null,
    content: attachment.content ?? '',
    source: attachment.source ?? null,
    sizeBytes: Number.isFinite(attachment.sizeBytes) && attachment.sizeBytes >= 0 ? attachment.sizeBytes : null
  };
}

export function createScenarioStep(step = {}) {
  const legacyAttachments = !Array.isArray(step.attachments) && step.attachment?.trim()
    ? [{ name: 'Attachment', mediaType: 'text/plain', content: step.attachment }]
    : [];
  return {
    number: Number.isFinite(step.number) ? step.number : null,
    text: step.text ?? '',
    durationMs: Number.isFinite(step.durationMs) && step.durationMs >= 0 ? step.durationMs : null,
    parameters: Array.isArray(step.parameters)
      ? step.parameters.map((parameter) => ({ name: parameter.name ?? '', value: parameter.value ?? '' }))
      : [],
    attachments: (Array.isArray(step.attachments) ? step.attachments : legacyAttachments).map(createScenarioAttachment),
    subSteps: Array.isArray(step.subSteps) ? step.subSteps.map(createScenarioStep) : []
  };
}

function parseLegacyScenario(value) {
  const steps = String(value ?? '').replace(/\r\n/g, '\n').split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => createScenarioStep({ number: index + 1, text: line.replace(/^\d+(?:\.\d+)*\.?\s+/, '') }));
  return { steps };
}

export function normalizeScenario(value) {
  if (value && typeof value === 'object' && Array.isArray(value.steps)) {
    return { steps: value.steps.map(createScenarioStep) };
  }
  if (typeof value === 'string' && value.trim().startsWith('{')) {
    try {
      return normalizeScenario(JSON.parse(value));
    } catch (_) {
      return parseLegacyScenario(value);
    }
  }
  return parseLegacyScenario(value);
}

export function hasStepContent(step) {
  return Boolean(step.text.trim() || step.parameters.length || step.attachments.length || step.subSteps.some(hasStepContent));
}

function serializeStep(step, index) {
  return {
    number: step.number ?? index + 1,
    text: step.text.trim(),
    durationMs: step.durationMs,
    parameters: step.parameters.map((parameter) => ({ ...parameter })),
    attachments: step.attachments.map((attachment) => ({ ...attachment })),
    subSteps: step.subSteps.filter(hasStepContent).map(serializeStep)
  };
}

export function serializeScenario(value) {
  const scenario = normalizeScenario(value);
  const steps = scenario.steps.filter(hasStepContent).map(serializeStep);
  return steps.length ? { steps } : null;
}

export function updateStepAtPath(steps, path, updater) {
  const [head, ...tail] = path;
  return steps.map((step, index) => {
    if (index !== head) return step;
    if (!tail.length) return updater(step);
    return { ...step, subSteps: updateStepAtPath(step.subSteps, tail, updater) };
  });
}

export function removeStepAtPath(steps, path) {
  const [head, ...tail] = path;
  if (!tail.length) return steps.filter((_, index) => index !== head);
  return steps.map((step, index) => index === head
    ? { ...step, subSteps: removeStepAtPath(step.subSteps, tail) }
    : step);
}

export function countDescendants(step) {
  return step.subSteps.reduce((total, child) => total + 1 + countDescendants(child), 0);
}

export function countAttachments(step) {
  return step.attachments.length + step.subSteps.reduce((total, child) => total + countAttachments(child), 0);
}

export function buildScenarioStepNumbers(steps) {
  const numbers = new Map();
  let nextNumber = 1;
  const visit = (items, parentPath = '') => {
    items.filter(hasStepContent).forEach((step, index) => {
      const path = parentPath ? `${parentPath}.${index}` : String(index);
      numbers.set(path, nextNumber);
      nextNumber += 1;
      visit(step.subSteps, path);
    });
  };
  visit(steps);
  return numbers;
}

export function collectScenarioExpansionPaths(steps) {
  const stepPaths = [];
  const attachmentPaths = [];
  const visit = (items, parentPath = '') => {
    items.filter(hasStepContent).forEach((step, index) => {
      const path = parentPath ? `${parentPath}.${index}` : String(index);
      if (step.subSteps.some(hasStepContent) || step.parameters.length || step.attachments.length) stepPaths.push(path);
      step.attachments.forEach((_, attachmentIndex) => attachmentPaths.push(`${path}.a${attachmentIndex}`));
      visit(step.subSteps, path);
    });
  };
  visit(steps);
  return { stepPaths, attachmentPaths };
}

export function formatDuration(durationMs) {
  if (!Number.isFinite(durationMs) || durationMs < 0) return '';
  const minutes = Math.floor(durationMs / 60000);
  const seconds = Math.floor((durationMs % 60000) / 1000);
  const milliseconds = durationMs % 1000;
  return [minutes ? `${minutes}m` : '', seconds ? `${seconds}s` : '', milliseconds && !minutes ? `${milliseconds}ms` : '']
    .filter(Boolean).join(' ') || '0ms';
}

export function formatSize(sizeBytes) {
  if (!Number.isFinite(sizeBytes) || sizeBytes < 0) return '';
  if (sizeBytes < 1024) return `${sizeBytes} B`;
  if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} KB`;
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function buildScenarioExportText(item) {
  const lines = ['TEST CASE', '', `ID: ${item.testId ?? '—'}`, `Category / Feature: ${item.category ?? '—'}`, `Short Title: ${item.shortTitle ?? '—'}`, '', 'DETAILED SCENARIO', ''];
  const render = (steps, level) => steps.forEach((step) => {
    const indent = '   '.repeat(level);
    lines.push(`${indent}${step.text || '—'}`);
    step.parameters.forEach((parameter) => lines.push(`${indent}   ${parameter.name} — ${parameter.value}`));
    step.attachments.forEach((attachment) => {
      lines.push(`${indent}   [${attachment.name || 'Attachment'}]`);
      if (attachment.content) lines.push(...attachment.content.split('\n').map((line) => `${indent}      ${line}`));
    });
    render(step.subSteps, level + 1);
  });
  render(normalizeScenario(item.scenario).steps, 0);
  return `${lines.join('\n').replace(/\n+$/u, '')}\n`;
}
