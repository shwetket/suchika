import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import { deleteVital, listVitals, recordVital } from '../../api/health';

const VITAL_TYPES = [
  'WEIGHT',
  'HEIGHT',
  'BLOOD_PRESSURE',
  'BLOOD_SUGAR_FASTING',
  'BLOOD_SUGAR_PP',
  'HEART_RATE',
  'TEMPERATURE',
  'OXYGEN_SATURATION',
  'BMI',
  'WAIST_CIRCUMFERENCE',
];

const UNIT_DEFAULTS = {
  WEIGHT: 'kg',
  HEIGHT: 'cm',
  BLOOD_PRESSURE: 'mmHg',
  BLOOD_SUGAR_FASTING: 'mg/dL',
  BLOOD_SUGAR_PP: 'mg/dL',
  HEART_RATE: 'bpm',
  TEMPERATURE: '°C',
  OXYGEN_SATURATION: '%',
  BMI: 'kg/m²',
  WAIST_CIRCUMFERENCE: 'cm',
};

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatVitalType(vitalType) {
  if (!vitalType) return '';
  return vitalType
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

const EMPTY_FORM = {
  vital_type: '',
  reading_date: todayISO(),
  value_primary: '',
  value_secondary: '',
  unit: '',
  notes: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

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

function VitalRow({ vital, onDelete }) {
  const [confirmDelete, setConfirmDelete] = useState(false);

  const valueDisplay =
    vital.value_secondary !== null && vital.value_secondary !== undefined
      ? `${vital.value_primary} / ${vital.value_secondary}`
      : String(vital.value_primary);

  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="px-4 py-3 text-sm text-gray-700 whitespace-nowrap">
        {formatDate(vital.reading_date)}
      </td>
      <td className="px-4 py-3 text-sm text-gray-700">{formatVitalType(vital.vital_type)}</td>
      <td className="px-4 py-3 text-sm text-gray-900 font-medium">{valueDisplay}</td>
      <td className="px-4 py-3 text-sm text-gray-500">{vital.unit || '—'}</td>
      <td className="px-4 py-3 text-sm text-gray-500 max-w-xs truncate">{vital.notes || '—'}</td>
      <td className="px-4 py-3 text-sm">
        {confirmDelete ? (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => onDelete(vital.id)}
              className="px-2 py-1 bg-red-600 text-white rounded text-xs font-medium hover:bg-red-700"
            >
              Confirm Delete?
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
          <button
            type="button"
            onClick={() => setConfirmDelete(true)}
            className="px-2 py-1 border border-red-400 text-red-500 rounded text-xs hover:bg-red-50"
          >
            Delete
          </button>
        )}
      </td>
    </tr>
  );
}

VitalRow.propTypes = {
  vital: PropTypes.shape({
    id: PropTypes.string.isRequired,
    vital_type: PropTypes.string.isRequired,
    reading_date: PropTypes.string.isRequired,
    value_primary: PropTypes.number.isRequired,
    value_secondary: PropTypes.number,
    unit: PropTypes.string,
    notes: PropTypes.string,
  }).isRequired,
  onDelete: PropTypes.func.isRequired,
};

export const Vitals = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [vitals, setVitals] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [typeFilter, setTypeFilter] = useState('');

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  useEffect(() => {
    listProfiles(null, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, []);

  const loadVitals = useCallback(async () => {
    if (!selectedProfileId) return;
    setLoading(true);
    setError(null);
    try {
      const vitalTypeArg = typeFilter || null;
      const data = await listVitals(selectedProfileId, vitalTypeArg);
      setVitals(data.vitals ?? []);
    } catch (err) {
      setError(err.message || 'Failed to load vitals');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId, typeFilter]);

  useEffect(() => {
    loadVitals();
  }, [loadVitals]);

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    if (name === 'vital_type') {
      setAddForm((prev) => ({
        ...prev,
        vital_type: value,
        unit: UNIT_DEFAULTS[value] || '',
        value_secondary: '',
      }));
    } else {
      setAddForm((prev) => ({ ...prev, [name]: value }));
    }
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.vital_type) {
        setAddError('Vital type is required');
        return;
      }
      if (!addForm.reading_date) {
        setAddError('Reading date is required');
        return;
      }
      if (addForm.value_primary === '') {
        setAddError('Value is required');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await recordVital({
          profile_id: selectedProfileId,
          vital_type: addForm.vital_type,
          reading_date: addForm.reading_date,
          value_primary: Number(addForm.value_primary),
          value_secondary: addForm.value_secondary !== '' ? Number(addForm.value_secondary) : null,
          unit: addForm.unit || null,
          notes: addForm.notes || null,
        });
        setShowAdd(false);
        setAddForm(EMPTY_FORM);
        await loadVitals();
      } catch (err) {
        setAddError(err.message || 'Failed to log reading');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadVitals]
  );

  const handleDelete = useCallback(
    async (id) => {
      try {
        await deleteVital(id);
        await loadVitals();
      } catch (err) {
        setError(err.message || 'Failed to delete reading');
      }
    },
    [loadVitals]
  );

  const isBloodPressure = addForm.vital_type === 'BLOOD_PRESSURE';

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Vital Signs</h1>
          <p className="text-gray-500 mt-1">Track vital sign readings over time</p>
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
            + Log Reading
          </button>
        )}
      </div>

      <div className="mb-6 flex flex-col sm:flex-row gap-3">
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
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className={`${inputClass} w-full sm:w-60`}
          >
            <option value="">All Types</option>
            {VITAL_TYPES.map((t) => (
              <option key={t} value={t}>
                {formatVitalType(t)}
              </option>
            ))}
          </select>
        )}
      </div>

      {!selectedProfileId && (
        <div className="text-center py-16 text-gray-400">Select a profile to view readings.</div>
      )}

      {selectedProfileId && (
        <>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading && <div className="text-center py-16 text-gray-500">Loading vitals...</div>}

          {!loading && vitals.length === 0 && (
            <div className="text-center py-16 text-gray-400">
              No readings logged yet. Log your first reading above.
            </div>
          )}

          {!loading && vitals.length > 0 && (
            <div className="overflow-x-auto rounded-lg border border-gray-200">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      Date
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      Type
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      Value
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      Unit
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      Notes
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white">
                  {vitals.map((v) => (
                    <VitalRow key={v.id} vital={v} onDelete={handleDelete} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {showAdd && (
        <Modal title="Log Reading" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <Field label="Vital Type" required>
              <select
                name="vital_type"
                value={addForm.vital_type}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select type</option>
                {VITAL_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {formatVitalType(t)}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Reading Date" required>
              <input
                name="reading_date"
                type="date"
                value={addForm.reading_date}
                onChange={handleAddChange}
                className={inputClass}
              />
            </Field>

            <Field label="Value" required>
              <input
                name="value_primary"
                type="number"
                value={addForm.value_primary}
                onChange={handleAddChange}
                placeholder="e.g. 72"
                className={inputClass}
              />
            </Field>

            {isBloodPressure && (
              <Field label="Diastolic">
                <input
                  name="value_secondary"
                  type="number"
                  value={addForm.value_secondary}
                  onChange={handleAddChange}
                  placeholder="e.g. 80"
                  className={inputClass}
                />
              </Field>
            )}

            <Field label="Unit">
              <input
                name="unit"
                type="text"
                value={addForm.unit}
                onChange={handleAddChange}
                placeholder="e.g. kg"
                className={inputClass}
              />
            </Field>

            <Field label="Notes">
              <textarea
                name="notes"
                value={addForm.notes}
                onChange={handleAddChange}
                placeholder="Optional notes"
                rows={3}
                className={inputClass}
              />
            </Field>

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
                {addSaving ? 'Saving...' : 'Log Reading'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default Vitals;
