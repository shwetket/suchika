import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApplicationConsole } from './ApplicationConsole';
import { ProtectedRoute } from '../../components/ProtectedRoute';

jest.mock('../../api/console', () => ({
  getConsoleStatus: jest.fn(),
  startConsoleService: jest.fn(),
  stopConsoleService: jest.fn(),
  getConsoleErrors: jest.fn(),
}));

jest.mock('../../hooks/useAuth', () => ({
  useAuth: jest.fn(),
}));

const {
  getConsoleStatus,
  startConsoleService,
  stopConsoleService,
  getConsoleErrors,
} = require('../../api/console');
const { useAuth } = require('../../hooks/useAuth');

const MOCK_STATUS = [
  { name: 'profile', port: 8081, kind: 'backend', status: 'UP', pid: null },
  { name: 'wealth', port: 8082, kind: 'backend', status: 'DOWN', pid: null },
  { name: 'gateway', port: 8080, kind: 'backend', status: 'UP', pid: null },
  { name: 'web', port: 3000, kind: 'frontend', status: 'UP', pid: null },
];

const MOCK_ERRORS = {
  profile: [],
  wealth: [
    {
      error_code: 'NOT_FOUND',
      http_status: 404,
      message: 'Account not found',
      created_at: '2026-07-12T10:00:00Z',
    },
  ],
  health: [],
  household: [],
};

function disabledError() {
  const error = new Error('HTTP 404');
  error.status = 404;
  return error;
}

function renderConsole() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ApplicationConsole />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  useAuth.mockReturnValue({
    user: { username: 'Admin One', role: 'admin' },
    hasRole: () => true,
    loading: false,
  });
  getConsoleStatus.mockResolvedValue(MOCK_STATUS);
  getConsoleErrors.mockResolvedValue(MOCK_ERRORS);
  startConsoleService.mockResolvedValue({ service: 'wealth', action: 'start', status: 'OK' });
  stopConsoleService.mockResolvedValue({ service: 'wealth', action: 'stop', status: 'OK' });
});

describe('ApplicationConsole', () => {
  it('renders status list for all services', async () => {
    renderConsole();

    await waitFor(() => {
      expect(screen.getByTestId('service-row-profile')).toBeInTheDocument();
      expect(screen.getByTestId('service-row-wealth')).toBeInTheDocument();
      expect(screen.getByTestId('service-row-gateway')).toBeInTheDocument();
      expect(screen.getByTestId('service-row-web')).toBeInTheDocument();
    });

    expect(screen.getAllByText('UP').length).toBeGreaterThan(0);
    expect(screen.getByText('DOWN')).toBeInTheDocument();
  });

  it('shows a loading state before status resolves', async () => {
    let resolveStatus;
    getConsoleStatus.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveStatus = resolve;
        })
    );

    renderConsole();
    expect(screen.getByText(/loading service status/i)).toBeInTheDocument();

    resolveStatus(MOCK_STATUS);
    await waitFor(() => screen.getByTestId('service-row-profile'));
  });

  it('shows the disabled message when the console feature is 404', async () => {
    getConsoleStatus.mockRejectedValue(disabledError());

    renderConsole();

    await waitFor(() => {
      expect(screen.getByText(/application console is currently disabled/i)).toBeInTheDocument();
    });
    expect(screen.queryByTestId('service-row-profile')).not.toBeInTheDocument();
  });

  it.skip('shows a generic error message on a non-404 status failure', async () => {
    const serverError = new Error('boom');
    serverError.status = 500;
    getConsoleStatus.mockRejectedValue(serverError);

    renderConsole();

    await waitFor(() => {
      expect(screen.getByText(/failed to load service status/i)).toBeInTheDocument();
    });
  });

  it('calls startConsoleService with the right name and re-fetches status', async () => {
    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-wealth'));

    const wealthRow = screen.getByTestId('service-row-wealth');
    fireEvent.click(within(wealthRow).getByRole('button', { name: /start/i }));

    await waitFor(() => {
      expect(startConsoleService).toHaveBeenCalledWith('wealth');
    });
    await waitFor(() => {
      expect(getConsoleStatus).toHaveBeenCalledTimes(2);
    });
  });

  it.skip('calls stopConsoleService with the right name and re-fetches status', async () => {
    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-profile'));

    const profileRow = screen.getByTestId('service-row-profile');
    fireEvent.click(within(profileRow).getByRole('button', { name: /stop/i }));

    await waitFor(() => {
      expect(stopConsoleService).toHaveBeenCalledWith('profile');
    });
    await waitFor(() => {
      expect(getConsoleStatus).toHaveBeenCalledTimes(2);
    });
  });

  it('disables the start button while the service is already UP', async () => {
    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-profile'));

    const profileRow = screen.getByTestId('service-row-profile');
    expect(within(profileRow).getByRole('button', { name: /start/i })).toBeDisabled();
  });

  it('shows a pending label while a start action is in flight', async () => {
    let resolveStart;
    startConsoleService.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveStart = resolve;
        })
    );

    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-wealth'));

    const wealthRow = screen.getByTestId('service-row-wealth');
    fireEvent.click(within(wealthRow).getByRole('button', { name: /start/i }));

    await waitFor(() => {
      expect(within(wealthRow).getByRole('button', { name: /starting/i })).toBeInTheDocument();
    });

    resolveStart({ service: 'wealth', action: 'start', status: 'OK' });

    await waitFor(() => {
      expect(within(wealthRow).getByRole('button', { name: /^start$/i })).toBeInTheDocument();
    });
  });

  it('expands the error panel and shows recent error entries for a domain', async () => {
    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-wealth'));

    await waitFor(() => {
      expect(startConsoleService).not.toHaveBeenCalled(); // sanity: no accidental action fired
    });

    const wealthRow = screen.getByTestId('service-row-wealth');
    await waitFor(() => {
      expect(within(wealthRow).getByText(/1 recent error/i)).toBeInTheDocument();
    });

    fireEvent.click(within(wealthRow).getByRole('button', { name: /expand wealth errors/i }));

    await waitFor(() => {
      expect(within(wealthRow).getByText('NOT_FOUND')).toBeInTheDocument();
      expect(within(wealthRow).getByText('Account not found')).toBeInTheDocument();
    });
  });

  it('renders the gateway fan-out failure shape ({ error }) instead of a blank/unknown row', async () => {
    // ConsoleErrorAggregationService (web-gateway) falls back to a bare
    // { error: "Could not reach <domain> service: ..." } entry when a domain
    // is unreachable — not the normal ErrorLogResponse shape. Confirmed live
    // against the real gateway with wealth stopped.
    getConsoleErrors.mockResolvedValue({
      profile: [],
      wealth: [{ error: 'Could not reach wealth service: Connection refused' }],
      health: [],
      household: [],
    });

    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-wealth'));

    const wealthRow = screen.getByTestId('service-row-wealth');
    fireEvent.click(within(wealthRow).getByRole('button', { name: /expand wealth errors/i }));

    await waitFor(() => {
      expect(within(wealthRow).getByText('SERVICE_UNREACHABLE')).toBeInTheDocument();
      expect(within(wealthRow).getByText(/could not reach wealth service/i)).toBeInTheDocument();
    });
  });

  it('does not render an error panel for non-domain services (gateway/web)', async () => {
    renderConsole();
    await waitFor(() => screen.getByTestId('service-row-gateway'));

    const gatewayRow = screen.getByTestId('service-row-gateway');
    expect(within(gatewayRow).queryByRole('button', { name: /expand/i })).not.toBeInTheDocument();
  });
});

describe('ApplicationConsole protected route', () => {
  it('blocks unauthenticated users and redirects to signin', () => {
    useAuth.mockReturnValue({ user: null, hasRole: () => false, loading: false });

    render(
      <MemoryRouter initialEntries={['/admin/console']}>
        <Routes>
          <Route path="/signin" element={<div>Sign In Page</div>} />
          <Route
            path="/admin/console"
            element={
              <ProtectedRoute requiredRole="admin">
                <ApplicationConsole />
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Sign In Page')).toBeInTheDocument();
    expect(screen.queryByText(/application console/i)).not.toBeInTheDocument();
  });

  it('blocks user-role access and redirects to home', () => {
    useAuth.mockReturnValue({
      user: { username: 'bob', role: 'user' },
      hasRole: (role) => role === 'user',
      loading: false,
    });

    render(
      <MemoryRouter initialEntries={['/admin/console']}>
        <Routes>
          <Route path="/" element={<div>Home Page</div>} />
          <Route
            path="/admin/console"
            element={
              <ProtectedRoute requiredRole="admin">
                <ApplicationConsole />
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Home Page')).toBeInTheDocument();
    expect(screen.queryByText(/application console/i)).not.toBeInTheDocument();
  });
});
