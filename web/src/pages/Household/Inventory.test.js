import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Inventory } from './Inventory';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/household', () => ({
  listInventoryItems: jest.fn(),
  createInventoryItem: jest.fn(),
  deleteInventoryItem: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const {
  listInventoryItems,
  createInventoryItem,
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
});
