import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Vitals } from './Vitals';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/health', () => ({
  listVitals: jest.fn(),
  recordVital: jest.fn(),
  deleteVital: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listVitals, recordVital } = require('../../api/health');

const MOCK_PROFILES = [
  { profile_id: 'p1', full_name: 'Alice', is_active: true },
  { profile_id: 'p2', full_name: 'Bob', is_active: true },
];

const MOCK_VITALS = [
  {
    id: 'v1',
    profile_id: 'p1',
    vital_type: 'WEIGHT',
    reading_date: '2026-06-01',
    value_primary: 72,
    value_secondary: null,
    unit: 'kg',
    notes: 'Morning reading',
  },
  {
    id: 'v2',
    profile_id: 'p1',
    vital_type: 'BLOOD_PRESSURE',
    reading_date: '2026-06-02',
    value_primary: 120,
    value_secondary: 80,
    unit: 'mmHg',
    notes: null,
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listVitals.mockResolvedValue({ vitals: [] });
});

describe('Vitals page', () => {
  it('renders "Select a profile" message when no profile is selected', async () => {
    render(<Vitals />);
    await waitFor(() => {
      expect(screen.getByText(/Select a profile to view readings/i)).toBeInTheDocument();
    });
  });

  it('shows loading state during fetch', async () => {
    listVitals.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ vitals: MOCK_VITALS }), 200))
    );
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));
    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });
    expect(screen.getByText(/Loading vitals/i)).toBeInTheDocument();
  });

  it('shows vitals history table after profile selection', async () => {
    listVitals.mockResolvedValue({ vitals: MOCK_VITALS });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Weight')).toBeInTheDocument();
      expect(screen.getByText('Blood Pressure')).toBeInTheDocument();
    });
  });

  it('shows value_primary / value_secondary for blood pressure', async () => {
    listVitals.mockResolvedValue({ vitals: MOCK_VITALS });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('120 / 80')).toBeInTheDocument();
    });
  });

  it('shows error message on API failure', async () => {
    listVitals.mockRejectedValue(new Error('Network error'));
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('"Log Reading" button opens modal', async () => {
    listVitals.mockResolvedValue({ vitals: MOCK_VITALS });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Reading'));

    fireEvent.click(screen.getByText('+ Log Reading'));
    expect(screen.getByRole('heading', { name: 'Log Reading' })).toBeInTheDocument();
  });

  it('shows Diastolic field only when Blood Pressure is selected', async () => {
    listVitals.mockResolvedValue({ vitals: [] });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Reading'));

    fireEvent.click(screen.getByText('+ Log Reading'));

    expect(screen.queryByPlaceholderText(/e\.g\. 80/i)).not.toBeInTheDocument();

    const typeSelects = screen.getAllByRole('combobox');
    const vitalTypeSelect = typeSelects.find((s) => s.getAttribute('name') === 'vital_type');
    fireEvent.change(vitalTypeSelect, { target: { value: 'BLOOD_PRESSURE' } });

    expect(screen.getByPlaceholderText(/e\.g\. 80/i)).toBeInTheDocument();
  });

  it('submits log reading form and reloads vitals', async () => {
    listVitals.mockResolvedValue({ vitals: MOCK_VITALS });
    recordVital.mockResolvedValue({});
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Reading'));

    fireEvent.click(screen.getByText('+ Log Reading'));

    const typeSelects = screen.getAllByRole('combobox');
    const vitalTypeSelect = typeSelects.find((s) => s.getAttribute('name') === 'vital_type');
    fireEvent.change(vitalTypeSelect, { target: { value: 'WEIGHT' } });

    fireEvent.change(screen.getByPlaceholderText(/e\.g\. 72/i), {
      target: { name: 'value_primary', value: '75' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /Log Reading/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(recordVital).toHaveBeenCalled();
    });
  });
});
