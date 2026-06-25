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

  it('shows empty state message when no events exist for profile', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: [] });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/no events yet/i)).toBeInTheDocument();
    });
  });

  it('shows error when delete API call fails', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    deleteCalendarEvent.mockRejectedValue(new Error('Delete failed'));
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(screen.getByText('Delete failed')).toBeInTheDocument();
    });
  });

  it('cancels delete confirmation when Cancel is clicked', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    expect(screen.getByText('Confirm?')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Confirm?')).not.toBeInTheDocument();
  });

  it('shows event type filter dropdown after profile selection', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('All Types')).toBeInTheDocument();
    });
  });

  it('filters events when type filter is changed', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('All Types'));

    const selects = screen.getAllByRole('combobox');
    const typeFilter = selects.find((s) => s.textContent.includes('All Types'));
    fireEvent.change(typeFilter, { target: { value: 'FAMILY' } });

    await waitFor(() => {
      expect(listCalendarEvents).toHaveBeenCalledWith('p1', 'FAMILY');
    });
  });

  it('shows validation error when event type is missing on add', async () => {
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));
    fireEvent.click(screen.getByText('+ Add Event'));

    fireEvent.change(screen.getByPlaceholderText('Event title'), {
      target: { name: 'title', value: 'My Event' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Event type is required')).toBeInTheDocument();
    });
  });

  it('shows validation error when title is empty on add', async () => {
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));
    fireEvent.click(screen.getByText('+ Add Event'));

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Title is required')).toBeInTheDocument();
    });
  });

  it('shows error when create API call fails', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: [] });
    createCalendarEvent.mockRejectedValue(new Error('Create failed'));
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
      expect(screen.getByText('Create failed')).toBeInTheDocument();
    });
  });

  it('opens edit modal when Edit button is clicked', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /edit event/i })).toBeInTheDocument();
    });
  });

  it('submits edit form and reloads events', async () => {
    const { updateCalendarEvent } = require('../../api/household');
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    updateCalendarEvent.mockResolvedValue({});
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit event/i }));

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(updateCalendarEvent).toHaveBeenCalledWith(
        'e1',
        expect.objectContaining({ title: 'Family Dinner' })
      );
    });
  });

  it('shows error when edit API call fails', async () => {
    const { updateCalendarEvent } = require('../../api/household');
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    updateCalendarEvent.mockRejectedValue(new Error('Update failed'));
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit event/i }));

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });

  it('shows validation error when title is empty on edit', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit event/i }));

    const titleInput = screen.getAllByPlaceholderText('Event title')[0];
    fireEvent.change(titleInput, { target: { name: 'title', value: '' } });

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Title is required')).toBeInTheDocument();
    });
  });

  it('shows date range when event has both start and end dates', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Doctor Appointment')).toBeInTheDocument();
    });
    expect(screen.getByText(/→/)).toBeInTheDocument();
  });

  it('renders event with unknown event_type using fallback colour', async () => {
    const unknownTypeEvent = [
      {
        id: 'ex',
        profile_id: 'p1',
        title: 'Mystery Event',
        event_type: 'UNKNOWN_TYPE',
        start_date: '2026-07-10',
        end_date: null,
        location: null,
        notes: null,
      },
    ];
    listCalendarEvents.mockResolvedValue({ calendar_events: unknownTypeEvent });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Mystery Event')).toBeInTheDocument();
    });
  });

  it('uses empty array when listProfiles returns no profiles key', async () => {
    listProfiles.mockResolvedValue({});
    render(<Calendar />);
    await waitFor(() => {
      expect(screen.queryByText('Alice')).not.toBeInTheDocument();
    });
  });

  it('falls back to events key when calendar_events is missing from response', async () => {
    listCalendarEvents.mockResolvedValue({ events: MOCK_EVENTS });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Family Dinner')).toBeInTheDocument();
    });
  });

  it('uses empty array when API response has no events key', async () => {
    listCalendarEvents.mockResolvedValue({});
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/no events yet/i)).toBeInTheDocument();
    });
  });

  it('shows fallback error message when load fails without error message', async () => {
    listCalendarEvents.mockRejectedValue(new Error());
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Failed to load events')).toBeInTheDocument();
    });
  });

  it('shows start_date validation error when date is cleared', async () => {
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));
    fireEvent.click(screen.getByText('+ Add Event'));

    fireEvent.change(screen.getByPlaceholderText('Event title'), {
      target: { name: 'title', value: 'My Event' },
    });
    const selects = screen.getAllByRole('combobox');
    const typeSelect = selects.find((s) => s.getAttribute('name') === 'event_type');
    fireEvent.change(typeSelect, { target: { value: 'PERSONAL' } });

    const dateInput = screen.getByDisplayValue(/\d{4}-\d{2}-\d{2}/);
    fireEvent.change(dateInput, { target: { name: 'start_date', value: '' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Start date is required')).toBeInTheDocument();
    });
  });

  it('creates event without conflicts and shows no conflict banner', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: [] });
    createCalendarEvent.mockResolvedValue({ id: 'e5', title: 'Solo Event' });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));
    fireEvent.click(screen.getByText('+ Add Event'));

    fireEvent.change(screen.getByPlaceholderText('Event title'), {
      target: { name: 'title', value: 'Solo Event' },
    });
    const selects = screen.getAllByRole('combobox');
    const typeSelect = selects.find((s) => s.getAttribute('name') === 'event_type');
    fireEvent.change(typeSelect, { target: { value: 'PERSONAL' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createCalendarEvent).toHaveBeenCalled();
      expect(screen.queryByText(/event created with conflicts/i)).not.toBeInTheDocument();
    });
  });

  it('shows conflict banner with event having null id', async () => {
    createCalendarEvent.mockResolvedValue({
      id: 'e6',
      title: 'Test',
      conflicting_events: [{ id: null, title: 'Overlap Event' }],
    });
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));
    fireEvent.click(screen.getByText('+ Add Event'));

    fireEvent.change(screen.getByPlaceholderText('Event title'), {
      target: { name: 'title', value: 'Test' },
    });
    const selects = screen.getAllByRole('combobox');
    const typeSelect = selects.find((s) => s.getAttribute('name') === 'event_type');
    fireEvent.change(typeSelect, { target: { value: 'WORK' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Overlap Event')).toBeInTheDocument();
    });
  });

  it('shows fallback error message when create fails without error message', async () => {
    createCalendarEvent.mockRejectedValue(new Error());
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Event'));
    fireEvent.click(screen.getByText('+ Add Event'));

    fireEvent.change(screen.getByPlaceholderText('Event title'), {
      target: { name: 'title', value: 'Test' },
    });
    const selects = screen.getAllByRole('combobox');
    const typeSelect = selects.find((s) => s.getAttribute('name') === 'event_type');
    fireEvent.change(typeSelect, { target: { value: 'WORK' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add event/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Failed to create event')).toBeInTheDocument();
    });
  });

  it('submits edit form for event with end_date and notes (e2)', async () => {
    const { updateCalendarEvent } = require('../../api/household');
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    updateCalendarEvent.mockResolvedValue({});
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[1]);
    await waitFor(() => screen.getByRole('heading', { name: /edit event/i }));

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(updateCalendarEvent).toHaveBeenCalledWith(
        'e2',
        expect.objectContaining({ title: 'Doctor Appointment' })
      );
    });
  });

  it('shows fallback error when edit fails without error message', async () => {
    const { updateCalendarEvent } = require('../../api/household');
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    updateCalendarEvent.mockRejectedValue(new Error());
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    await waitFor(() => screen.getByRole('heading', { name: /edit event/i }));

    const submitBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Failed to update event')).toBeInTheDocument();
    });
  });

  it('shows fallback error message when delete fails without error message', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: MOCK_EVENTS });
    deleteCalendarEvent.mockRejectedValue(new Error());
    render(<Calendar />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(screen.getByText('Failed to delete event')).toBeInTheDocument();
    });
  });

  it('resets conflictWarning when profile is changed', async () => {
    listCalendarEvents.mockResolvedValue({ calendar_events: [] });
    createCalendarEvent.mockResolvedValue({
      id: 'e3',
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

    await waitFor(() => screen.getByText(/event created with conflicts/i));

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: '' } });
    expect(screen.queryByText(/event created with conflicts/i)).not.toBeInTheDocument();
  });
});
