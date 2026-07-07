import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listAdmins } from '../../api/admins';
import { createProfile, deactivateProfile, listProfiles, updateProfile } from '../../api/profiles';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';

const RELATIONS = [
  'SELF',
  'SPOUSE',
  'CHILD',
  'PARENT',
  'PARENT_IN_LAW',
  'SIBLING',
  'GRANDPARENT',
  'GRANDCHILD',
  'OTHER',
];

const GENDERS = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'];

const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

const EMPTY_ADD_FORM = {
  admin_id: '',
  full_name: '',
  dob: '',
  relation_to_admin: '',
  email_address: '',
  gender: '',
  blood_type: '',
};

const EMPTY_EDIT_FORM = {
  email_address: '',
  gender: '',
  blood_type: '',
  is_active: true,
};

function formatRelation(value) {
  if (!value) return '';
  return value.replaceAll('_', ' ');
}

function StatusBadge({ active }) {
  const classes = active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-500';
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${classes}`}>
      {active ? 'Active' : 'Inactive'}
    </span>
  );
}

StatusBadge.propTypes = { active: PropTypes.bool.isRequired };

function ProfileCard({ profile, onEdit, onDeactivate }) {
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-2">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900 text-lg">{profile.full_name}</h3>
          <p className="text-sm text-gray-500">{formatRelation(profile.relation_to_admin)}</p>
        </div>
        <StatusBadge active={profile.is_active} />
      </div>

      <div className="text-sm text-gray-600 space-y-1">
        {profile.dob && <p>DOB: {profile.dob}</p>}
        {profile.email_address && <p>Email: {profile.email_address}</p>}
        {profile.gender && <p>Gender: {profile.gender.replaceAll('_', ' ')}</p>}
        {profile.blood_type && <p>Blood Type: {profile.blood_type}</p>}
      </div>

      <div className="flex gap-2 pt-2">
        <button
          type="button"
          onClick={() => onEdit(profile)}
          className="flex-1 border border-blue-600 text-blue-600 py-1.5 rounded text-sm font-medium hover:bg-blue-50"
        >
          Edit
        </button>
        {profile.is_active && (
          <button
            type="button"
            onClick={() => onDeactivate(profile)}
            className="flex-1 border border-red-500 text-red-500 py-1.5 rounded text-sm font-medium hover:bg-red-50"
          >
            Deactivate
          </button>
        )}
      </div>
    </div>
  );
}

ProfileCard.propTypes = {
  profile: PropTypes.shape({
    profile_id: PropTypes.string.isRequired,
    full_name: PropTypes.string.isRequired,
    relation_to_admin: PropTypes.string,
    dob: PropTypes.string,
    email_address: PropTypes.string,
    gender: PropTypes.string,
    blood_type: PropTypes.string,
    is_active: PropTypes.bool.isRequired,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDeactivate: PropTypes.func.isRequired,
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

export const Profiles = () => {
  const [profiles, setProfiles] = useState([]);
  const [admins, setAdmins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filterActive, setFilterActive] = useState(null);

  const [showAddModal, setShowAddModal] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_ADD_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingProfile, setEditingProfile] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_EDIT_FORM);
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [profileData, adminData] = await Promise.all([
        listProfiles(null, filterActive),
        listAdmins(),
      ]);
      setProfiles(profileData.profiles ?? []);
      setAdmins(adminData.admins ?? []);
    } catch (err) {
      setError(err.message || 'Failed to load profiles');
    } finally {
      setLoading(false);
    }
  }, [filterActive]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleAddOpen = useCallback(() => {
    setAddForm(EMPTY_ADD_FORM);
    setAddError(null);
    setShowAddModal(true);
  }, []);

  const handleAddClose = useCallback(() => {
    setShowAddModal(false);
  }, []);

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.admin_id) {
        setAddError('Please select an admin');
        return;
      }
      if (!addForm.full_name.trim()) {
        setAddError('Full name is required');
        return;
      }
      if (!addForm.dob) {
        setAddError('Date of birth is required');
        return;
      }
      if (!addForm.relation_to_admin) {
        setAddError('Relation is required');
        return;
      }

      setAddSaving(true);
      setAddError(null);
      try {
        await createProfile({
          admin_id: addForm.admin_id,
          full_name: addForm.full_name.trim(),
          dob: addForm.dob,
          relation_to_admin: addForm.relation_to_admin,
          email_address: addForm.email_address || null,
          gender: addForm.gender || null,
          blood_type: addForm.blood_type || null,
        });
        setShowAddModal(false);
        await loadData();
      } catch (err) {
        setAddError(err.message || 'Failed to create profile');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, loadData]
  );

  const handleEditOpen = useCallback((profile) => {
    setEditingProfile(profile);
    setEditForm({
      email_address: profile.email_address ?? '',
      gender: profile.gender ?? '',
      blood_type: profile.blood_type ?? '',
      is_active: profile.is_active,
    });
    setEditError(null);
  }, []);

  const handleEditClose = useCallback(() => {
    setEditingProfile(null);
  }, []);

  const handleEditChange = useCallback((e) => {
    const { name, value, type, checked } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }, []);

  const handleEditSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      setEditSaving(true);
      setEditError(null);
      try {
        await updateProfile(editingProfile.profile_id, {
          email_address: editForm.email_address || null,
          gender: editForm.gender || null,
          blood_type: editForm.blood_type || null,
          is_active: editForm.is_active,
        });
        setEditingProfile(null);
        await loadData();
      } catch (err) {
        setEditError(err.message || 'Failed to update profile');
      } finally {
        setEditSaving(false);
      }
    },
    [editingProfile, editForm, loadData]
  );

  const handleDeactivate = useCallback(async () => {
    if (!confirmTarget) return;
    setDeactivating(true);
    try {
      await deactivateProfile(confirmTarget.profile_id);
      setConfirmTarget(null);
      await loadData();
    } catch (err) {
      setConfirmTarget(null);
      setError(err.message || 'Failed to deactivate profile');
    } finally {
      setDeactivating(false);
    }
  }, [confirmTarget, loadData]);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Household Profiles</h1>
          <p className="text-gray-500 mt-1">Manage members of your household</p>
        </div>
        <button
          type="button"
          onClick={handleAddOpen}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700"
        >
          + Add Profile
        </button>
      </div>

      <div className="flex gap-2 mb-6">
        {[
          { label: 'All', value: null },
          { label: 'Active', value: true },
          { label: 'Inactive', value: false },
        ].map(({ label, value }) => {
          const isSelected = filterActive === value;
          const cls = isSelected
            ? 'bg-blue-600 text-white'
            : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50';
          return (
            <button
              key={label}
              type="button"
              onClick={() => setFilterActive(value)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium ${cls}`}
            >
              {label}
            </button>
          );
        })}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      {loading && <div className="text-center py-16 text-gray-500">Loading profiles...</div>}

      {!loading && profiles.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          No profiles found.{' '}
          <button type="button" onClick={handleAddOpen} className="text-blue-600 hover:underline">
            Add the first one.
          </button>
        </div>
      )}

      {!loading && profiles.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {profiles.map((p) => (
            <ProfileCard
              key={p.profile_id}
              profile={p}
              onEdit={handleEditOpen}
              onDeactivate={setConfirmTarget}
            />
          ))}
        </div>
      )}

      {showAddModal && (
        <Modal title="Add Profile" onClose={handleAddClose}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <Field label="Admin" required>
              <select
                name="admin_id"
                value={addForm.admin_id}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select admin</option>
                {admins.map((a) => (
                  <option key={a.admin_id} value={a.admin_id}>
                    {a.display_name}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Full Name" required>
              <input
                name="full_name"
                type="text"
                value={addForm.full_name}
                onChange={handleAddChange}
                placeholder="Full name"
                className={inputClass}
              />
            </Field>

            <Field label="Date of Birth" required>
              <input
                name="dob"
                type="date"
                value={addForm.dob}
                onChange={handleAddChange}
                className={inputClass}
              />
            </Field>

            <Field label="Relation to Admin" required>
              <select
                name="relation_to_admin"
                value={addForm.relation_to_admin}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select relation</option>
                {RELATIONS.map((r) => (
                  <option key={r} value={r}>
                    {formatRelation(r)}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Email Address">
              <input
                name="email_address"
                type="email"
                value={addForm.email_address}
                onChange={handleAddChange}
                placeholder="Optional"
                className={inputClass}
              />
            </Field>

            <Field label="Gender">
              <select
                name="gender"
                value={addForm.gender}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select gender</option>
                {GENDERS.map((g) => (
                  <option key={g} value={g}>
                    {g.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Blood Type">
              <select
                name="blood_type"
                value={addForm.blood_type}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select blood type</option>
                {BLOOD_TYPES.map((bt) => (
                  <option key={bt} value={bt}>
                    {bt}
                  </option>
                ))}
              </select>
            </Field>

            {addError && <p className="text-red-600 text-sm">{addError}</p>}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={handleAddClose}
                className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={addSaving}
                className="px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {addSaving ? 'Saving...' : 'Add Profile'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingProfile && (
        <Modal title={`Edit — ${editingProfile.full_name}`} onClose={handleEditClose}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <Field label="Email Address">
              <input
                name="email_address"
                type="email"
                value={editForm.email_address}
                onChange={handleEditChange}
                placeholder="Optional"
                className={inputClass}
              />
            </Field>

            <Field label="Gender">
              <select
                name="gender"
                value={editForm.gender}
                onChange={handleEditChange}
                className={inputClass}
              >
                <option value="">Select gender</option>
                {GENDERS.map((g) => (
                  <option key={g} value={g}>
                    {g.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Blood Type">
              <select
                name="blood_type"
                value={editForm.blood_type}
                onChange={handleEditChange}
                className={inputClass}
              >
                <option value="">Select blood type</option>
                {BLOOD_TYPES.map((bt) => (
                  <option key={bt} value={bt}>
                    {bt}
                  </option>
                ))}
              </select>
            </Field>

            <div className="flex items-center gap-3">
              <input
                id="edit-is-active"
                name="is_active"
                type="checkbox"
                checked={editForm.is_active}
                onChange={handleEditChange}
                className="w-4 h-4"
              />
              <label htmlFor="edit-is-active" className="text-sm font-medium text-gray-700">
                Active
              </label>
            </div>

            {editError && <p className="text-red-600 text-sm">{editError}</p>}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={handleEditClose}
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
            <h3 className="text-lg font-bold mb-2">Deactivate Profile</h3>
            <p className="text-gray-600 text-sm mb-6">
              Deactivate <strong>{confirmTarget.full_name}</strong>? They can be re-activated later
              via edit.
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
                onClick={handleDeactivate}
                disabled={deactivating}
                className="px-4 py-2 bg-red-600 text-white rounded text-sm font-medium hover:bg-red-700 disabled:opacity-50"
              >
                {deactivating ? 'Deactivating...' : 'Deactivate'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profiles;
