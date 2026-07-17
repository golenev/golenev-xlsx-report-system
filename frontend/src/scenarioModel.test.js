import assert from 'node:assert/strict';
import test from 'node:test';
import { countAttachments, countDescendants, normalizeScenario, serializeScenario, updateStepAtPath } from './scenarioModel.js';

function boundaryScenario() {
  let leaf = { text: 'leaf', durationMs: 24, parameters: [{ name: 'expectedDate', value: '2026-07-16' }], attachments: Array.from({ length: 10 }, (_, index) => ({ name: `Attachment ${index + 1}`, content: `content-${index + 1}` })), subSteps: [] };
  for (let level = 4; level >= 1; level -= 1) leaf = { text: `level-${level}`, attachments: [], parameters: [], subSteps: [leaf] };
  return { steps: [leaf] };
}

test('normalization and serialization preserve five levels and ten independent attachments', () => {
  const scenario = normalizeScenario(boundaryScenario());
  assert.equal(countDescendants(scenario.steps[0]), 4);
  assert.equal(countAttachments(scenario.steps[0]), 10);
  const serialized = serializeScenario(scenario);
  const leaf = serialized.steps[0].subSteps[0].subSteps[0].subSteps[0].subSteps[0];
  assert.equal(leaf.durationMs, 24);
  assert.deepEqual(leaf.parameters, [{ name: 'expectedDate', value: '2026-07-16' }]);
  assert.deepEqual(leaf.attachments.map(({ name, content }) => ({ name, content })), Array.from({ length: 10 }, (_, index) => ({ name: `Attachment ${index + 1}`, content: `content-${index + 1}` })));
});

test('editing a parent and one attachment does not mutate nested metadata or siblings', () => {
  const original = normalizeScenario(boundaryScenario());
  const parentChanged = { ...original, steps: updateStepAtPath(original.steps, [0], (step) => ({ ...step, text: 'changed' })) };
  const leafPath = [0, 0, 0, 0, 0];
  const attachmentChanged = { ...parentChanged, steps: updateStepAtPath(parentChanged.steps, leafPath, (step) => ({ ...step, attachments: step.attachments.map((attachment, index) => index === 4 ? { ...attachment, content: 'changed-5' } : attachment) })) };
  const leaf = attachmentChanged.steps[0].subSteps[0].subSteps[0].subSteps[0].subSteps[0];
  assert.equal(parentChanged.steps[0].subSteps[0].subSteps[0].subSteps[0].subSteps[0].attachments[4].content, 'content-5');
  assert.equal(leaf.attachments[4].content, 'changed-5');
  assert.equal(leaf.attachments[5].content, 'content-6');
  assert.equal(leaf.durationMs, 24);
  assert.equal(leaf.parameters[0].value, '2026-07-16');
});
