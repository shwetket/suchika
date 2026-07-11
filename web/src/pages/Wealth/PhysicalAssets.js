import React, { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import {
  createPhysicalAsset,
  deactivatePhysicalAsset,
  listPhysicalAssets,
  updatePhysicalAsset,
} from '../../api/wealth';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';
import { Badge } from '../../components/shared/Badge';
import { EditIcon } from '../../components/shared/EditIcon';
import { useAuth } from '../../hooks/useAuth';

const ASSET_TYPES = ['VEHICLE'];
const REGISTRATION_TYPES = ['PRIVATE', 'COMMERCIAL', 'GOVERNMENT', 'BH_SERIES'];

const EMPTY_ADD = {
  asset_name: '',
  asset_type: 'VEHICLE',
  make: '',
  model: '',
  registration_number: '',
  registration_type: '',
};
const EMPTY_EDIT = {
  asset_name: '',
  make: '',
  model: '',
  puc_expiry: '',
  insurance_expiry: '',
  road_tax_expiry: '',
};

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

const PAGE_SIZE = 20;

function StatusBadge({ active }) {
  return (
    <Badge variant={active ? 'success' : 'neutral'}>
      {active ? 'Active' : 'Inactive'}
    </Badge>
  );
}
StatusBadge.propTypes = { active: PropTypes.bool.isRequired };

function expiryStatus(dateStr) {
  if (!dateStr) return null;
  const days = Math.ceil((new Date(dateStr).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  if (days < 0) return { label: 'Expired', cls: 'text-red-600' };
  if (days <= 30) return { label: `Due in ${days}d`, cls: 'text-amber-600' };
  return { label: dateStr, cls: 'text-gray-600' };
}

function ComplianceRow({ label, value }) {
  const status = expiryStatus(value);
  if (!status) return null;
  return (
    <p className="text-sm">
      {label}: <span className={status.cls}>{status.label}</span>
    </p>
  );
}
ComplianceRow.propTypes = { label: PropTypes.string.isRequired, value: PropTypes.string };
ComplianceRow.defaultProps = { value: null };

function AssetCard({ asset, onEdit }) {
  const metadata = asset.metadata || {};
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-5 flex flex-col gap-2 card-hover">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900 text-lg">{asset.asset_name}</h3>
          <p className="text-sm text-gray-500">
            {asset.make} {asset.model}
          </p>
        </div>
        <StatusBadge active={asset.is_active} />
      </div>
      <span className="self-start px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
        {asset.registration_number}
      </span>
      <div className="text-sm text-gray-600 space-y-1">
        {asset.registration_type && (
          <p>Registration: {asset.registration_type.replaceAll('_', ' ')}</p>
        )}
        {asset.current_value !== null && asset.current_value !== undefined && (
          <p>
            Current Value: {Number(asset.current_value).toLocaleString('en-IN')}
            {asset.valuation_date ? ` (as of ${asset.valuation_date})` : ''}
          </p>
        )}
        <ComplianceRow label="PUC" value={metadata.puc_expiry} />
        <ComplianceRow label="Insurance" value={metadata.insurance_expiry} />
        <ComplianceRow label="Road Tax" value={metadata.road_tax_expiry} />
      </div>
      <div className="flex gap-2 pt-2">
        <button
          type="button"
          onClick={() => onEdit(asset)}
          className="flex-1 border border-blue-600 text-blue-600 py-1.5 rounded text-sm font-medium hover:bg-blue-50"
        >
          Edit
        </button>
      </div>
    </div>
  );
}
AssetCard.propTypes = {
  asset: PropTypes.shape({
    asset_id: PropTypes.string.isRequired,
    asset_name: PropTypes.string.isRequired,
    // make/model/registration_number/registration_type are VEHICLE-only fields —
    // genuinely null for REAL_ESTATE/GOLD_JEWELRY/GOLD_BOND assets (v1.0 net-worth-model pass).
    make: PropTypes.string,
    model: PropTypes.string,
    registration_number: PropTypes.string,
    registration_type: PropTypes.string,
    current_value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    valuation_date: PropTypes.string,
    is_active: PropTypes.bool.isRequired,
    metadata: PropTypes.object,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
};

export const PhysicalAssets = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [assets, setAssets] = useState([]);
  const [totalSize, setTotalSize] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeFilter, setActiveFilter] = useState(null);

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_ADD);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingAsset, setEditingAsset] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_EDIT);
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);
  const [reactivating, setReactivating] = useState(false);

  const { user } = useAuth();

  useEffect(() => {
    listProfiles(user?.admin_id, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, [user?.admin_id]);

  // Stale-response guard (UX-015): loadAssets() runs on both the effect below and
  // on manual reload after add/edit/deactivate. An older in-flight request
  // resolving after a newer one must never clobber state with outdated data —
  // same intent as Accounts.js's `cancelled` flag on its balance-loading effect,
  // implemented with a request-id ref since this call site is also invoked manually.
  const loadRequestIdRef = useRef(0);

  const loadAssets = useCallback(async () => {
    if (!selectedProfileId) return;
    const requestId = loadRequestIdRef.current + 1;
    loadRequestIdRef.current = requestId;
    setLoading(true);
    setError(null);
    try {
      const data = await listPhysicalAssets(selectedProfileId, null, activeFilter, page, PAGE_SIZE);
      if (requestId !== loadRequestIdRef.current) return;
      setAssets(data.physical_assets ?? []);
      setTotalSize(data.total_size ?? (data.physical_assets ?? []).length);
    } catch (err) {
      if (requestId !== loadRequestIdRef.current) return;
      setError(err.message || 'Failed to load physical assets');
    } finally {
      if (requestId === loadRequestIdRef.current) setLoading(false);
    }
  }, [selectedProfileId, activeFilter, page]);

  useEffect(() => {
    loadAssets();
  }, [loadAssets]);

  // Reset to page 0 whenever the filters (not the page itself) change.
  useEffect(() => {
    setPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedProfileId, activeFilter]);

  const totalPages = Math.max(1, Math.ceil(totalSize / PAGE_SIZE));

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.asset_name.trim()) {
        setAddError('Asset name is required');
        return;
      }
      if (!addForm.make.trim()) {
        setAddError('Make is required');
        return;
      }
      if (!addForm.model.trim()) {
        setAddError('Model is required');
        return;
      }
      if (!addForm.registration_number.trim()) {
        setAddError('Registration number is required');
        return;
      }
      if (!addForm.registration_type) {
        setAddError('Registration type is required');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await createPhysicalAsset(selectedProfileId, {
          asset_name: addForm.asset_name.trim(),
          asset_type: addForm.asset_type,
          make: addForm.make.trim(),
          model: addForm.model.trim(),
          registration_number: addForm.registration_number.trim(),
          registration_type: addForm.registration_type,
        });
        setShowAdd(false);
        setAddForm(EMPTY_ADD);
        await loadAssets();
      } catch (err) {
        setAddError(err.message || 'Failed to create physical asset');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadAssets]
  );

  const handleEditOpen = useCallback((asset) => {
    const metadata = asset.metadata || {};
    setEditingAsset(asset);
    setEditForm({
      asset_name: asset.asset_name,
      make: asset.make,
      model: asset.model,
      puc_expiry: metadata.puc_expiry ?? '',
      insurance_expiry: metadata.insurance_expiry ?? '',
      road_tax_expiry: metadata.road_tax_expiry ?? '',
    });
    setEditError(null);
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
        await updatePhysicalAsset(editingAsset.asset_id, selectedProfileId, {
          asset_name: editForm.asset_name || null,
          make: editForm.make || null,
          model: editForm.model || null,
          metadata: {
            puc_expiry: editForm.puc_expiry || '',
            insurance_expiry: editForm.insurance_expiry || '',
            road_tax_expiry: editForm.road_tax_expiry || '',
          },
        });
        setEditingAsset(null);
        await loadAssets();
      } catch (err) {
        setEditError(err.message || 'Failed to update physical asset');
      } finally {
        setEditSaving(false);
      }
    },
    [editingAsset, editForm, loadAssets, selectedProfileId]
  );

  const handleDeactivate = useCallback(async () => {
    if (!confirmTarget) return;
    setDeactivating(true);
    try {
      await deactivatePhysicalAsset(confirmTarget.asset_id, selectedProfileId);
      setConfirmTarget(null);
      await loadAssets();
    } catch (err) {
      setError(err.message || 'Failed to deactivate physical asset');
      setConfirmTarget(null);
    } finally {
      setDeactivating(false);
    }
  }, [confirmTarget, loadAssets, selectedProfileId]);

  const handleDeactivateFromEdit = useCallback(() => {
    setConfirmTarget(editingAsset);
    setEditingAsset(null);
  }, [editingAsset]);

  const handleReactivate = useCallback(async () => {
    if (!editingAsset) return;
    setReactivating(true);
    setEditError(null);
    try {
      await updatePhysicalAsset(editingAsset.asset_id, selectedProfileId, { is_active: true });
      setEditingAsset(null);
      await loadAssets();
    } catch (err) {
      setEditError(err.message || 'Failed to reactivate physical asset');
    } finally {
      setReactivating(false);
    }
  }, [editingAsset, loadAssets, selectedProfileId]);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Physical Assets</h1>
          <p className="text-gray-500 mt-1">Vehicles and compliance deadlines</p>
        </div>
        {selectedProfileId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_ADD);
              setAddError(null);
              setShowAdd(true);
            }}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700"
          >
            + Add Asset
          </button>
        )}
      </div>

      <div className="mb-6">
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
      </div>

      {!selectedProfileId && (
        <div className="text-center py-16 text-gray-400">Select a profile to view assets.</div>
      )}

      {selectedProfileId && (
        <>
          <div className="flex gap-2 mb-6">
            {[
              { label: 'All', value: null },
              { label: 'Active', value: true },
              { label: 'Inactive', value: false },
            ].map(({ label, value }) => {
              const sel = activeFilter === value;
              const cls = sel
                ? 'bg-blue-600 text-white'
                : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50';
              return (
                <button
                  type="button"
                  key={label}
                  onClick={() => setActiveFilter(value)}
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
          {loading && <div className="text-center py-16 text-gray-500">Loading assets...</div>}
          {!loading && assets.length === 0 && (
            <div className="text-center py-16 text-gray-400">No physical assets found.</div>
          )}
          {!loading && assets.length > 0 && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {assets.map((a) => (
                  <AssetCard key={a.asset_id} asset={a} onEdit={handleEditOpen} />
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
        <Modal title="Add Physical Asset" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <Field label="Asset Name" required>
              <input
                name="asset_name"
                type="text"
                value={addForm.asset_name}
                onChange={handleAddChange}
                placeholder="e.g. Family Car"
                className={inputClass}
              />
            </Field>
            <Field label="Asset Type" required>
              <select
                name="asset_type"
                value={addForm.asset_type}
                onChange={handleAddChange}
                className={inputClass}
              >
                {ASSET_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Make" required>
              <input
                name="make"
                type="text"
                value={addForm.make}
                onChange={handleAddChange}
                placeholder="e.g. Maruti"
                className={inputClass}
              />
            </Field>
            <Field label="Model" required>
              <input
                name="model"
                type="text"
                value={addForm.model}
                onChange={handleAddChange}
                placeholder="e.g. Swift"
                className={inputClass}
              />
            </Field>
            <Field label="Registration Number" required>
              <input
                name="registration_number"
                type="text"
                value={addForm.registration_number}
                onChange={handleAddChange}
                placeholder="e.g. KA-01-AB-1234"
                className={inputClass}
              />
            </Field>
            <Field label="Registration Type" required>
              <select
                name="registration_type"
                value={addForm.registration_type}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select type</option>
                {REGISTRATION_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
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
                {addSaving ? 'Saving...' : 'Add Asset'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingAsset && (
        <Modal title={`Edit — ${editingAsset.asset_name}`} onClose={() => setEditingAsset(null)}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <Field label="Asset Name" required>
              <input
                name="asset_name"
                type="text"
                value={editForm.asset_name}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Make">
              <input
                name="make"
                type="text"
                value={editForm.make}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Model">
              <input
                name="model"
                type="text"
                value={editForm.model}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="PUC Expiry">
              <input
                name="puc_expiry"
                type="date"
                value={editForm.puc_expiry}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Insurance Expiry">
              <input
                name="insurance_expiry"
                type="date"
                value={editForm.insurance_expiry}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Road Tax Expiry">
              <input
                name="road_tax_expiry"
                type="date"
                value={editForm.road_tax_expiry}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <div className="border-t border-gray-200 pt-4">
              {editingAsset.is_active ? (
                <button
                  type="button"
                  onClick={handleDeactivateFromEdit}
                  className="text-sm font-medium text-red-600 hover:text-red-700 hover:underline"
                >
                  Deactivate this asset
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleReactivate}
                  disabled={reactivating}
                  className="text-sm font-medium text-blue-600 hover:text-blue-700 hover:underline disabled:opacity-50"
                >
                  {reactivating ? 'Reactivating...' : 'Reactivate asset'}
                </button>
              )}
            </div>
            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingAsset(null)}
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
            <h3 className="text-lg font-bold mb-2">Deactivate Asset</h3>
            <p className="text-gray-600 text-sm mb-6">
              Deactivate <strong>{confirmTarget.asset_name}</strong>? It can be re-activated via
              edit.
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

export default PhysicalAssets;
