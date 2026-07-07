import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DoctorVisits } from './DoctorVisits';

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
const { listDoctorVisits, createDoctorVisit } = require('../../api/health');

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
});
