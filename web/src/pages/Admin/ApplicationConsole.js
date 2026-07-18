import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getConsoleStatus,
  startConsoleService,
  stopConsoleService,
  getConsoleErrors,
} from '../../api/console';
import { Badge } from '../../components/shared/Badge';

const STATUS_POLL_INTERVAL_MS = 10000;

// Only these four domains own an error_log table (Phase 4) — gateway has no DB
// and "web"/frontend isn't a backend domain, so they never appear as keys in
// ConsoleErrorsResponse.
const ERROR_DOMAINS = new Set(['profile', 'wealth', 'health', 'household']);

function isDisabledError(error) {
  return error?.status === 404;
}

function statusBadgeVariant(status) {
  if (status === 'UP') return 'success';
  if (status === 'DOWN') return 'danger';
  return 'neutral';
}

function formatTimestamp(isoStr) {
  if (!isoStr) return '';
  return new Date(isoStr).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ConsoleErrorAggregationService (web-gateway) falls back to a bare
// { error: "Could not reach <domain> service: ..." } entry — not the normal
// ErrorLogResponse shape (error_code/http_status/message) — when a domain is
// unreachable. Render both shapes instead of assuming the documented one.
function ErrorEntryRow({ entry }) {
  const message = entry.message || entry.error || null;
  const code = entry.error_code || (entry.error ? 'SERVICE_UNREACHABLE' : 'UNKNOWN_ERROR');

  return (
    <li className="py-2 border-b border-gray-100 last:border-0">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-gray-700">{code}</span>
        {entry.http_status !== undefined && (
          <span className="text-xs text-gray-400">{entry.http_status}</span>
        )}
      </div>
      {message && <p className="text-xs text-gray-600 mt-0.5">{message}</p>}
      {entry.created_at && (
        <p className="text-xs text-gray-400 mt-0.5">{formatTimestamp(entry.created_at)}</p>
      )}
    </li>
  );
}
ErrorEntryRow.propTypes = {
  entry: PropTypes.shape({
    error_code: PropTypes.string,
    http_status: PropTypes.number,
    message: PropTypes.string,
    error: PropTypes.string,
    created_at: PropTypes.string,
  }).isRequired,
};

function ServiceRow({
  service,
  pendingAction,
  anyActionPending,
  onStart,
  onStop,
  errors,
  errorsAvailable,
}) {
  // Disabled whenever ANY service has an action in flight, not just this row --
  // handleStart/handleStop no-op globally while one is pending, so leaving other
  // rows' buttons clickable would silently do nothing on click.
  const isPending = anyActionPending;
  const [expanded, setExpanded] = useState(false);
  const hasErrorPanel = ERROR_DOMAINS.has(service.name);
  const errorCount = errors.length;

  let errorLabel = 'Errors';
  if (errorsAvailable) {
    const suffix = errorCount === 1 ? '' : 's';
    errorLabel = `${errorCount} recent error${suffix}`;
  }

  return (
    <div
      className="bg-white rounded-xl border border-gray-100 shadow-sm p-4"
      data-testid={`service-row-${service.name}`}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-semibold text-gray-900">{service.name}</p>
          <p className="text-xs text-gray-400">
            {service.kind} · port {service.port}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant={statusBadgeVariant(service.status)}>{service.status || 'UNKNOWN'}</Badge>
          <button
            type="button"
            onClick={() => onStart(service.name)}
            disabled={isPending || service.status === 'UP'}
            className="bg-green-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-green-700 disabled:opacity-50"
          >
            {pendingAction === 'start' ? 'Starting...' : 'Start'}
          </button>
          <button
            type="button"
            onClick={() => onStop(service.name)}
            disabled={isPending || service.status === 'DOWN'}
            className="bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700 disabled:opacity-50"
          >
            {pendingAction === 'stop' ? 'Stopping...' : 'Stop'}
          </button>
        </div>
      </div>

      {hasErrorPanel && (
        <div className="mt-3 pt-3 border-t border-gray-100">
          <button
            type="button"
            onClick={() => setExpanded((prev) => !prev)}
            aria-label={
              expanded ? `Collapse ${service.name} errors` : `Expand ${service.name} errors`
            }
            className="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1"
          >
            <span>{expanded ? '▾' : '▸'}</span>
            <span>{errorLabel}</span>
          </button>
          {expanded && (
            <ul className="mt-2">
              {errorCount === 0 && (
                <li className="text-xs text-gray-400 py-1">No recent errors.</li>
              )}
              {errors.map((entry, idx) => (
                // eslint-disable-next-line react/no-array-index-key
                <ErrorEntryRow
                  key={`${entry.error_code}-${entry.created_at}-${idx}`}
                  entry={entry}
                />
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
ServiceRow.propTypes = {
  service: PropTypes.shape({
    name: PropTypes.string,
    port: PropTypes.number,
    kind: PropTypes.string,
    status: PropTypes.string,
  }).isRequired,
  pendingAction: PropTypes.oneOf(['start', 'stop', null]),
  anyActionPending: PropTypes.bool,
  onStart: PropTypes.func.isRequired,
  onStop: PropTypes.func.isRequired,
  errors: PropTypes.array,
  errorsAvailable: PropTypes.bool,
};
ServiceRow.defaultProps = {
  pendingAction: null,
  anyActionPending: false,
  errors: [],
  errorsAvailable: false,
};

export const ApplicationConsole = () => {
  const queryClient = useQueryClient();
  const [pending, setPending] = useState(null); // { name, action } | null

  const statusQuery = useQuery({
    queryKey: ['console-status'],
    queryFn: getConsoleStatus,
    retry: false,
    refetchInterval: (query) =>
      isDisabledError(query.state.error) ? false : STATUS_POLL_INTERVAL_MS,
  });

  const consoleDisabled = statusQuery.isError && isDisabledError(statusQuery.error);

  const errorsQuery = useQuery({
    queryKey: ['console-errors'],
    queryFn: getConsoleErrors,
    retry: false,
    enabled: statusQuery.isSuccess,
    refetchInterval: STATUS_POLL_INTERVAL_MS,
  });

  const invalidateStatus = () => queryClient.invalidateQueries({ queryKey: ['console-status'] });

  const startMutation = useMutation({
    mutationFn: startConsoleService,
    onSettled: () => {
      setPending(null);
      invalidateStatus();
    },
  });

  const stopMutation = useMutation({
    mutationFn: stopConsoleService,
    onSettled: () => {
      setPending(null);
      invalidateStatus();
    },
  });

  const handleStart = (name) => {
    if (pending) return;
    setPending({ name, action: 'start' });
    startMutation.mutate(name);
  };

  const handleStop = (name) => {
    if (pending) return;
    setPending({ name, action: 'stop' });
    stopMutation.mutate(name);
  };

  const services = statusQuery.data ?? [];
  const errorsData = errorsQuery.data ?? null;

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-1">Application Console</h1>
      <p className="text-sm text-gray-500 mb-6">
        Live status, start/stop controls, and recent errors for every Suchika service.
      </p>

      {consoleDisabled && (
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
          <p className="text-sm text-gray-600">
            The Application Console is currently disabled. Set{' '}
            <code className="bg-gray-100 px-1 rounded">suchika.console.enabled=true</code> in the
            gateway configuration to turn it on.
          </p>
        </div>
      )}

      {!consoleDisabled && statusQuery.isLoading && (
        <p className="text-sm text-gray-500">Loading service status...</p>
      )}

      {!consoleDisabled && statusQuery.isError && (
        <p className="text-sm text-red-700 bg-red-50 rounded-lg px-3 py-2">
          Failed to load service status.
        </p>
      )}

      {!consoleDisabled && statusQuery.isSuccess && (
        <div className="space-y-3">
          {services.length === 0 && <p className="text-sm text-gray-500">No services reported.</p>}
          {services.map((service) => (
            <ServiceRow
              key={service.name}
              service={service}
              pendingAction={pending && pending.name === service.name ? pending.action : null}
              anyActionPending={pending !== null}
              onStart={handleStart}
              onStop={handleStop}
              errors={errorsData?.[service.name] ?? []}
              errorsAvailable={Boolean(errorsData)}
            />
          ))}
        </div>
      )}
    </div>
  );
};
