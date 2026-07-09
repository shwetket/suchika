import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DoctorVisits } from './DoctorVisits';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/health', () => ({
  listDoctorVisits: jest.fn(),
  createDoctorVisit: jest.fn(),
  updateDoctorVisit: jest.fn(),
  deleteDoctorVisit: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listDoctorVisits,
  createDoctorVisit,
  updateDoctorVisit,
  deleteDoctorVisit,
} = require('../../api/health');

const MOCK_PROFILES = [
  { profile_id: 'p1', full_name: 'Alice', is_active: true },
  { profile_id: 'p2', full_name: 'Bob', is_active: true },
];

const MOCK_VISITS = [
  {
    id: 'dv1',
    profile_id: 'p1',
    from_date: '2026-06-01',
    to_date: '2026-06-01',
    visited_doctor: true,
    doctor_name: 'Dr. Smith',
    hospital_name: 'City Hospital',
    speciality: 'General',
    symptoms: 'Fever and cough',
    diagnosis: 'Viral infection',
    notes: null,
    follow_up_date: null,
  },
  {
    id: 'dv2',
    profile_id: 'p1',
    from_date: '2026-05-15',
    to_date: null,
    visited_doctor: false,
    doctor_name: null,
    hospital_name: null,
    speciality: null,
    symptoms: 'Headache',
    diagnosis: null,
    notes: 'Self-care',
    follow_up_date: null,
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
});

describe('DoctorVisits page', () => {
  it('renders "Select a profile" message when no profile is selected', async () => {
    render(<DoctorVisits />);
    await waitFor(() => {
      expect(screen.getByText(/Select a profile to view visits/i)).toBeInTheDocument();
    });
  });

  it('shows loading state during fetch', async () => {
    listDoctorVisits.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ doctor_visits: MOCK_VISITS }), 200))
    );
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    expect(screen.getByText(/Loading visits/i)).toBeInTheDocument();
  });

  it('shows visit cards after profile selection', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Fever and cough/i)).toBeInTheDocument();
      expect(screen.getByText(/Viral infection/i)).toBeInTheDocument();
      expect(screen.getByText(/Headache/i)).toBeInTheDocument();
    });
  });

  it('calls listDoctorVisits with null from/to when no date filter set', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(listDoctorVisits).toHaveBeenCalledWith('p1', null, null, 0, 20);
    });
  });

  it('passes from/to date filters through to listDoctorVisits', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByLabelText('From date'));

    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-01-01' } });
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-06-30' } });

    await waitFor(() => {
      expect(listDoctorVisits).toHaveBeenCalledWith('p1', '2026-01-01', '2026-06-30', 0, 20);
    });
  });

  it('shows pagination controls with page count derived from total_size', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS, total_size: 45 });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 3 \(45 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
  });

  it('clicking Next requests the next page', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS, total_size: 45 });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByRole('button', { name: 'Next' }));

    fireEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      expect(listDoctorVisits).toHaveBeenCalledWith('p1', null, null, 1, 20);
      expect(screen.getByText(/Page 2 of 3/i)).toBeInTheDocument();
    });
  });

  it('disables Next on the last page', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS, total_size: 2 });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 1 \(2 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('shows error message on API failure', async () => {
    listDoctorVisits.mockRejectedValue(new Error('Server error'));
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });

  it('"Log Visit" button opens modal', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Visit'));

    fireEvent.click(screen.getByText('+ Log Visit'));
    expect(screen.getByRole('heading', { name: 'Log Visit' })).toBeInTheDocument();
  });

  it('visited_doctor checkbox toggles doctor_name required label', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Visit'));

    fireEvent.click(screen.getByText('+ Log Visit'));

    const checkbox = screen.getByLabelText(/Saw a doctor/i);
    expect(checkbox).toBeChecked();

    const doctorNameLabels = screen.getAllByText(/Doctor Name/i);
    const requiredMarker = doctorNameLabels[0].closest('label');
    expect(requiredMarker).not.toBeNull();

    fireEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });

  it('submits create form and reloads visits', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    createDoctorVisit.mockResolvedValue({});
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Visit'));

    fireEvent.click(screen.getByText('+ Log Visit'));

    fireEvent.change(document.querySelector('input[name="from_date"]'), {
      target: { name: 'from_date', value: '2026-06-19' },
    });

    const doctorNameInputs = screen.getAllByPlaceholderText('Optional');
    fireEvent.change(doctorNameInputs[0], {
      target: { name: 'doctor_name', value: 'Dr. Jones' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /Log Visit/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createDoctorVisit).toHaveBeenCalled();
    });
  });

  it('shows "No visits logged" empty state when there are none', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/No visits logged yet/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when from_date is cleared on add', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Visit'));
    fireEvent.click(screen.getByText('+ Log Visit'));

    fireEvent.change(document.querySelector('input[name="from_date"]'), {
      target: { name: 'from_date', value: '' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /Log Visit/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    expect(screen.getByText(/Visit date is required/i)).toBeInTheDocument();
    expect(createDoctorVisit).not.toHaveBeenCalled();
  });

  it('shows validation error when doctor name is required but missing', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Visit'));
    fireEvent.click(screen.getByText('+ Log Visit'));

    const submitBtn = screen
      .getAllByRole('button', { name: /Log Visit/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    expect(
      screen.getByText(/Doctor name is required when saw a doctor is checked/i)
    ).toBeInTheDocument();
    expect(createDoctorVisit).not.toHaveBeenCalled();
  });

  it('shows add error message when createDoctorVisit fails', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: [] });
    createDoctorVisit.mockRejectedValue(new Error('Create failed'));
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Log Visit'));
    fireEvent.click(screen.getByText('+ Log Visit'));

    fireEvent.change(document.querySelector('input[name="from_date"]'), {
      target: { name: 'from_date', value: '2026-06-19' },
    });
    const doctorNameInputs = screen.getAllByPlaceholderText('Optional');
    fireEvent.change(doctorNameInputs[0], {
      target: { name: 'doctor_name', value: 'Dr. Jones' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /Log Visit/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Create failed')).toBeInTheDocument();
    });
  });

  it('opens edit modal pre-filled and submits the update', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    updateDoctorVisit.mockResolvedValue({});
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Edit' })[0]);

    fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);
    expect(screen.getByRole('heading', { name: 'Edit Visit' })).toBeInTheDocument();

    const submitBtn = screen
      .getAllByRole('button', { name: /Save Changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(updateDoctorVisit).toHaveBeenCalledWith('dv1', expect.any(Object));
    });
  });

  it('shows edit validation error when doctor name required but missing', async () => {
    const visitNeedingDoctor = {
      ...MOCK_VISITS[0],
      doctor_name: '',
    };
    listDoctorVisits.mockResolvedValue({ doctor_visits: [visitNeedingDoctor] });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Edit' })[0]);

    fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);

    const submitBtn = screen
      .getAllByRole('button', { name: /Save Changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    expect(
      screen.getByText(/Doctor name is required when saw a doctor is checked/i)
    ).toBeInTheDocument();
    expect(updateDoctorVisit).not.toHaveBeenCalled();
  });

  it('shows edit error message when updateDoctorVisit fails', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    updateDoctorVisit.mockRejectedValue(new Error('Update failed'));
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Edit' })[0]);
    fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);

    const submitBtn = screen
      .getAllByRole('button', { name: /Save Changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });

  it('deletes a visit via the confirm dialog', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    deleteDoctorVisit.mockResolvedValue(null);
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Delete' })[0]);

    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0]);
    expect(screen.getByRole('heading', { name: 'Delete Visit' })).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole('button', { name: 'Delete' });
    fireEvent.click(deleteButtons[deleteButtons.length - 1]);

    await waitFor(() => {
      expect(deleteDoctorVisit).toHaveBeenCalledWith('dv1');
    });
  });

  it('cancels the delete confirm dialog without calling deleteDoctorVisit', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Delete' })[0]);

    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0]);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('heading', { name: 'Delete Visit' })).not.toBeInTheDocument();
    expect(deleteDoctorVisit).not.toHaveBeenCalled();
  });

  it('shows delete error message when deleteDoctorVisit fails', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS });
    deleteDoctorVisit.mockRejectedValue(new Error('Delete failed'));
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByRole('button', { name: 'Delete' })[0]);
    fireEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0]);
    const deleteButtons = screen.getAllByRole('button', { name: 'Delete' });
    fireEvent.click(deleteButtons[deleteButtons.length - 1]);

    await waitFor(() => {
      expect(screen.getByText('Delete failed')).toBeInTheDocument();
    });
  });

  it('clicking Previous requests the prior page', async () => {
    listDoctorVisits.mockResolvedValue({ doctor_visits: MOCK_VISITS, total_size: 45 });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByRole('button', { name: 'Next' }));
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => screen.getByText(/Page 2 of 3/i));

    fireEvent.click(screen.getByRole('button', { name: 'Previous' }));

    await waitFor(() => {
      expect(listDoctorVisits).toHaveBeenCalledWith('p1', null, null, 0, 20);
    });
  });

  it('renders follow-up badges for overdue, soon, and future dates', async () => {
    const withFollowUps = [
      { ...MOCK_VISITS[0], id: 'dv-overdue', follow_up_date: '2020-01-01' },
      {
        ...MOCK_VISITS[0],
        id: 'dv-soon',
        follow_up_date: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
      },
      {
        ...MOCK_VISITS[0],
        id: 'dv-future',
        follow_up_date: new Date(Date.now() + 200 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
      },
    ];
    listDoctorVisits.mockResolvedValue({ doctor_visits: withFollowUps });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Overdue')).toBeInTheDocument();
      expect(screen.getByText('Follow-up soon')).toBeInTheDocument();
    });
  });

  it('renders a date range label when to_date differs from from_date', async () => {
    listDoctorVisits.mockResolvedValue({
      doctor_visits: [{ ...MOCK_VISITS[0], to_date: '2026-06-05' }],
    });
    render(<DoctorVisits />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/→/)).toBeInTheDocument();
    });
  });
});
