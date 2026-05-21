import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import {
  createCalendarEvent,
  deleteCalendarEvent,
  listCalendarEvents,
  updateCalendarEvent,
} from '../../api/household';

const EVENT_TYPES = [
  'PERSONAL',
  'FAMILY',
  'SCHOOL',
  'TRAVEL',
  'MEDICAL',
  'WORK',
  'ANNIVERSARY',
  'OTHER',
];

const EVENT_TYPE_COLOURS = {
  PERSONAL: 'bg-blue-100 text-blue-800',
  FAMILY: 'bg-purple-100 text-purple-800',
  SCHOOL: 'bg-yellow-100 text-yellow-800',
  TRAVEL: 'bg-teal-100 text-teal-800',
  MEDICAL: 'bg-red-100 text-red-800',
  WORK: 'bg-gray-100 text-gray-800',
  ANNIVERSARY: 'bg-pink-100 text-pink-800',
  OTHER: 'bg-slate-100 text-slate-700',
};

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

const EMPTY_FORM = {
  title: '',
  event_type: '',
  start_date: todayISO(),
  end_date: '',
  location: '',
  notes: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full';

function Modal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b">
          <h2 className="text-xl font-bold">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
          >
            &times;
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}

Modal.propTypes = {
  title: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
};

function Field({ label, required, children }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-gray-700">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>
      {children}
    </div>
  );
}

Field.propTypes = {
  label: PropTypes.string.isRequired,
  required: PropTypes.bool,
  children: PropTypes.node.isRequired,
};

Field.defaultProps = { required: false };

function EventFormFields({ form, onChange }) {
  return (
    <>
      <Field label="Title" required>
        <input
          name="title"
          type="text"
          value={form.title}
          onChange={onChange}
          placeholder="Event title"
          className={inputClass}
        />
      </Field>
      <Field label="Event Type" required>
        <select
          name="event_type"
          value={form.event_type}
          onChange={onChange}
          className={inputClass}
        >
          <option value="">Select type</option>
          {EVENT_TYPES.map((t) => (
            <option key={t} value={t}>
              {t.charAt(0) + t.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Start Date" required>
        <input
          name="start_date"
          type="date"
          value={form.start_date}
          onChange={onChange}
          className={inputClass}
        />
      </Field>
      <Field label="End Date">
        <input
          name="end_date"
          type="date"
          value={form.end_date}
          onChange={onChange}
          className={inputClass}
        />
      </Field>
      <Field label="Location">
        <input
          name="location"
          type="text"
          value={form.location}
          onChange={onChange}
          placeholder="Optional"
          className={inputClass}
        />
      </Field>
      <Field label="Notes">
        <textarea
          name="notes"
          value={form.notes}
          onChange={onChange}
          placeholder="Optional"
          rows={3}
          className={inputClass}
        />
      </Field>
    </>
  );
}

EventFormFields.propTypes = {
  form: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};

function ConflictWarning({ conflictingEvents }) {
  if (!conflictingEvents || conflictingEvents.length === 0) return null;
  return (
    <div className="bg-yellow-50 border border-yellow-300 text-yellow-800 rounded px-4 py-3 mb-4 text-sm">
      <strong>Event created with conflicts:</strong>
      <ul className="mt-1 list-disc list-inside">
        {conflictingEvents.map((ev) => (
          <li key={ev.id ?? ev.title}>{ev.title}</li>
        ))}
      </ul>
    </div>
  );
}

ConflictWarning.propTypes = {
  conflictingEvents: PropTypes.arrayOf(
    PropTypes.shape({ id: PropTypes.string, title: PropTypes.string })
  ),
};

ConflictWarning.defaultProps = { conflictingEvents: null };

function EventRow({ event, onEdit, onDelete }) {
  const [confirmDelete, setConfirmDelete] = useState(false);
  const colour = EVENT_TYPE_COLOURS[event.event_type] || 'bg-gray-100 text-gray-700';
  const dateLine =
    event.end_date && event.end_date !== event.start_date
      ? `${formatDate(event.start_date)} → ${formatDate(event.end_date)}`
      : formatDate(event.start_date);

  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="px-4 py-3 text-sm text-gray-900 font-medium">{event.title}</td>
      <td className="px-4 py-3">
        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colour}`}>
          {event.event_type}
        </span>
      </td>
      <td className="px-4 py-3 text-sm text-gray-600 whitespace-nowrap">{dateLine}</td>
      <td className="px-4 py-3 text-sm text-gray-500">{event.location || '—'}</td>
      <td className="px-4 py-3 text-sm">
        {confirmDelete ? (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => onDelete(event.id)}
              className="px-2 py-1 bg-red-600 text-white rounded text-xs font-medium hover:bg-red-700"
            >
              Confirm?
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              className="px-2 py-1 border border-gray-300 text-gray-600 rounded text-xs hover:bg-gray-50"
            >
              Cancel
            </button>
          </div>
        ) : (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => onEdit(event)}
              className="px-2 py-1 border border-indigo-400 text-indigo-600 rounded text-xs hover:bg-indigo-50"
            >
              Edit
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(true)}
              className="px-2 py-1 border border-red-400 text-red-500 rounded text-xs hover:bg-red-50"
            >
              Delete
            </button>
          </div>
        )}
      </td>
    </tr>
  );
}

EventRow.propTypes = {
  event: PropTypes.shape({
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    event_type: PropTypes.string.isRequired,
    start_date: PropTypes.string.isRequired,
    end_date: PropTypes.string,
    location: PropTypes.string,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
};

export const Calendar = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [typeFilter, setTypeFilter] = useState('');
  const [conflictWarning, setConflictWarning] = useState(null);

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingEvent, setEditingEvent] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_FORM);
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  useEffect(() => {
    listProfiles(null, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, []);

  const loadEvents = useCallback(async () => {
    if (!selectedProfileId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listCalendarEvents(selectedProfileId, typeFilter || null);
      setEvents(data.calendar_events ?? data.events ?? []);
    } catch (err) {
      setError(err.message || 'Failed to load events');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId, typeFilter]);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  const handleChange = useCallback(
    (setter) => (e) => {
      const { name, value } = e.target;
      setter((prev) => ({ ...prev, [name]: value }));
    },
    []
  );

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.title.trim()) {
        setAddError('Title is required');
        return;
      }
      if (!addForm.event_type) {
        setAddError('Event type is required');
        return;
      }
      if (!addForm.start_date) {
        setAddError('Start date is required');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        const response = await createCalendarEvent({
          profile_id: selectedProfileId,
          title: addForm.title.trim(),
          event_type: addForm.event_type,
          start_date: addForm.start_date,
          end_date: addForm.end_date || null,
          location: addForm.location || null,
          notes: addForm.notes || null,
        });
        setShowAdd(false);
        setAddForm(EMPTY_FORM);
        const conflicts = response?.conflicting_events ?? [];
        setConflictWarning(conflicts.length > 0 ? conflicts : null);
        await loadEvents();
      } catch (err) {
        setAddError(err.message || 'Failed to create event');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadEvents]
  );

  const handleEditOpen = useCallback((event) => {
    setEditingEvent(event);
    setEditForm({
      title: event.title,
      event_type: event.event_type,
      start_date: event.start_date,
      end_date: event.end_date ?? '',
      location: event.location ?? '',
      notes: event.notes ?? '',
    });
    setEditError(null);
  }, []);

  const handleEditSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!editForm.title.trim()) {
        setEditError('Title is required');
        return;
      }
      setEditSaving(true);
      setEditError(null);
      try {
        await updateCalendarEvent(editingEvent.id, {
          title: editForm.title.trim(),
          event_type: editForm.event_type || null,
          start_date: editForm.start_date || null,
          end_date: editForm.end_date || null,
          location: editForm.location || null,
          notes: editForm.notes || null,
        });
        setEditingEvent(null);
        await loadEvents();
      } catch (err) {
        setEditError(err.message || 'Failed to update event');
      } finally {
        setEditSaving(false);
      }
    },
    [editingEvent, editForm, loadEvents]
  );

  const handleDelete = useCallback(
    async (id) => {
      try {
        await deleteCalendarEvent(id);
        await loadEvents();
      } catch (err) {
        setError(err.message || 'Failed to delete event');
      }
    },
    [loadEvents]
  );

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Household Calendar</h1>
          <p className="text-gray-500 mt-1">Manage household calendar events</p>
        </div>
        {selectedProfileId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_FORM);
              setAddError(null);
              setConflictWarning(null);
              setShowAdd(true);
            }}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-indigo-700"
          >
            + Add Event
          </button>
        )}
      </div>

      <div className="mb-6 flex flex-col sm:flex-row gap-3">
        <select
          value={selectedProfileId}
          onChange={(e) => {
            setSelectedProfileId(e.target.value);
            setConflictWarning(null);
          }}
          className={`${inputClass} sm:w-72`}
        >
          <option value="">Select a profile...</option>
          {profiles.map((p) => (
            <option key={p.profile_id} value={p.profile_id}>
              {p.full_name}
            </option>
          ))}
        </select>

        {selectedProfileId && (
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className={`${inputClass} sm:w-60`}
          >
            <option value="">All Types</option>
            {EVENT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t.charAt(0) + t.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        )}
      </div>

      {!selectedProfileId && (
        <div className="text-center py-16 text-gray-400">Select a profile to view events.</div>
      )}

      {selectedProfileId && (
        <>
          <ConflictWarning conflictingEvents={conflictWarning} />

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading && <div className="text-center py-16 text-gray-500">Loading events...</div>}

          {!loading && events.length === 0 && (
            <div className="text-center py-16 text-gray-400">
              No events yet. Add your first event above.
            </div>
          )}

          {!loading && events.length > 0 && (
            <div className="overflow-x-auto rounded-lg border border-gray-200">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    {['Title', 'Type', 'Date(s)', 'Location', 'Actions'].map((h) => (
                      <th
                        key={h}
                        className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider"
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {events.map((ev) => (
                    <EventRow
                      key={ev.id}
                      event={ev}
                      onEdit={handleEditOpen}
                      onDelete={handleDelete}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {showAdd && (
        <Modal title="Add Event" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <EventFormFields form={addForm} onChange={handleChange(setAddForm)} />
            {addError && <p className="text-red-600 text-sm">{addError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowAdd(false)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={addSaving}
                className="px-4 py-2 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {addSaving ? 'Saving...' : 'Add Event'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingEvent && (
        <Modal title="Edit Event" onClose={() => setEditingEvent(null)}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <EventFormFields form={editForm} onChange={handleChange(setEditForm)} />
            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingEvent(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={editSaving}
                className="px-4 py-2 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {editSaving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default Calendar;
