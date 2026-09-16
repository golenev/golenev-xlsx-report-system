import assert from 'node:assert/strict';
import test from 'node:test';
import {
  EMPTY_FILTER_VALUE,
  FILTER_KINDS,
  applyTableFilters,
  collectFilterOptions,
  groupTableItems,
  parseTableViewState,
  serializeTableViewState
} from './tableViewModel.js';

const items = [
  {
    testId: 'UI-FILTER-1',
    category: 'Авторизация',
    shortTitle: 'Успешный вход',
    issueLink: '',
    readyDate: '2026-09-01',
    generalStatus: 'Готово',
    priority: 'Critical',
    scenario: { steps: [{ text: 'Открываем форму', attachments: [], subSteps: [] }] },
    notes: 'smoke',
    regressionStatus: 'PASSED'
  },
  {
    testId: 'UI-FILTER-2',
    category: 'Авторизация',
    shortTitle: 'Ошибка входа',
    issueLink: 'https://tracker/2',
    readyDate: '2026-09-02',
    generalStatus: 'Бэклог',
    priority: 'Blocker',
    scenario: { steps: [{ text: 'Вводим неверный пароль', attachments: [], subSteps: [] }] },
    notes: '',
    regressionStatus: 'FAILED'
  },
  {
    testId: 'UI-FILTER-3',
    category: 'Профиль',
    shortTitle: 'Редактирование профиля',
    issueLink: 'https://tracker/3',
    readyDate: '2026-09-02',
    generalStatus: 'Готово',
    priority: 'Medium',
    scenario: { steps: [{ text: 'Меняем имя', attachments: [{ name: 'payload', content: 'Иван' }] }] },
    notes: 'regression',
    regressionStatus: ''
  }
];

test('every table column has an explicit filter kind', () => {
  assert.deepEqual(Object.keys(FILTER_KINDS), [
    'testId',
    'category',
    'shortTitle',
    'issueLink',
    'readyDate',
    'generalStatus',
    'priority',
    'scenario',
    'notes',
    'regressionStatus'
  ]);
});

test('text filters are case-insensitive and find nested scenario and attachment content', () => {
  assert.deepEqual(
    applyTableFilters(items, { testId: { kind: 'text', mode: 'contains', query: 'filter-2' } })
      .map((item) => item.testId),
    ['UI-FILTER-2']
  );
  assert.deepEqual(
    applyTableFilters(items, { scenario: { kind: 'text', mode: 'contains', query: 'иван' } })
      .map((item) => item.testId),
    ['UI-FILTER-3']
  );
});

test('selected values use OR inside one column and filters use AND across columns', () => {
  const filtered = applyTableFilters(items, {
    priority: { kind: 'values', values: ['Critical', 'Blocker'] },
    generalStatus: { kind: 'values', values: ['Готово'] }
  });
  assert.deepEqual(filtered.map((item) => item.testId), ['UI-FILTER-1']);
});

test('empty and non-empty modes distinguish blank values', () => {
  assert.deepEqual(
    applyTableFilters(items, { notes: { kind: 'text', mode: 'empty', query: '' } })
      .map((item) => item.testId),
    ['UI-FILTER-2']
  );
  assert.deepEqual(
    applyTableFilters(items, { notes: { kind: 'text', mode: 'not-empty', query: '' } })
      .map((item) => item.testId),
    ['UI-FILTER-1', 'UI-FILTER-3']
  );
});

test('value options are unique, sorted and expose an empty value', () => {
  assert.deepEqual(collectFilterOptions(items, 'regressionStatus'), [EMPTY_FILTER_VALUE, 'FAILED', 'PASSED']);
});

test('grouping is applied to the already filtered collection without empty groups', () => {
  const filtered = applyTableFilters(items, {
    generalStatus: { kind: 'values', values: ['Готово'] }
  });
  const groups = groupTableItems(filtered, 'category');
  assert.deepEqual(groups.map((group) => group.label), ['Авторизация', 'Профиль']);
  assert.deepEqual(groups.flatMap((group) => group.items.map((item) => item.testId)), [
    'UI-FILTER-1',
    'UI-FILTER-3'
  ]);
});

test('URL round-trip preserves repeated values, Cyrillic text and grouping', () => {
  const filters = {
    priority: { kind: 'values', values: ['Critical', 'Blocker'] },
    generalStatus: { kind: 'values', values: ['Готово'] },
    shortTitle: { kind: 'text', mode: 'contains', query: 'форма входа' }
  };
  const search = serializeTableViewState(filters, 'category', '?unrelated=kept');
  assert.deepEqual(parseTableViewState(search), { filters, groupBy: 'category' });
  assert.equal(new URLSearchParams(search).get('unrelated'), 'kept');
});

test('invalid URL parameters are ignored', () => {
  assert.deepEqual(
    parseTableViewState('?filter.unknown=value&groupBy=unknown&filter.testId.mode=wrong'),
    { filters: {}, groupBy: '' }
  );
});

test('filtering and grouping do not mutate source items', () => {
  const source = structuredClone(items);
  applyTableFilters(items, { priority: { kind: 'values', values: ['Critical'] } });
  groupTableItems(items, 'priority');
  assert.deepEqual(items, source);
});
