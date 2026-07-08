import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { Vitals } from './Vitals';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/health', () => ({
  listVitals: jest.fn(),
  recordVital: jest.fn(),
  updateVital: jest.fn(),
  deleteVital: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listVitals, recordVital, updateVital } = require('../../api/health');

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
  listVitals.mockResolvedValue({ vital_readings: [] });
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
      () =>
        new Promise((resolve) => setTimeout(() => resolve({ vital_readings: MOCK_VITALS }), 200))
    );
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));
    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });
    expect(screen.getByText(/Loading vitals/i)).toBeInTheDocument();
  });

  it('shows vitals history table after profile selection', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Weight')).toBeInTheDocument();
      expect(screen.getByText('Blood Pressure')).toBeInTheDocument();
    });
  });

  it('renders actual rows (not just the total count) matching the real API response shape', async () => {
    // Regression test for a bug where the page read `data.vitals` instead of
    // `data.vital_readings` (the field name actually returned by the API per
    // health.yaml's ListVitalReadingsResponse schema). That bug caused the
    // total count to render correctly (it used the right key) while the
    // table body silently stayed empty. Asserting only on the count text
    // would NOT have caught it, so this test asserts on row content too.
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS, total_size: 2 });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 1 \(2 total\)/i)).toBeInTheDocument();
    });
    const table = screen.getByRole('table');
    expect(within(table).getByText('Weight')).toBeInTheDocument();
    expect(within(table).getByText('Blood Pressure')).toBeInTheDocument();
    expect(within(table).getByText('120 / 80')).toBeInTheDocument();
  });

  it('shows value_primary / value_secondary for blood pressure', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
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

  it('requests page 0 and the default page size on first load', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS, total_size: 2 });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(listVitals).toHaveBeenCalledWith('p1', null, 0, 20);
    });
  });

  it('shows pagination controls with page count derived from total_size', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS, total_size: 45 });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 3 \(45 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
  });

  it('clicking Next requests the next page', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS, total_size: 45 });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByRole('button', { name: 'Next' }));

    fireEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      expect(listVitals).toHaveBeenCalledWith('p1', null, 1, 20);
      expect(screen.getByText(/Page 2 of 3/i)).toBeInTheDocument();
    });
  });

  it('disables Next on the last page', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS, total_size: 2 });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 1 \(2 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('"Log Reading" button opens modal', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Reading'));

    fireEvent.click(screen.getByText('+ Log Reading'));
    expect(screen.getByRole('heading', { name: 'Log Reading' })).toBeInTheDocument();
  });

  it('shows Diastolic field only when Blood Pressure is selected', async () => {
    listVitals.mockResolvedValue({ vital_readings: [] });
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
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
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

  it('"Edit" button opens modal pre-filled with existing values', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Edit' }));

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    fireEvent.click(editButtons[0]);

    expect(screen.getByRole('heading', { name: 'Edit Reading' })).toBeInTheDocument();
    expect(screen.getByDisplayValue('72')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Morning reading')).toBeInTheDocument();
  });

  it('submits edit form and reloads vitals', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
    updateVital.mockResolvedValue({});
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Edit' }));

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    fireEvent.click(editButtons[0]);

    const valueInput = screen.getByDisplayValue('72');
    fireEvent.change(valueInput, { target: { name: 'value_primary', value: '71' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /Save Changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(updateVital).toHaveBeenCalledWith(
        'v1',
        expect.objectContaining({ value_primary: 71 })
      );
    });
  });

  it('shows error message when update fails', async () => {
    listVitals.mockResolvedValue({ vital_readings: MOCK_VITALS });
    updateVital.mockRejectedValue(new Error('Update failed'));
    render(<Vitals />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Edit' }));

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    fireEvent.click(editButtons[0]);

    const submitBtn = screen
      .getAllByRole('button', { name: /Save Changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });
});
