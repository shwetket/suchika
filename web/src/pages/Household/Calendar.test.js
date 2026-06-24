import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Calendar } from './Calendar';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/household', () => ({
  listCalendarEvents: jest.fn(),
  createCalendarEvent: jest.fn(),
  updateCalendarEvent: jest.fn(),
  deleteCalendarEvent: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listCalendarEvents,
  createCalendarEvent,
  deleteCalendarEvent,
} = require('../../api/household');

const MOCK_PROFILES = [{ profile_id: 'p1', full_name: 'Alice', is_active: true }];

const MOCK_EVENTS = [
  {
    id: 'e1',
    profile_id: 'p1',
    title: 'Family Dinner',
    event_type: 'FAMILY',
    start_date: '2026-07-01',
    end_date: null,
    location: 'Home',
    notes: null,
  },
  {
    id: 'e2',
    profile_id: 'p1',
    title: 'Doctor Appointment',
    event_type: 'MEDICAL',
    start_date: '2026-07-05',
    end_date: '2026-07-06',
    location: null,
    notes: 'Check-up',
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listCalendarEvents.mockResolvedValue({ calendar_events: [] });
});

describe('Calendar page', () => {
  it('renders select profile prompt when no profile selected', async () => {
    render(<Calendar />);
    await waitFor(() => {
      expect(screen.getByText(/select a profile to view events/i)).toBeInTheDocument();
    });
  });

  it('renders event list after profile selection', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Family Dinner')).toBeInTheDocument();
      expect(screen.getByText('Doctor Appointment')).toBeInTheDocument();
    });
  });

  it('"Add Event" button opens the add form modal', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));

    fireEvent.click(screen.getByText('+ Add Event'));
    expect(screen.getByRole('heading', { name: /add event/i })).toBeInTheDocument();
  });

  it('shows conflict warning banner when create response has conflicting_events', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: [] });
    createCalendarEvent.mockResolvedValue({
      id: 'e3',
      title: 'New Event',
      conflicting_events: [{ id: 'e1', title: 'Family Dinner' }],
    });

    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));

    fireEvent.click(screen.getByText('+ Add Event'));

    fireEvent.change(screen.getByPlaceholderText('Event title'), {
      target: { name: 'title', value: 'New Event' },
    });

    const selects = screen.getAllByRole('combobox');
    const typeSelect = selects.find((s) => s.getAttribute('name') === 'event_type');
    fireEvent.change(typeSelect, { target: { value: 'PERSONAL' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/event created with conflicts/i)).toBeInTheDocument();
      expect(screen.getByText('Family Dinner')).toBeInTheDocument();
    });
  });

  it('shows inline delete confirmation and calls deleteCalendarEvent', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    deleteCalendarEvent.mockResolvedValue(null);
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    const deleteButtons = screen.getAllByText('Delete');
    fireEvent.click(deleteButtons[0]);

    expect(screen.getByText('Confirm?')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(deleteCalendarEvent).toHaveBeenCalledWith('e1');
    });
  });

  it('shows loading state while fetching events', async () => {
    listCalendarEvents.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ calendar_events: [] }), 300))
    );
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    expect(screen.getByText(/loading events/i)).toBeInTheDocument();
  });

  it('shows error message when API fails', async () => {
    listCalendarEvents.mockRejectedValue(new Error('Server error'));
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });
});
