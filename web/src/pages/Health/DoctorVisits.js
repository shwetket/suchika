import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import {
  createDoctorVisit,
  deleteDoctorVisit,
  listDoctorVisits,
  updateDoctorVisit,
} from '../../api/health';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function addDays(dateStr, days) {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + days);
  return d;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function truncate(str, max) {
  if (!str) return '';
  return str.length > max ? `${str.slice(0, max)}…` : str;
}

function followUpBadgeClass(followUpDate) {
  if (!followUpDate) return null;
  const today = new Date(todayISO());
  const due = new Date(followUpDate);
  const in30 = addDays(todayISO(), 30);
  if (due < today) return 'overdue';
  if (due <= in30) return 'soon';
  return 'future';
}

const EMPTY_FORM = {
  from_date: todayISO(),
  to_date: '',
  visited_doctor: true,
  doctor_name: '',
  hospital_name: '',
  speciality: '',
  symptoms: '',
  diagnosis: '',
  notes: '',
  follow_up_date: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

function VisitFormFields({ form, onChange }) {
  return (
    <>
      <Field label="Visit Date / Start Date" required>
        <input
          name="from_date"
          type="date"
          value={form.from_date}
          onChange={onChange}
          className={inputClass}
        />
      </Field>

      <Field label="End Date">
        <input
          name="to_date"
          type="date"
          value={form.to_date}
          onChange={onChange}
          className={inputClass}
        />
      </Field>

      <div className="flex items-center gap-3">
        <input
          id="visited-doctor-check"
          name="visited_doctor"
          type="checkbox"
          checked={form.visited_doctor}
          onChange={onChange}
          className="w-4 h-4"
        />
        <label htmlFor="visited-doctor-check" className="text-sm font-medium text-gray-700">
          Saw a doctor
        </label>
      </div>

      <Field label="Doctor Name" required={form.visited_doctor}>
        <input
          name="doctor_name"
          type="text"
          value={form.doctor_name}
          onChange={onChange}
          placeholder="Optional"
          className={inputClass}
        />
      </Field>

      <Field label="Hospital Name">
        <input
          name="hospital_name"
          type="text"
          value={form.hospital_name}
          onChange={onChange}
          placeholder="Optional"
          className={inputClass}
        />
      </Field>

      <Field label="Speciality">
        <input
          name="speciality"
          type="text"
          value={form.speciality}
          onChange={onChange}
          placeholder="Optional"
          className={inputClass}
        />
      </Field>

      <Field label="Symptoms">
        <textarea
          name="symptoms"
          value={form.symptoms}
          onChange={onChange}
          placeholder="Optional"
          rows={2}
          className={inputClass}
        />
      </Field>

      <Field label="Diagnosis">
        <textarea
          name="diagnosis"
          value={form.diagnosis}
          onChange={onChange}
          placeholder="Optional"
          rows={2}
          className={inputClass}
        />
      </Field>

      <Field label="Notes">
        <textarea
          name="notes"
          value={form.notes}
          onChange={onChange}
          placeholder="Optional"
          rows={2}
          className={inputClass}
        />
      </Field>

      <Field label="Follow-up Date">
        <input
          name="follow_up_date"
          type="date"
          value={form.follow_up_date}
          onChange={onChange}
          className={inputClass}
        />
      </Field>
    </>
  );
}

VisitFormFields.propTypes = {
  form: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};

function FollowUpBadge({ followUpDate }) {
  const status = followUpBadgeClass(followUpDate);
  if (!status) return null;

  const badgeStyles = {
    overdue: 'bg-red-100 text-red-800',
    soon: 'bg-yellow-100 text-yellow-800',
    future: 'bg-green-100 text-green-800',
  };

  const labels = {
    overdue: 'Overdue',
    soon: 'Follow-up soon',
    future: formatDate(followUpDate),
  };

  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${badgeStyles[status]}`}>
      {labels[status]}
    </span>
  );
}

FollowUpBadge.propTypes = {
  followUpDate: PropTypes.string,
};

FollowUpBadge.defaultProps = { followUpDate: null };

function VisitCard({ visit, onEdit, onDelete }) {
  const dateLabel =
    visit.to_date && visit.to_date !== visit.from_date
      ? `${formatDate(visit.from_date)} → ${formatDate(visit.to_date)}`
      : formatDate(visit.from_date);

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-2">
      <div className="flex items-start justify-between gap-2">
        <p className="font-semibold text-gray-900 text-sm">{dateLabel}</p>
        {visit.visited_doctor ? (
          <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 whitespace-nowrap">
            Saw doctor
          </span>
        ) : (
          <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-500 whitespace-nowrap">
            No visit
          </span>
        )}
      </div>

      <div className="text-sm text-gray-600 space-y-1">
        {visit.doctor_name && (
          <p>
            <span className="font-medium">Doctor:</span> {visit.doctor_name}
            {visit.speciality ? ` (${visit.speciality})` : ''}
          </p>
        )}
        {visit.hospital_name && (
          <p>
            <span className="font-medium">Hospital:</span> {visit.hospital_name}
          </p>
        )}
        {visit.symptoms && <p className="text-gray-500">{truncate(visit.symptoms, 80)}</p>}
        {visit.diagnosis && (
          <p>
            <span className="font-medium">Dx:</span> {truncate(visit.diagnosis, 80)}
          </p>
        )}
      </div>

      {visit.follow_up_date && (
        <div className="pt-1">
          <FollowUpBadge followUpDate={visit.follow_up_date} />
        </div>
      )}

      <div className="flex gap-2 pt-2">
        <button
          type="button"
          onClick={() => onEdit(visit)}
          className="flex-1 border border-blue-600 text-blue-600 py-1.5 rounded text-sm font-medium hover:bg-blue-50"
        >
          Edit
        </button>
        <button
          type="button"
          onClick={() => onDelete(visit)}
          className="flex-1 border border-red-500 text-red-500 py-1.5 rounded text-sm font-medium hover:bg-red-50"
        >
          Delete
        </button>
      </div>
    </div>
  );
}

VisitCard.propTypes = {
  visit: PropTypes.shape({
    id: PropTypes.string.isRequired,
    from_date: PropTypes.string.isRequired,
    to_date: PropTypes.string,
    visited_doctor: PropTypes.bool.isRequired,
    doctor_name: PropTypes.string,
    hospital_name: PropTypes.string,
    speciality: PropTypes.string,
    symptoms: PropTypes.string,
    diagnosis: PropTypes.string,
    notes: PropTypes.string,
    follow_up_date: PropTypes.string,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
};

export const DoctorVisits = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [visits, setVisits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filterFrom, setFilterFrom] = useState('');
  const [filterTo, setFilterTo] = useState('');
  const [totalSize, setTotalSize] = useState(0);
  const [page, setPage] = useState(0);

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingVisit, setEditingVisit] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_FORM);
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    listProfiles(null, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, []);

  const loadVisits = useCallback(async () => {
    if (!selectedProfileId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listDoctorVisits(
        selectedProfileId,
        filterFrom || null,
        filterTo || null,
        page,
        PAGE_SIZE
      );
      setVisits(data.doctor_visits ?? []);
      setTotalSize(data.total_size ?? (data.doctor_visits ?? []).length);
    } catch (err) {
      setError(err.message || 'Failed to load visits');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId, filterFrom, filterTo, page]);

  useEffect(() => {
    loadVisits();
  }, [loadVisits]);

  // Reset to page 0 whenever the filters (not the page itself) change.
  useEffect(() => {
    setPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedProfileId, filterFrom, filterTo]);

  const totalPages = Math.max(1, Math.ceil(totalSize / PAGE_SIZE));

  const handleFormChange = useCallback(
    (setter) => (e) => {
      const { name, value, type, checked } = e.target;
      setter((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    },
    []
  );

  const handleAddChange = handleFormChange(setAddForm);
  const handleEditChange = handleFormChange(setEditForm);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.from_date) {
        setAddError('Visit date is required');
        return;
      }
      if (addForm.visited_doctor && !addForm.doctor_name.trim()) {
        setAddError('Doctor name is required when saw a doctor is checked');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await createDoctorVisit({
          profile_id: selectedProfileId,
          from_date: addForm.from_date,
          to_date: addForm.to_date || null,
          visited_doctor: addForm.visited_doctor,
          doctor_name: addForm.doctor_name || null,
          hospital_name: addForm.hospital_name || null,
          speciality: addForm.speciality || null,
          symptoms: addForm.symptoms || null,
          diagnosis: addForm.diagnosis || null,
          notes: addForm.notes || null,
          follow_up_date: addForm.follow_up_date || null,
        });
        setShowAdd(false);
        setAddForm(EMPTY_FORM);
        await loadVisits();
      } catch (err) {
        setAddError(err.message || 'Failed to log visit');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadVisits]
  );

  const handleEditOpen = useCallback((visit) => {
    setEditingVisit(visit);
    setEditForm({
      from_date: visit.from_date,
      to_date: visit.to_date ?? '',
      visited_doctor: visit.visited_doctor,
      doctor_name: visit.doctor_name ?? '',
      hospital_name: visit.hospital_name ?? '',
      speciality: visit.speciality ?? '',
      symptoms: visit.symptoms ?? '',
      diagnosis: visit.diagnosis ?? '',
      notes: visit.notes ?? '',
      follow_up_date: visit.follow_up_date ?? '',
    });
    setEditError(null);
  }, []);

  const handleEditSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (editForm.visited_doctor && !editForm.doctor_name.trim()) {
        setEditError('Doctor name is required when saw a doctor is checked');
        return;
      }
      setEditSaving(true);
      setEditError(null);
      try {
        await updateDoctorVisit(editingVisit.id, {
          to_date: editForm.to_date || null,
          doctor_name: editForm.doctor_name || null,
          hospital_name: editForm.hospital_name || null,
          speciality: editForm.speciality || null,
          symptoms: editForm.symptoms || null,
          diagnosis: editForm.diagnosis || null,
          notes: editForm.notes || null,
          follow_up_date: editForm.follow_up_date || null,
        });
        setEditingVisit(null);
        await loadVisits();
      } catch (err) {
        setEditError(err.message || 'Failed to update visit');
      } finally {
        setEditSaving(false);
      }
    },
    [editingVisit, editForm, loadVisits]
  );

  const handleDeleteConfirm = useCallback(async () => {
    if (!confirmTarget) return;
    setDeleting(true);
    try {
      await deleteDoctorVisit(confirmTarget.id);
      setConfirmTarget(null);
      await loadVisits();
    } catch (err) {
      setError(err.message || 'Failed to delete visit');
      setConfirmTarget(null);
    } finally {
      setDeleting(false);
    }
  }, [confirmTarget, loadVisits]);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Doctor Visits</h1>
          <p className="text-gray-500 mt-1">Record and review doctor appointments</p>
        </div>
        {selectedProfileId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_FORM);
              setAddError(null);
              setShowAdd(true);
            }}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700"
          >
            + Log Visit
          </button>
        )}
      </div>

      <div className="mb-6 flex flex-wrap gap-3 items-center">
        <select
          value={selectedProfileId}
          onChange={(e) => setSelectedProfileId(e.target.value)}
          className={`${inputClass} w-full sm:w-72`}
        >
          <option value="">Select a profile...</option>
          {profiles.map((p) => (
            <option key={p.profile_id} value={p.profile_id}>
              {p.full_name}
            </option>
          ))}
        </select>
        {selectedProfileId && (
          <>
            <input
              type="date"
              aria-label="From date"
              value={filterFrom}
              onChange={(e) => setFilterFrom(e.target.value)}
              className={inputClass}
            />
            <input
              type="date"
              aria-label="To date"
              value={filterTo}
              onChange={(e) => setFilterTo(e.target.value)}
              className={inputClass}
            />
          </>
        )}
      </div>

      {!selectedProfileId && (
        <div className="text-center py-16 text-gray-400">Select a profile to view visits.</div>
      )}

      {selectedProfileId && (
        <>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading && <div className="text-center py-16 text-gray-500">Loading visits...</div>}

          {!loading && visits.length === 0 && (
            <div className="text-center py-16 text-gray-400">
              No visits logged yet. Log your first visit above.
            </div>
          )}

          {!loading && visits.length > 0 && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {visits.map((v) => (
                  <VisitCard
                    key={v.id}
                    visit={v}
                    onEdit={handleEditOpen}
                    onDelete={setConfirmTarget}
                  />
                ))}
              </div>

              <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
                <span>
                  Page {page + 1} of {totalPages} ({totalSize} total)
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="px-3 py-1.5 border border-gray-300 rounded text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
                    disabled={page + 1 >= totalPages}
                    className="px-3 py-1.5 border border-gray-300 rounded text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              </div>
            </>
          )}
        </>
      )}

      {showAdd && (
        <Modal title="Log Visit" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <VisitFormFields form={addForm} onChange={handleAddChange} />
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
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {addSaving ? 'Saving...' : 'Log Visit'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingVisit && (
        <Modal title="Edit Visit" onClose={() => setEditingVisit(null)}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <VisitFormFields form={editForm} onChange={handleEditChange} />
            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingVisit(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={editSaving}
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {editSaving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {confirmTarget && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-bold mb-2">Delete Visit</h3>
            <p className="text-gray-600 text-sm mb-6">
              Delete the visit on <strong>{formatDate(confirmTarget.from_date)}</strong>? This
              cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setConfirmTarget(null)}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteConfirm}
                disabled={deleting}
                className="px-4 py-2 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700 disabled:opacity-50"
              >
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DoctorVisits;
