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

  describe('Asset-type-conditional form fields', () => {
    async function openAddModal() {
      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
      await waitFor(() => screen.getByText('+ Add Asset'));
      fireEvent.click(screen.getByText('+ Add Asset'));
    }

    function assetTypeSelect() {
      return screen.getAllByRole('combobox').find((s) => s.getAttribute('name') === 'asset_type');
    }

    it('shows VEHICLE-only fields (make/model/registration) by default in the Add form', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: [] });
      await openAddModal();

      expect(screen.getByText('Make')).toBeInTheDocument();
      expect(screen.getByText('Model')).toBeInTheDocument();
      expect(screen.getByText('Registration Number')).toBeInTheDocument();
      expect(screen.getByText('Registration Type')).toBeInTheDocument();
      expect(screen.getByText('Current Value')).toBeInTheDocument();
      expect(screen.getByText('Valuation Date')).toBeInTheDocument();
    });

    it.each(['REAL_ESTATE', 'GOLD_JEWELRY', 'GOLD_BOND'])(
      'hides VEHICLE-only fields and keeps current_value/valuation_date when asset_type is %s',
      async (nonVehicleType) => {
        listPhysicalAssets.mockResolvedValue({ physical_assets: [] });
        await openAddModal();

        fireEvent.change(assetTypeSelect(), { target: { value: nonVehicleType } });

        expect(screen.queryByText('Make')).not.toBeInTheDocument();
        expect(screen.queryByText('Model')).not.toBeInTheDocument();
        expect(screen.queryByText('Registration Number')).not.toBeInTheDocument();
        expect(screen.queryByText('Registration Type')).not.toBeInTheDocument();
        expect(screen.getByText('Current Value')).toBeInTheDocument();
        expect(screen.getByText('Valuation Date')).toBeInTheDocument();
      }
    );

    it('submits a non-VEHICLE asset with current_value/valuation_date and null vehicle-only fields, without requiring make/model/registration', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: [] });
      createPhysicalAsset.mockResolvedValue({});
      await openAddModal();

      fireEvent.change(screen.getByPlaceholderText(/e\.g\. Family Car/i), {
        target: { name: 'asset_name', value: 'Bangalore Flat' },
      });
      fireEvent.change(assetTypeSelect(), { target: { value: 'REAL_ESTATE' } });
      fireEvent.change(screen.getByPlaceholderText(/e\.g\. 500000/i), {
        target: { name: 'current_value', value: '9500000' },
      });

      const submitBtn = screen
        .getAllByRole('button', { name: /Add Asset/i })
        .find((b) => b.type === 'submit');
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(createPhysicalAsset).toHaveBeenCalledWith(
          'p1',
          expect.objectContaining({
            asset_name: 'Bangalore Flat',
            asset_type: 'REAL_ESTATE',
            make: null,
            model: null,
            registration_number: null,
            registration_type: null,
            current_value: 9500000,
          })
        );
      });
    });

    it('shows VEHICLE-only fields in the Edit modal for a VEHICLE asset', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      fireEvent.click(screen.getAllByRole('button', { name: 'Edit asset' })[0]);

      expect(screen.getByText('PUC Expiry')).toBeInTheDocument();
      expect(screen.getByText('Insurance Expiry')).toBeInTheDocument();
      expect(screen.getByText('Road Tax Expiry')).toBeInTheDocument();
      expect(screen.getByText('Valuation Date')).toBeInTheDocument();
    });

    it('hides VEHICLE-only fields in the Edit modal for a non-VEHICLE asset', async () => {
      const realEstateAsset = {
        asset_id: 'asset3',
        asset_name: 'Bangalore Flat',
        asset_type: 'REAL_ESTATE',
        make: null,
        model: null,
        registration_number: null,
        registration_type: null,
        current_value: 9500000,
        valuation_date: '2026-07-01',
        is_active: true,
        metadata: {},
      };
      listPhysicalAssets.mockResolvedValue({ physical_assets: [realEstateAsset] });
      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
      await waitFor(() => screen.getByText('Bangalore Flat'));

      fireEvent.click(screen.getByRole('button', { name: 'Edit asset' }));

      expect(screen.queryByText('PUC Expiry')).not.toBeInTheDocument();
      expect(screen.queryByText('Insurance Expiry')).not.toBeInTheDocument();
      expect(screen.queryByText('Road Tax Expiry')).not.toBeInTheDocument();
      expect(screen.getByText('Current Value')).toBeInTheDocument();
      expect(screen.getByText('Valuation Date')).toBeInTheDocument();
    });

    it('submits edit for a non-VEHICLE asset carrying valuation_date through to updatePhysicalAsset', async () => {
      const realEstateAsset = {
        asset_id: 'asset3',
        asset_name: 'Bangalore Flat',
        asset_type: 'REAL_ESTATE',
        make: null,
        model: null,
        registration_number: null,
        registration_type: null,
        current_value: 9500000,
        valuation_date: '2026-07-01',
        is_active: true,
        metadata: {},
      };
      listPhysicalAssets.mockResolvedValue({ physical_assets: [realEstateAsset] });
      updatePhysicalAsset.mockResolvedValue({});
      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
      await waitFor(() => screen.getByText('Bangalore Flat'));

      fireEvent.click(screen.getByRole('button', { name: 'Edit asset' }));
      const submitBtn = screen
        .getAllByRole('button', { name: /Save Changes/i })
        .find((b) => b.type === 'submit');
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(updatePhysicalAsset).toHaveBeenCalledWith(
          'asset3',
          'p1',
          expect.objectContaining({
            valuation_date: '2026-07-01',
            make: null,
            model: null,
          })
        );
      });
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
