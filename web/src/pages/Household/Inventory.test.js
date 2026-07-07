import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Inventory } from './Inventory';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/household', () => ({
  listInventoryItems: jest.fn(),
  createInventoryItem: jest.fn(),
  updateInventoryItem: jest.fn(),
  deleteInventoryItem: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listInventoryItems,
  createInventoryItem,
  updateInventoryItem,
  deleteInventoryItem,
} = require('../../api/household');

const MOCK_PROFILES = [{ profile_id: 'p1', full_name: 'Alice', is_active: true }];

const MOCK_ITEMS = [
  {
    id: 'i1',
    profile_id: 'p1',
    item_name: 'Milk',
    quantity: 2,
    unit: 'L',
    source_platform: 'BLINKIT',
    purchase_date: '2026-06-20',
    category: 'Dairy',
    is_consumed: false,
  },
  {
    id: 'i2',
    profile_id: 'p1',
    item_name: 'Rice',
    quantity: 5,
    unit: 'KG',
    source_platform: 'AMAZON_FRESH',
    purchase_date: '2026-06-18',
    category: null,
    is_consumed: true,
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listInventoryItems.mockResolvedValue({ inventory_items: [] });
});

describe('Inventory page', () => {
  it('renders select profile prompt when no profile selected', async () => {
    render(<Inventory />);
    await waitFor(() => {
      expect(screen.getByText(/select a profile to view inventory/i)).toBeInTheDocument();
    });
  });

  it('renders inventory list after profile selection', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Milk')).toBeInTheDocument();
      expect(screen.getByText('Rice')).toBeInTheDocument();
    });
  });

  it('"Add Item" button opens the add form modal', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));

    fireEvent.click(screen.getByText('+ Add Item'));
    expect(screen.getByRole('heading', { name: /add item/i })).toBeInTheDocument();
  });

  it('submits add item form and reloads list', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    createInventoryItem.mockResolvedValue({ id: 'i3', item_name: 'Eggs' });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));

    fireEvent.click(screen.getByText('+ Add Item'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Milk'), {
      target: { name: 'item_name', value: 'Eggs' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 2'), {
      target: { name: 'quantity', value: '12' },
    });

    const selects = screen.getAllByRole('combobox');
    const unitSelect = selects.find((s) => s.getAttribute('name') === 'unit');
    fireEvent.change(unitSelect, { target: { value: 'UNITS' } });

    const platformSelect = selects.find((s) => s.getAttribute('name') === 'source_platform');
    fireEvent.change(platformSelect, { target: { value: 'MANUAL' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add item/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createInventoryItem).toHaveBeenCalledWith(
        expect.objectContaining({ item_name: 'Eggs', quantity: 12, unit: 'UNITS' })
      );
    });
  });

  it('shows inline delete confirmation and calls deleteInventoryItem', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    deleteInventoryItem.mockResolvedValue(null);
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    expect(screen.getByText('Confirm?')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(deleteInventoryItem).toHaveBeenCalledWith('i1');
    });
  });

  it('shows loading state while fetching', async () => {
    listInventoryItems.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ inventory_items: [] }), 300))
    );
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    expect(screen.getByText(/loading inventory/i)).toBeInTheDocument();
  });

  it('cancels delete confirmation when Cancel is clicked', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    expect(screen.getByText('Confirm?')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Confirm?')).not.toBeInTheDocument();
  });

  it('shows validation error when item_name is empty on add', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));
    fireEvent.click(screen.getByText('+ Add Item'));

    const submitBtn = screen
      .getAllByRole('button', { name: /add item/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/item name is required/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when quantity is zero on add', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));
    fireEvent.click(screen.getByText('+ Add Item'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Milk'), {
      target: { name: 'item_name', value: 'Eggs' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 2'), {
      target: { name: 'quantity', value: '0' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /add item/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/quantity must be greater than 0/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when unit is missing on add', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));
    fireEvent.click(screen.getByText('+ Add Item'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Milk'), {
      target: { name: 'item_name', value: 'Eggs' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 2'), {
      target: { name: 'quantity', value: '3' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /add item/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/unit is required/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when source_platform is missing on add', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));
    fireEvent.click(screen.getByText('+ Add Item'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Milk'), {
      target: { name: 'item_name', value: 'Eggs' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 2'), {
      target: { name: 'quantity', value: '3' },
    });
    const selects = screen.getAllByRole('combobox');
    const unitSelect = selects.find((s) => s.getAttribute('name') === 'unit');
    fireEvent.change(unitSelect, { target: { value: 'UNITS' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add item/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/source platform is required/i)).toBeInTheDocument();
    });
  });

  it('shows validation error when purchase_date is missing on add', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));
    fireEvent.click(screen.getByText('+ Add Item'));

    fireEvent.change(screen.getByPlaceholderText('e.g. Milk'), {
      target: { name: 'item_name', value: 'Eggs' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 2'), {
      target: { name: 'quantity', value: '3' },
    });
    const selects = screen.getAllByRole('combobox');
    const unitSelect = selects.find((s) => s.getAttribute('name') === 'unit');
    fireEvent.change(unitSelect, { target: { value: 'UNITS' } });
    const platformSelect = selects.find((s) => s.getAttribute('name') === 'source_platform');
    fireEvent.change(platformSelect, { target: { value: 'MANUAL' } });

    const dateInput = screen.getByDisplayValue(/\d{4}-\d{2}-\d{2}/);
    fireEvent.change(dateInput, { target: { name: 'purchase_date', value: '' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /add item/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/purchase date is required/i)).toBeInTheDocument();
    });
  });

  it('closes the add item modal when Cancel is clicked', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: [] });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('+ Add Item'));
    fireEvent.click(screen.getByText('+ Add Item'));

    expect(screen.getByRole('heading', { name: /add item/i })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(screen.queryByRole('heading', { name: /add item/i })).not.toBeInTheDocument();
  });

  it('shows error when listInventoryItems API fails', async () => {
    listInventoryItems.mockRejectedValue(new Error());
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/failed to load inventory/i)).toBeInTheDocument();
    });
  });

  it('shows error when deleteInventoryItem API fails', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    deleteInventoryItem.mockRejectedValue(new Error());
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Delete'));

    fireEvent.click(screen.getAllByText('Delete')[0]);
    fireEvent.click(screen.getByText('Confirm?'));

    await waitFor(() => {
      expect(screen.getByText(/failed to delete item/i)).toBeInTheDocument();
    });
  });

  it('"Edit" button opens the edit form modal pre-filled with item data', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);

    expect(screen.getByRole('heading', { name: /edit item/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue('Milk')).toBeInTheDocument();
  });

  it('submits edit item form and reloads list', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    updateInventoryItem.mockResolvedValue({ id: 'i1', item_name: 'Whole Milk' });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);

    fireEvent.change(screen.getByDisplayValue('Milk'), {
      target: { name: 'item_name', value: 'Whole Milk' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /save changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(updateInventoryItem).toHaveBeenCalledWith(
        'i1',
        expect.objectContaining({ item_name: 'Whole Milk' })
      );
    });
  });

  it('shows validation error when item_name is cleared on edit', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);

    fireEvent.change(screen.getByDisplayValue('Milk'), {
      target: { name: 'item_name', value: '' },
    });

    const submitBtn = screen
      .getAllByRole('button', { name: /save changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/item name is required/i)).toBeInTheDocument();
    });
  });

  it('closes the edit item modal when Cancel is clicked', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);
    expect(screen.getByRole('heading', { name: /edit item/i })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(screen.queryByRole('heading', { name: /edit item/i })).not.toBeInTheDocument();
  });

  it('shows error when updateInventoryItem API fails on edit', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    updateInventoryItem.mockRejectedValue(new Error());
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getAllByText('Edit'));

    fireEvent.click(screen.getAllByText('Edit')[0]);

    const submitBtn = screen
      .getAllByRole('button', { name: /save changes/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/failed to update item/i)).toBeInTheDocument();
    });
  });

  it('renders is_consumed checkbox reflecting item state', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('Milk'));

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes[0]).not.toBeChecked();
    expect(checkboxes[1]).toBeChecked();
  });

  it('toggles is_consumed and reloads list', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    updateInventoryItem.mockResolvedValue({ id: 'i1', is_consumed: true });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('Milk'));

    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);

    await waitFor(() => {
      expect(updateInventoryItem).toHaveBeenCalledWith('i1', { is_consumed: true });
    });
  });

  // ---- Q54 pagination pass ----

  it('requests page 0 and the default page size on first load', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS, total_size: 2 });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(listInventoryItems).toHaveBeenCalledWith('p1', null, 0, 20);
    });
  });

  it('shows pagination controls with page count derived from total_size', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS, total_size: 45 });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 3 \(45 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
  });

  it('clicking Next requests the next page', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS, total_size: 45 });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByRole('button', { name: 'Next' }));

    fireEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      expect(listInventoryItems).toHaveBeenCalledWith('p1', null, 1, 20);
      expect(screen.getByText(/Page 2 of 3/i)).toBeInTheDocument();
    });
  });

  it('disables Next on the last page', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS, total_size: 2 });
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 1 \(2 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('shows error when toggling is_consumed fails', async () => {
    listInventoryItems.mockResolvedValue({ inventory_items: MOCK_ITEMS });
    updateInventoryItem.mockRejectedValue(new Error());
    render(<Inventory />);
    await waitFor(() => screen.getByText('Alice'));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
    await waitFor(() => screen.getByText('Milk'));

    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);

    await waitFor(() => {
      expect(screen.getByText(/failed to update item/i)).toBeInTheDocument();
    });
  });
});
