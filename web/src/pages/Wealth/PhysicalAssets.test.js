import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PhysicalAssets } from './PhysicalAssets';

jest.mock('../../api/profiles', () => ({
  listProfiles: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  listPhysicalAssets: jest.fn(),
  createPhysicalAsset: jest.fn(),
  updatePhysicalAsset: jest.fn(),
  deactivatePhysicalAsset: jest.fn(),
}));

const { listProfiles } = require('../../api/profiles');
const { listPhysicalAssets, createPhysicalAsset } = require('../../api/wealth');

const MOCK_PROFILES = [
  { profile_id: 'p1', full_name: 'Alice', is_active: true },
  { profile_id: 'p2', full_name: 'Bob', is_active: true },
];

const MOCK_ASSETS = [
  {
    asset_id: 'asset1',
    asset_name: 'Family Car',
    asset_type: 'VEHICLE',
    make: 'Maruti',
    model: 'Swift',
    registration_number: 'KA-01-AB-1234',
    registration_type: 'PRIVATE',
    is_active: true,
    metadata: { puc_expiry: '2026-12-31' },
  },
  {
    asset_id: 'asset2',
    asset_name: 'Delivery Van',
    asset_type: 'VEHICLE',
    make: 'Tata',
    model: 'Ace',
    registration_number: 'KA-01-CD-5678',
    registration_type: 'COMMERCIAL',
    is_active: false,
    metadata: {},
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listPhysicalAssets.mockResolvedValue({ physical_assets: [] });
});

describe('PhysicalAssets page', () => {
  it('renders "Select a profile" message when no profile is selected', async () => {
    render(<PhysicalAssets />);
    await waitFor(() => {
      expect(screen.getByText(/Select a profile to view assets/i)).toBeInTheDocument();
    });
  });

  it('shows loading state during fetch', async () => {
    listPhysicalAssets.mockImplementation(
      () =>
        new Promise((resolve) => setTimeout(() => resolve({ physical_assets: MOCK_ASSETS }), 200))
    );
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));
    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });
    expect(screen.getByText(/Loading assets/i)).toBeInTheDocument();
  });

  it('shows assets after profile selection', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Family Car')).toBeInTheDocument();
      expect(screen.getByText('Delivery Van')).toBeInTheDocument();
    });
  });

  it('opens add asset modal on button click', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('+ Add Asset'));
    fireEvent.click(screen.getByText('+ Add Asset'));

    expect(screen.getByRole('heading', { name: 'Add Physical Asset' })).toBeInTheDocument();
  });

  it('shows error message on API failure', async () => {
    listPhysicalAssets.mockRejectedValue(new Error('Network error'));
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('submits create form and reloads assets', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
    createPhysicalAsset.mockResolvedValue({});
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => screen.getByText('+ Add Asset'));
    fireEvent.click(screen.getByText('+ Add Asset'));

    fireEvent.change(screen.getByPlaceholderText(/e\.g\. Family Car/i), {
      target: { name: 'asset_name', value: 'Test Car' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. Maruti/i), {
      target: { name: 'make', value: 'Test Make' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. Swift/i), {
      target: { name: 'model', value: 'Test Model' },
    });
    fireEvent.change(screen.getByPlaceholderText(/e\.g\. KA-01-AB-1234/i), {
      target: { name: 'registration_number', value: 'KA-09-ZZ-0001' },
    });

    const selects = screen.getAllByRole('combobox');
    const regTypeSelect = selects.find((s) => s.getAttribute('name') === 'registration_type');
    fireEvent.change(regTypeSelect, { target: { value: 'PRIVATE' } });

    const submitBtn = screen
      .getAllByRole('button', { name: /Add Asset/i })
      .find((b) => b.type === 'submit');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createPhysicalAsset).toHaveBeenCalled();
    });
  });
});
