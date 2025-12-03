import { useEffect, useMemo, useState } from 'react';
import {
  ArcElement,
  Chart as ChartJS,
  Legend,
  Tooltip
} from 'chart.js';
import { Doughnut } from 'react-chartjs-2';

ChartJS.register(ArcElement, Tooltip, Legend);

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

function withBase(path: string) {
  if (!API_BASE) {
    return path;
  }
  return `${API_BASE}${path}`;
}

type RegressionStatus = 'PASSED' | 'FAILED' | 'SKIPPED' | string;

type GeneralStatus =
  | 'Очередь'
  | 'В работе'
  | 'Готово'
  | 'Бэклог'
  | 'Только ручное'
  | 'Неактуально'
  | 'Фронт'
  | string;

export interface RegressionTest {
  notes?: string;
  testId: string;
  category?: string;
  scenario?: string;
  issueLink?: string;
  readyDate?: string;
  shortTitle?: string;
  generalStatus?: GeneralStatus;
  regressionStatus?: RegressionStatus;
}

interface RegressionSnapshot {
  tests: RegressionTest[];
  status?: string;
  regressionDate?: string;
  name?: string;
}

interface RegressionReleaseSummary {
  id: string | number;
  name: string;
  regressionDate?: string;
  status?: string;
}

interface RegressionResponse extends RegressionReleaseSummary {
  snapshot: RegressionSnapshot;
}

type LoadingState = 'idle' | 'loading' | 'error' | 'success';

const iconChevronDown = '▼';
const iconChevronRight = '▶';

function formatDate(date?: string) {
  if (!date) return '—';
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) {
    return date;
  }
  return parsed.toLocaleDateString('ru-RU');
}

function getHealthColor(passed: number, failed: number, skipped: number) {
  if (failed > 0) return '#f44336';
  if (skipped > 0) return '#fbc02d';
  if (passed > 0) return '#4caf50';
  return '#9e9e9e';
}

export default function ReleaseAnalyticsWidget() {
  const [expanded, setExpanded] = useState(false);
  const [releases, setReleases] = useState<RegressionReleaseSummary[]>([]);
  const [releasesState, setReleasesState] = useState<LoadingState>('idle');
  const [snapshotState, setSnapshotState] = useState<LoadingState>('idle');
  const [snapshotError, setSnapshotError] = useState<string | null>(null);
  const [selectedReleaseId, setSelectedReleaseId] = useState<string | number | null>(null);
  const [regression, setRegression] = useState<RegressionResponse | null>(null);

  const selectedSnapshot = regression?.snapshot;

  const metrics = useMemo(() => {
    const tests = selectedSnapshot?.tests ?? [];
    const totalTests = tests.length;
    const passedCount = tests.filter((t) => t.regressionStatus === 'PASSED').length;
    const failedCount = tests.filter((t) => t.regressionStatus === 'FAILED').length;
    const skippedCount = tests.filter((t) => t.regressionStatus === 'SKIPPED').length;

    const queuedCount = tests.filter((t) => t.generalStatus === 'Очередь').length;
    const inProgressCount = tests.filter((t) => t.generalStatus === 'В работе').length;
    const readyCount = tests.filter((t) => t.generalStatus === 'Готово').length;

    const automationPercent = totalTests === 0 ? 0 : Math.round((readyCount / totalTests) * 100);

    return {
      tests,
      totalTests,
      passedCount,
      failedCount,
      skippedCount,
      queuedCount,
      inProgressCount,
      readyCount,
      automationPercent
    };
  }, [selectedSnapshot?.tests]);

  const categories = useMemo(() => {
    const groups: Record<
      string,
      { name: string; total: number; passed: number; failed: number; skipped: number }
    > = {};

    metrics.tests.forEach((test) => {
      const name = test.category || 'Без категории';
      if (!groups[name]) {
        groups[name] = { name, total: 0, passed: 0, failed: 0, skipped: 0 };
      }
      groups[name].total += 1;
      if (test.regressionStatus === 'PASSED') groups[name].passed += 1;
      if (test.regressionStatus === 'FAILED') groups[name].failed += 1;
      if (test.regressionStatus === 'SKIPPED') groups[name].skipped += 1;
    });

    return Object.values(groups).sort((a, b) => b.total - a.total);
  }, [metrics.tests]);

  const failures = useMemo(() => {
    return metrics.tests.filter(
      (test) => test.regressionStatus === 'FAILED' || test.regressionStatus === 'SKIPPED'
    );
  }, [metrics.tests]);

  const collapsedSummary = useMemo(() => {
    const releaseName = regression?.name || '—';
    const automation = metrics.automationPercent;
    const failed = metrics.failedCount;
    return { releaseName, automation, failed };
  }, [metrics.automationPercent, metrics.failedCount, regression?.name]);

  const loadReleases = async () => {
    setReleasesState('loading');
    try {
      const response = await fetch(withBase('/api/regressions/releases'));
      if (!response.ok) {
        throw new Error('Не удалось загрузить список релизов');
      }
      const data: RegressionReleaseSummary[] = await response.json();
      setReleases(data || []);
      setSelectedReleaseId((prev) => prev ?? data?.[0]?.id ?? null);
      setReleasesState('success');
    } catch (error) {
      console.error(error);
      setReleasesState('error');
    }
  };

  const loadSnapshot = async (releaseId: string | number) => {
    if (!releaseId) return;
    setSnapshotState('loading');
    setSnapshotError(null);
    try {
      const response = await fetch(withBase(`/api/regressions/${releaseId}`));
      if (!response.ok) {
        throw new Error('Не удалось загрузить снапшот регресса');
      }
      const data: RegressionResponse = await response.json();
      setRegression(data);
      setSnapshotState('success');
    } catch (error) {
      console.error(error);
      setSnapshotError(error instanceof Error ? error.message : 'Ошибка загрузки снапшота');
      setSnapshotState('error');
    }
  };

  useEffect(() => {
    loadReleases();
  }, []);

  useEffect(() => {
    if (selectedReleaseId != null) {
      loadSnapshot(selectedReleaseId);
    }
  }, [selectedReleaseId]);

  const donutData = useMemo(() => {
    const ready = metrics.readyCount;
    const remaining = Math.max(metrics.totalTests - ready, 0);
    return {
      labels: ['Готово', 'Остальные'],
      datasets: [
        {
          data: [ready, remaining],
          backgroundColor: ['#5b8def', '#e6e9f5'],
          borderWidth: 0,
          cutout: '70%'
        }
      ]
    };
  }, [metrics.readyCount, metrics.totalTests]);

  const donutOptions = useMemo(() => ({
    plugins: { legend: { display: false }, tooltip: { enabled: false } },
    responsive: true,
    maintainAspectRatio: false
  }), []);

  const renderSkeleton = (lines = 3) => (
    <div className="analytics-skeleton">
      {Array.from({ length: lines }).map((_, idx) => (
        <div key={idx} className="analytics-skeleton-line" />
      ))}
    </div>
  );

  const renderContent = () => {
    if (snapshotState === 'loading') {
      return (
        <div className="analytics-loading-block">
          {renderSkeleton(6)}
          {renderSkeleton(4)}
        </div>
      );
    }

    if (snapshotState === 'error') {
      return (
        <div className="analytics-error">
          <div>Не удалось загрузить данные релиза.</div>
          <button type="button" className="primary-btn" onClick={() => selectedReleaseId && loadSnapshot(selectedReleaseId)}>
            Повторить
          </button>
          {snapshotError && <div className="analytics-error-details">{snapshotError}</div>}
        </div>
      );
    }

    return (
      <div className="analytics-grid">
        <div className="analytics-card">
          <div className="donut-wrapper">
            <div className="donut-chart">
              <Doughnut data={donutData} options={donutOptions} />
              <div className="donut-center">
                <div className="donut-value">{metrics.automationPercent}%</div>
                <div className="donut-label">Автоматизация</div>
              </div>
            </div>
            <div className="donut-legend">
              <div className="legend-item">
                <span className="legend-dot ready" /> Готово — {metrics.readyCount}
              </div>
              <div className="legend-item">
                <span className="legend-dot other" /> Остальные — {metrics.totalTests - metrics.readyCount}
              </div>
            </div>
          </div>
        </div>
        <div className="analytics-card">
          <div className="analytics-subtitle">Categories</div>
          <div className="categories-list">
            {categories.map((category) => {
              const color = getHealthColor(category.passed, category.failed, category.skipped);
              return (
                <div key={category.name} className="category-row">
                  <span className="category-indicator" style={{ backgroundColor: color }} />
                  <div className="category-info">
                    <div className="category-name">{category.name}</div>
                    <div className="category-meta">
                      {category.total} тестов ({category.passed}/{category.failed}/{category.skipped})
                    </div>
                  </div>
                </div>
              );
            })}
            {categories.length === 0 && <div className="empty-placeholder">Нет данных по категориям</div>}
          </div>
          <div className="analytics-subtitle">Failures & risks</div>
          <div className="failures-list">
            {failures.map((test) => (
              <div key={test.testId} className="failure-row">
                <div className="failure-id">#{test.testId}</div>
                <div className="failure-body">
                  <div className="failure-title">{test.shortTitle || 'Без названия'}</div>
                  <div className="failure-meta">
                    {test.category || 'Без категории'}
                    {test.issueLink && (
                      <a className="issue-link" href={test.issueLink} target="_blank" rel="noreferrer">
                        Issue
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ))}
            {failures.length === 0 && <div className="empty-placeholder">Серьёзных рисков не обнаружено</div>}
          </div>
        </div>
      </div>
    );
  };

  const renderKpi = () => {
    const chips = [
      { label: 'Total', value: metrics.totalTests },
      { label: 'Passed', value: metrics.passedCount },
      { label: 'Failed', value: metrics.failedCount },
      { label: 'Skipped', value: metrics.skippedCount },
      { label: 'Очередь', value: metrics.queuedCount },
      { label: 'В работе', value: metrics.inProgressCount },
      { label: 'Готово', value: metrics.readyCount }
    ];

    return (
      <div className="kpi-row">
        {chips.map((chip) => (
          <div key={chip.label} className="kpi-chip">
            <div className="kpi-label">{chip.label}</div>
            <div className="kpi-value">{chip.value}</div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="release-analytics-widget">
      {expanded ? (
        <div className="release-analytics-card">
          <div className="analytics-header">
            <div>
              <div className="analytics-title">Release Analytics</div>
              <div className="analytics-subtitle">
                Release {regression?.name || '—'} • {regression?.status || '—'} • {formatDate(regression?.regressionDate)}
              </div>
            </div>
            <div className="analytics-header-center">
              <select
                className="release-select"
                value={selectedReleaseId ?? ''}
                onChange={(e) => setSelectedReleaseId(e.target.value)}
                disabled={releasesState === 'loading'}
              >
                {releases.map((release) => (
                  <option key={release.id} value={release.id}>
                    {release.name}
                  </option>
                ))}
                {releases.length === 0 && <option value="">Нет доступных релизов</option>}
              </select>
            </div>
            <div className="analytics-actions">
              <button type="button" className="icon-button" onClick={() => setExpanded(false)} title="Свернуть">
                {iconChevronDown}
              </button>
              <button
                type="button"
                className="primary-btn download-btn"
                onClick={() =>
                  selectedReleaseId && window.open(withBase(`/api/regressions/${selectedReleaseId}/snapshot.xlsx`), '_blank')
                }
                disabled={!selectedReleaseId}
              >
                📥 Download report snapshot
              </button>
            </div>
          </div>

          {releasesState === 'error' && (
            <div className="analytics-error">
              <div>Не удалось загрузить список релизов.</div>
              <button type="button" className="primary-btn" onClick={loadReleases}>
                Повторить
              </button>
            </div>
          )}

          {renderKpi()}

          <div className="analytics-content">{renderContent()}</div>
        </div>
      ) : (
        <button type="button" className="release-analytics-collapsed" onClick={() => setExpanded(true)}>
          <div className="collapsed-left">
            <span className="collapsed-icon">📊</span>
            <span className="collapsed-title">Release Analytics</span>
          </div>
          <div className="collapsed-summary">
            Release {collapsedSummary.releaseName} • {collapsedSummary.automation}% automation • {collapsedSummary.failed}{' '}
            failed
          </div>
          <span className="collapsed-toggle" aria-hidden>
            {iconChevronRight}
          </span>
        </button>
      )}
    </div>
  );
}
