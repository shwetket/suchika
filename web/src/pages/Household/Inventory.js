import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { listProfiles } from '../../api/profiles';
import {
  createInventoryItem,
  deleteInventoryItem,
  listInventoryItems,
  updateInventoryItem,
} from '../../api/household';
import { Field } from '../../components/Field';
import { Modal } from '../../components/Modal';
import { useAuth } from '../../hooks/useAuth';

const UNITS = ['KG', 'G', 'L', 'ML', 'UNITS', 'PACK', 'DOZEN', 'OTHER'];

const SOURCE_PLATFORMS = [
  'INSTAMART',
  'FLIPKART_GROCERY',
  'COUNTRY_DELIGHT',
  'AMAZON_FRESH',
  'BLINKIT',
  'ZEPTO',
  'MANUAL',
  'OTHER',
];

const PLATFORM_COLOURS = {
  INSTAMART: 'bg-orange-100 text-orange-800',
  FLIPKART_GROCERY: 'bg-blue-100 text-blue-800',
  COUNTRY_DELIGHT: 'bg-green-100 text-green-800',
  AMAZON_FRESH: 'bg-teal-100 text-teal-800',
  BLINKIT: 'bg-yellow-100 text-yellow-800',
  ZEPTO: 'bg-purple-100 text-purple-800',
  MANUAL: 'bg-gray-100 text-gray-700',
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

function formatPlatform(platform) {
  if (!platform) return '';
  return platform
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

const EMPTY_FORM = {
  item_name: '',
  quantity: '',
  unit: '',
  source_platform: '',
  purchase_date: todayISO(),
  category: '',
};

function toEditForm(item) {
  return {
    item_name: item.item_name,
    quantity: String(item.quantity),
    unit: item.unit,
    source_platform: item.source_platform,
    purchase_date: item.purchase_date,
    category: item.category || '',
  };
}

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full';

const PAGE_SIZE = 20;

function ItemRow({ item, onEdit, onDelete, onToggleConsumed }) {
  const [confirmDelete, setConfirmDelete] = useState(false);
  const colour = PLATFORM_COLOURS[item.source_platform] || 'bg-gray-100 text-gray-700';

  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="px-4 py-3 text-sm text-gray-900 font-medium">{item.item_name}</td>
      <td className="px-4 py-3 text-sm text-gray-700">
        {item.quantity} {item.unit}
      </td>
      <td className="px-4 py-3">
        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colour}`}>
          {formatPlatform(item.source_platform)}
        </span>
      </td>
      <td className="px-4 py-3 text-sm text-gray-600 whitespace-nowrap">
        {formatDate(item.purchase_date)}
      </td>
      <td className="px-4 py-3 text-sm text-gray-500">{item.category || '—'}</td>
      <td className="px-4 py-3 text-sm">
        <label className="inline-flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={!!item.is_consumed}
            onChange={(e) => onToggleConsumed(item, e.target.checked)}
            className="h-4 w-4 text-indigo-600 rounded border-gray-300 focus:ring-indigo-500"
          />
          <span className={item.is_consumed ? 'text-green-700' : 'text-gray-400'}>
            {item.is_consumed ? 'Consumed' : 'Not consumed'}
          </span>
        </label>
      </td>
      <td className="px-4 py-3 text-sm">
        {confirmDelete ? (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => onDelete(item.id)}
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
              onClick={() => onEdit(item)}
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

ItemRow.propTypes = {
  item: PropTypes.shape({
    id: PropTypes.string.isRequired,
    item_name: PropTypes.string.isRequired,
    quantity: PropTypes.number.isRequired,
    unit: PropTypes.string.isRequired,
    source_platform: PropTypes.string.isRequired,
    purchase_date: PropTypes.string.isRequired,
    category: PropTypes.string,
    is_consumed: PropTypes.bool,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onToggleConsumed: PropTypes.func.isRequired,
};

export const Inventory = () => {
  const [profiles, setProfiles] = useState([]);
  const [selectedProfileId, setSelectedProfileId] = useState('');
  const [items, setItems] = useState([]);
  const [totalSize, setTotalSize] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [platformFilter, setPlatformFilter] = useState('');

  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState(EMPTY_FORM);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

  const [editingItem, setEditingItem] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_FORM);
  const [editError, setEditError] = useState(null);
  const [editSaving, setEditSaving] = useState(false);

  const { user } = useAuth();

  useEffect(() => {
    listProfiles(user?.admin_id, null)
      .then((data) => setProfiles(data.profiles ?? []))
      .catch(() => setProfiles([]));
  }, [user?.admin_id]);

  const loadItems = useCallback(async () => {
    if (!selectedProfileId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listInventoryItems(
        selectedProfileId,
        platformFilter || null,
        page,
        PAGE_SIZE
      );
      const list = data.inventory_items ?? data.items ?? [];
      setItems(list);
      setTotalSize(data.total_size ?? list.length);
    } catch (err) {
      setError(err.message || 'Failed to load inventory');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId, platformFilter, page]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  // Reset to page 0 whenever the filters (not the page itself) change.
  useEffect(() => {
    setPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedProfileId, platformFilter]);

  const totalPages = Math.max(1, Math.ceil(totalSize / PAGE_SIZE));

  const handleAddChange = useCallback((e) => {
    const { name, value } = e.target;
    setAddForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!addForm.item_name.trim()) {
        setAddError('Item name is required');
        return;
      }
      if (!addForm.quantity || Number(addForm.quantity) <= 0) {
        setAddError('Quantity must be greater than 0');
        return;
      }
      if (!addForm.unit) {
        setAddError('Unit is required');
        return;
      }
      if (!addForm.source_platform) {
        setAddError('Source platform is required');
        return;
      }
      if (!addForm.purchase_date) {
        setAddError('Purchase date is required');
        return;
      }
      setAddSaving(true);
      setAddError(null);
      try {
        await createInventoryItem({
          profile_id: selectedProfileId,
          item_name: addForm.item_name.trim(),
          quantity: Number(addForm.quantity),
          unit: addForm.unit,
          source_platform: addForm.source_platform,
          purchase_date: addForm.purchase_date,
          category: addForm.category || null,
        });
        setShowAdd(false);
        setAddForm(EMPTY_FORM);
        await loadItems();
      } catch (err) {
        setAddError(err.message || 'Failed to add item');
      } finally {
        setAddSaving(false);
      }
    },
    [addForm, selectedProfileId, loadItems]
  );

  const handleDelete = useCallback(
    async (id) => {
      try {
        await deleteInventoryItem(id);
        await loadItems();
      } catch (err) {
        setError(err.message || 'Failed to delete item');
      }
    },
    [loadItems]
  );

  const handleEditOpen = useCallback((item) => {
    setEditingItem(item);
    setEditForm(toEditForm(item));
    setEditError(null);
  }, []);

  const handleEditChange = useCallback((e) => {
    const { name, value } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleEditSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!editForm.item_name.trim()) {
        setEditError('Item name is required');
        return;
      }
      if (!editForm.quantity || Number(editForm.quantity) <= 0) {
        setEditError('Quantity must be greater than 0');
        return;
      }
      if (!editForm.unit) {
        setEditError('Unit is required');
        return;
      }
      if (!editForm.source_platform) {
        setEditError('Source platform is required');
        return;
      }
      if (!editForm.purchase_date) {
        setEditError('Purchase date is required');
        return;
      }
      setEditSaving(true);
      setEditError(null);
      try {
        await updateInventoryItem(editingItem.id, {
          item_name: editForm.item_name.trim(),
          quantity: Number(editForm.quantity),
          unit: editForm.unit,
          source_platform: editForm.source_platform,
          purchase_date: editForm.purchase_date,
          category: editForm.category || null,
        });
        setEditingItem(null);
        await loadItems();
      } catch (err) {
        setEditError(err.message || 'Failed to update item');
      } finally {
        setEditSaving(false);
      }
    },
    [editForm, editingItem, loadItems]
  );

  const handleToggleConsumed = useCallback(
    async (item, isConsumed) => {
      try {
        await updateInventoryItem(item.id, { is_consumed: isConsumed });
        await loadItems();
      } catch (err) {
        setError(err.message || 'Failed to update item');
      }
    },
    [loadItems]
  );

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Household Inventory</h1>
          <p className="text-gray-500 mt-1">Track grocery and supply inventory</p>
        </div>
        {selectedProfileId && (
          <button
            type="button"
            onClick={() => {
              setAddForm(EMPTY_FORM);
              setAddError(null);
              setShowAdd(true);
            }}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-indigo-700"
          >
            + Add Item
          </button>
        )}
      </div>

      <div className="mb-6 flex flex-col sm:flex-row gap-3">
        <select
          value={selectedProfileId}
          onChange={(e) => setSelectedProfileId(e.target.value)}
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
            value={platformFilter}
            onChange={(e) => setPlatformFilter(e.target.value)}
            className={`${inputClass} sm:w-60`}
          >
            <option value="">All Platforms</option>
            {SOURCE_PLATFORMS.map((p) => (
              <option key={p} value={p}>
                {formatPlatform(p)}
              </option>
            ))}
          </select>
        )}
      </div>

      {!selectedProfileId && (
        <div className="text-center py-16 text-gray-400">Select a profile to view inventory.</div>
      )}

      {selectedProfileId && (
        <>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading && <div className="text-center py-16 text-gray-500">Loading inventory...</div>}

          {!loading && items.length === 0 && (
            <div className="text-center py-16 text-gray-400">
              No items yet. Add your first item above.
            </div>
          )}

          {!loading && items.length > 0 && (
            <div className="overflow-x-auto rounded-lg border border-gray-200">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    {[
                      'Item',
                      'Quantity',
                      'Platform',
                      'Purchased',
                      'Category',
                      'Consumed',
                      'Actions',
                    ].map((h) => (
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
                  {items.map((item) => (
                    <ItemRow
                      key={item.id}
                      item={item}
                      onEdit={handleEditOpen}
                      onDelete={handleDelete}
                      onToggleConsumed={handleToggleConsumed}
                    />
                  ))}
                </tbody>
              </table>

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
            </div>
          )}
        </>
      )}

      {showAdd && (
        <Modal title="Add Item" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAddSubmit} className="space-y-4">
            <Field label="Item Name" required>
              <input
                name="item_name"
                type="text"
                value={addForm.item_name}
                onChange={handleAddChange}
                placeholder="e.g. Milk"
                className={inputClass}
              />
            </Field>
            <Field label="Quantity" required>
              <input
                name="quantity"
                type="number"
                min="0.001"
                step="any"
                value={addForm.quantity}
                onChange={handleAddChange}
                placeholder="e.g. 2"
                className={inputClass}
              />
            </Field>
            <Field label="Unit" required>
              <select
                name="unit"
                value={addForm.unit}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select unit</option>
                {UNITS.map((u) => (
                  <option key={u} value={u}>
                    {u}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Source Platform" required>
              <select
                name="source_platform"
                value={addForm.source_platform}
                onChange={handleAddChange}
                className={inputClass}
              >
                <option value="">Select platform</option>
                {SOURCE_PLATFORMS.map((p) => (
                  <option key={p} value={p}>
                    {formatPlatform(p)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Purchase Date" required>
              <input
                name="purchase_date"
                type="date"
                value={addForm.purchase_date}
                onChange={handleAddChange}
                className={inputClass}
              />
            </Field>
            <Field label="Category">
              <input
                name="category"
                type="text"
                value={addForm.category}
                onChange={handleAddChange}
                placeholder="Optional (e.g. Dairy)"
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
                className="px-4 py-2 bg-indigo-600 text-white rounded text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {addSaving ? 'Saving...' : 'Add Item'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {editingItem && (
        <Modal title="Edit Item" onClose={() => setEditingItem(null)}>
          <form onSubmit={handleEditSubmit} className="space-y-4">
            <Field label="Item Name" required>
              <input
                name="item_name"
                type="text"
                value={editForm.item_name}
                onChange={handleEditChange}
                placeholder="e.g. Milk"
                className={inputClass}
              />
            </Field>
            <Field label="Quantity" required>
              <input
                name="quantity"
                type="number"
                min="0.001"
                step="any"
                value={editForm.quantity}
                onChange={handleEditChange}
                placeholder="e.g. 2"
                className={inputClass}
              />
            </Field>
            <Field label="Unit" required>
              <select
                name="unit"
                value={editForm.unit}
                onChange={handleEditChange}
                className={inputClass}
              >
                <option value="">Select unit</option>
                {UNITS.map((u) => (
                  <option key={u} value={u}>
                    {u}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Source Platform" required>
              <select
                name="source_platform"
                value={editForm.source_platform}
                onChange={handleEditChange}
                className={inputClass}
              >
                <option value="">Select platform</option>
                {SOURCE_PLATFORMS.map((p) => (
                  <option key={p} value={p}>
                    {formatPlatform(p)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Purchase Date" required>
              <input
                name="purchase_date"
                type="date"
                value={editForm.purchase_date}
                onChange={handleEditChange}
                className={inputClass}
              />
            </Field>
            <Field label="Category">
              <input
                name="category"
                type="text"
                value={editForm.category}
                onChange={handleEditChange}
                placeholder="Optional (e.g. Dairy)"
                className={inputClass}
              />
            </Field>
            {editError && <p className="text-red-600 text-sm">{editError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEditingItem(null)}
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

export default Inventory;
