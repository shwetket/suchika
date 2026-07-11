import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PhysicalAssets } from './PhysicalAssets';

const mockUseAuth = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth(),
}));

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
const {
  listPhysicalAssets,
  createPhysicalAsset,
  updatePhysicalAsset,
} = require('../../api/wealth');

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
  mockUseAuth.mockReturnValue({ user: { admin_id: 'admin-1' } });
  listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
  listPhysicalAssets.mockResolvedValue({ physical_assets: [] });
});

async function selectProfileAndWaitForAssets() {
  render(<PhysicalAssets />);
  await waitFor(() => screen.getByText('Alice'));
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
  await waitFor(() => screen.getByText('Family Car'));
}

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

  it('requests page 0 and the default page size on first load', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS, total_size: 2 });
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(listPhysicalAssets).toHaveBeenCalledWith('p1', null, null, 0, 20);
    });
  });

  it('shows pagination controls with page count derived from total_size', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS, total_size: 45 });
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 3 \(45 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
  });

  it('clicking Next requests the next page', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS, total_size: 45 });
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });
    await waitFor(() => screen.getByRole('button', { name: 'Next' }));

    fireEvent.click(screen.getByRole('button', { name: 'Next' }));

    await waitFor(() => {
      expect(listPhysicalAssets).toHaveBeenCalledWith('p1', null, null, 1, 20);
      expect(screen.getByText(/Page 2 of 3/i)).toBeInTheDocument();
    });
  });

  it('disables Next on the last page', async () => {
    listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS, total_size: 2 });
    render(<PhysicalAssets />);
    await waitFor(() => screen.getByText('Alice'));

    const profileSelect = screen.getByRole('combobox');
    fireEvent.change(profileSelect, { target: { value: 'p1' } });

    await waitFor(() => {
      expect(screen.getByText(/Page 1 of 1 \(2 total\)/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  describe('Load stale-response guard (UX-015)', () => {
    it('does not let a stale (late-resolving) response overwrite a newer one', async () => {
      let resolveStale;
      const stalePromise = new Promise((resolve) => {
        resolveStale = resolve;
      });

      listPhysicalAssets
        .mockImplementationOnce(() => stalePromise) // initial load (All filter) — resolves late
        .mockImplementationOnce(() =>
          Promise.resolve({
            physical_assets: [
              {
                asset_id: 'fresh-1',
                asset_name: 'Fresh Asset',
                asset_type: 'VEHICLE',
                make: 'Honda',
                model: 'City',
                registration_number: 'KA-05-XY-9999',
                registration_type: 'PRIVATE',
                is_active: true,
                metadata: {},
              },
            ],
            total_size: 1,
          })
        );

      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      // Trigger a second, newer request before the first (stale) one resolves.
      await waitFor(() => screen.getByRole('button', { name: 'Active' }));
      fireEvent.click(screen.getByRole('button', { name: 'Active' }));

      await waitFor(() => screen.getByText('Fresh Asset'));

      // Now let the stale first request resolve with older, different data.
      resolveStale({
        physical_assets: [MOCK_ASSETS[0]],
        total_size: 1,
      });

      await new Promise((resolve) => {
        setTimeout(resolve, 0);
      });

      expect(screen.getByText('Fresh Asset')).toBeInTheDocument();
      expect(screen.queryByText('Family Car')).not.toBeInTheDocument();
    });
  });

  describe('Deactivate relocated to Edit modal (UX-006, UX-007)', () => {
    it('does not render a Deactivate control directly on the asset card', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      expect(screen.queryByRole('button', { name: /^Deactivate$/i })).not.toBeInTheDocument();
      expect(screen.queryByText(/Deactivate this asset/i)).not.toBeInTheDocument();
    });

    it('shows "Deactivate this asset" link inside the Edit modal for an active asset', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]); // Family Car, is_active: true

      await waitFor(() => {
        expect(screen.getByText('Deactivate this asset')).toBeInTheDocument();
      });
      expect(screen.queryByText('Reactivate asset')).not.toBeInTheDocument();
    });

    it('clicking the Deactivate link closes the edit modal and opens the confirm dialog', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);

      await waitFor(() => screen.getByText('Deactivate this asset'));
      fireEvent.click(screen.getByText('Deactivate this asset'));

      expect(screen.queryByRole('heading', { name: /Edit —/i })).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Deactivate Asset' })).toBeInTheDocument();
    });

    it('shows "Reactivate asset" (no confirmation) inside the Edit modal for an inactive asset, mutually exclusive with Deactivate', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[1]); // Delivery Van, is_active: false

      await waitFor(() => {
        expect(screen.getByText('Reactivate asset')).toBeInTheDocument();
      });
      expect(screen.queryByText('Deactivate this asset')).not.toBeInTheDocument();
    });

    it('clicking Reactivate calls updatePhysicalAsset with is_active true and requires no confirmation dialog', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      updatePhysicalAsset.mockResolvedValue({});
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[1]);

      await waitFor(() => screen.getByText('Reactivate asset'));
      fireEvent.click(screen.getByText('Reactivate asset'));

      await waitFor(() => {
        expect(updatePhysicalAsset).toHaveBeenCalledWith('asset2', 'p1', { is_active: true });
      });
      expect(screen.queryByRole('heading', { name: 'Deactivate Asset' })).not.toBeInTheDocument();
    });

    it('does not render a raw Active checkbox in the Edit modal', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);

      await waitFor(() => screen.getByText('Deactivate this asset'));
      expect(screen.queryByLabelText('Active')).not.toBeInTheDocument();
    });
  });
});
