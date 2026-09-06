import { useEffect, useMemo, useRef, useState } from 'react';
import { EMPTY_FILTER_VALUE } from './tableViewModel.js';

export function TableFilterPanel({ column, filter, options, position, translate, onApply, onClear, onClose }) {
  const panelRef = useRef(null);
  const isValuesFilter = column.filterKind === 'values';
  const [draftValues, setDraftValues] = useState(filter?.values ?? []);
  const [draftMode, setDraftMode] = useState(filter?.mode ?? 'contains');
  const [draftQuery, setDraftQuery] = useState(filter?.query ?? '');
  const [optionSearch, setOptionSearch] = useState('');

  useEffect(() => {
    const handlePointerDown = (event) => {
      if (panelRef.current?.contains(event.target)) return;
      if (event.target.closest(`[data-action='open-column-filter'][data-name='${column.key}']`)) return;
      onClose();
    };
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    const handleScroll = (event) => {
      if (panelRef.current?.contains(event.target)) return;
      onClose();
    };
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('scroll', handleScroll, true);
    window.addEventListener('resize', onClose);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('scroll', handleScroll, true);
      window.removeEventListener('resize', onClose);
    };
  }, [column.key, onClose]);

  const filteredOptions = useMemo(() => {
    const query = optionSearch.trim().toLocaleLowerCase('ru-RU');
    if (!query) return options;
    return options.filter((value) => {
      const label = value === EMPTY_FILTER_VALUE ? translate('No value') : value;
      return label.toLocaleLowerCase('ru-RU').includes(query);
    });
  }, [optionSearch, options, translate]);

  const toggleValue = (value) => {
    setDraftValues((current) => current.includes(value)
      ? current.filter((item) => item !== value)
      : [...current, value]);
  };

  const apply = () => {
    if (isValuesFilter) {
      onApply(draftValues.length > 0 ? { kind: 'values', values: draftValues } : null);
    } else {
      const nextFilter = draftMode === 'contains'
        ? { kind: 'text', mode: draftMode, query: draftQuery.trim() }
        : { kind: 'text', mode: draftMode, query: '' };
      onApply(draftMode === 'contains' && !nextFilter.query ? null : nextFilter);
    }
  };

  return (
    <div
      ref={panelRef}
      id={`table-filter-panel-${column.key}`}
      className="table-filter-popover"
      style={{ top: position.top, left: position.left }}
      role="dialog"
      aria-label={`${translate('Filter')}: ${column.label}`}
      data-testid="table-filter-panel"
      data-role="filter-panel"
      data-name={column.key}
    >
      <div className="table-filter-popover-title">{column.label}</div>
      {isValuesFilter ? (
        <>
          <input
            className="table-filter-search"
            type="search"
            value={optionSearch}
            onChange={(event) => setOptionSearch(event.target.value)}
            placeholder={translate('Search values')}
            aria-label={`${translate('Search values')}: ${column.label}`}
            data-testid="table-filter-search"
            data-role="input"
            data-name={column.key}
          />
          <div className="table-filter-options">
            {filteredOptions.map((value) => {
              const label = value === EMPTY_FILTER_VALUE ? translate('No value') : value;
              return (
                <label
                  className="table-filter-option"
                  key={value}
                  data-testid="table-filter-option"
                  data-role="filter-option"
                  data-name={column.key}
                  data-value={value}
                  data-state={draftValues.includes(value) ? 'selected' : 'unselected'}
                >
                  <input
                    type="checkbox"
                    checked={draftValues.includes(value)}
                    onChange={() => toggleValue(value)}
                  />
                  <span>{label}</span>
                </label>
              );
            })}
            {filteredOptions.length === 0 && <div className="table-filter-empty-options">{translate('No values')}</div>}
          </div>
        </>
      ) : (
        <>
          <label className="table-filter-field">
            <span>{translate('Condition')}</span>
            <select
              value={draftMode}
              onChange={(event) => setDraftMode(event.target.value)}
              data-testid="table-filter-condition"
              data-role="select"
              data-name={column.key}
            >
              <option value="contains">{translate('Contains')}</option>
              <option value="empty">{translate('Empty')}</option>
              <option value="not-empty">{translate('Not empty')}</option>
            </select>
          </label>
          {draftMode === 'contains' && (
            <input
              autoFocus
              className="table-filter-search"
              type="search"
              value={draftQuery}
              onChange={(event) => setDraftQuery(event.target.value)}
              placeholder={translate('Enter text')}
              aria-label={`${translate('Filter value')}: ${column.label}`}
              data-testid="table-filter-search"
              data-role="input"
              data-name={column.key}
            />
          )}
        </>
      )}
      <div className="table-filter-actions">
        <button
          type="button"
          className="ghost-btn"
          onClick={onClear}
          data-role="button"
          data-action="clear-filter"
          data-name={column.key}
        >
          {translate('Reset')}
        </button>
        <button
          type="button"
          className="secondary-btn"
          onClick={apply}
          data-role="button"
          data-action="apply-filter"
          data-name={column.key}
        >
          {translate('Apply')}
        </button>
      </div>
    </div>
  );
}
