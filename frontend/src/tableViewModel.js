export const EMPTY_FILTER_VALUE = '__EMPTY__';

export const FILTER_KINDS = {
  testId: 'text',
  category: 'values',
  shortTitle: 'text',
  issueLink: 'text',
  readyDate: 'values',
  generalStatus: 'values',
  priority: 'values',
  scenario: 'text',
  notes: 'text',
  regressionStatus: 'values'
};

export const GROUPABLE_COLUMNS = new Set([
  'category',
  'readyDate',
  'generalStatus',
  'priority',
  'regressionStatus'
]);

const FILTER_KEYS = new Set(Object.keys(FILTER_KINDS));
function normalizedText(value) {
  return String(value ?? '').trim();
}

function scenarioText(value) {
  if (!value) return '';
  if (typeof value === 'string') return value;

  const parts = [];
  const collectSteps = (steps) => {
    for (const step of steps ?? []) {
      parts.push(step.text ?? '');
      for (const parameter of step.parameters ?? []) {
        parts.push(parameter.name ?? '', parameter.value ?? '');
      }
      for (const attachment of step.attachments ?? []) {
        parts.push(attachment.name ?? '', attachment.content ?? '');
      }
      collectSteps(step.subSteps);
    }
  };
  collectSteps(value.steps);
  return parts.join(' ');
}

export function getFilterValue(item, key) {
  if (key === 'scenario') return scenarioText(item.scenario);
  return normalizedText(item[key]);
}

export function isFilterActive(filter) {
  if (!filter) return false;
  if (filter.kind === 'values') return (filter.values ?? []).length > 0;
  if (filter.mode === 'empty' || filter.mode === 'not-empty') return true;
  return normalizedText(filter.query).length > 0;
}

export function applyTableFilters(items, filters) {
  const activeFilters = Object.entries(filters ?? {}).filter(([, filter]) => isFilterActive(filter));
  if (activeFilters.length === 0) return [...items];

  return items.filter((item) => activeFilters.every(([key, filter]) => {
    const value = getFilterValue(item, key);
    if (filter.kind === 'values') {
      return filter.values.some((selectedValue) =>
        selectedValue === EMPTY_FILTER_VALUE ? value.length === 0 : value === selectedValue
      );
    }
    if (filter.mode === 'empty') return value.length === 0;
    if (filter.mode === 'not-empty') return value.length > 0;
    return value.toLocaleLowerCase('ru-RU').includes(
      normalizedText(filter.query).toLocaleLowerCase('ru-RU')
    );
  }));
}

export function collectFilterOptions(items, key) {
  const values = new Set();
  let hasEmpty = false;
  for (const item of items) {
    const value = getFilterValue(item, key);
    if (value) values.add(value);
    else hasEmpty = true;
  }
  const result = [...values].sort((left, right) => left.localeCompare(right, 'ru'));
  return hasEmpty ? [EMPTY_FILTER_VALUE, ...result] : result;
}

export function groupTableItems(items, groupBy) {
  if (!GROUPABLE_COLUMNS.has(groupBy)) return [];
  const groups = new Map();
  for (const item of items) {
    const rawValue = getFilterValue(item, groupBy);
    const value = rawValue || EMPTY_FILTER_VALUE;
    if (!groups.has(value)) groups.set(value, []);
    groups.get(value).push(item);
  }
  return [...groups.entries()]
    .sort(([left], [right]) => {
      if (left === EMPTY_FILTER_VALUE) return 1;
      if (right === EMPTY_FILTER_VALUE) return -1;
      return left.localeCompare(right, 'ru');
    })
    .map(([value, groupedItems]) => ({
      value,
      label: value === EMPTY_FILTER_VALUE ? '' : value,
      items: groupedItems
    }));
}

export function describeFilter(filter, translate = (text) => text) {
  if (filter.kind === 'values') {
    return filter.values
      .map((value) => value === EMPTY_FILTER_VALUE ? translate('No value') : value)
      .join(', ');
  }
  if (filter.mode === 'empty') return translate('No value');
  if (filter.mode === 'not-empty') return translate('Has value');
  return `${translate('Contains')}: ${normalizedText(filter.query)}`;
}

export function parseTableViewState(search) {
  const params = new URLSearchParams(search);
  const filters = {};

  for (const key of FILTER_KEYS) {
    const kind = FILTER_KINDS[key];
    if (kind === 'values') {
      const values = params.getAll(`filter.${key}`).filter((value) => value.length > 0);
      if (values.length > 0) filters[key] = { kind, values: [...new Set(values)] };
      continue;
    }

    const mode = params.get(`filter.${key}.mode`);
    const query = params.get(`filter.${key}`) ?? '';
    if (mode === 'empty' || mode === 'not-empty') {
      filters[key] = { kind, mode, query: '' };
    } else if (query.trim()) {
      filters[key] = { kind, mode: 'contains', query };
    }
  }

  const requestedGroup = params.get('groupBy');
  return {
    filters,
    groupBy: GROUPABLE_COLUMNS.has(requestedGroup) ? requestedGroup : ''
  };
}

export function serializeTableViewState(filters, groupBy, currentSearch = '') {
  const params = new URLSearchParams(currentSearch);
  for (const key of [...params.keys()]) {
    if (key === 'groupBy' || key.startsWith('filter.')) params.delete(key);
  }

  for (const [key, filter] of Object.entries(filters ?? {})) {
    if (!FILTER_KEYS.has(key) || !isFilterActive(filter)) continue;
    if (filter.kind === 'values') {
      filter.values.forEach((value) => params.append(`filter.${key}`, value));
    } else if (filter.mode === 'empty' || filter.mode === 'not-empty') {
      params.set(`filter.${key}.mode`, filter.mode);
    } else {
      params.set(`filter.${key}`, normalizedText(filter.query));
    }
  }
  if (GROUPABLE_COLUMNS.has(groupBy)) params.set('groupBy', groupBy);

  const serialized = params.toString();
  return serialized ? `?${serialized}` : '';
}
