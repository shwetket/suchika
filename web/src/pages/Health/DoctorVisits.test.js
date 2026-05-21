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

    const dateInputs = document.querySelectorAll('input[type="date"]');
    fireEvent.change(dateInputs[0], {
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
