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
  deactivatePhysicalAsset,
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

  describe('Add form validation', () => {
    async function openAddModal() {
      listPhysicalAssets.mockResolvedValue({ physical_assets: [] });
      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });
      await waitFor(() => screen.getByText('+ Add Asset'));
      fireEvent.click(screen.getByText('+ Add Asset'));
      await waitFor(() => screen.getByRole('heading', { name: 'Add Physical Asset' }));
    }

    function submitAddForm() {
      const submitBtn = screen
        .getAllByRole('button', { name: /Add Asset/i })
        .find((b) => b.type === 'submit');
      fireEvent.click(submitBtn);
    }

    it('requires an asset name', async () => {
      await openAddModal();
      submitAddForm();
      await waitFor(() => {
        expect(screen.getByText(/asset name is required/i)).toBeInTheDocument();
      });
      expect(createPhysicalAsset).not.toHaveBeenCalled();
    });

    it('requires make/model/registration for a VEHICLE asset', async () => {
      await openAddModal();
      fireEvent.change(screen.getByPlaceholderText(/e\.g\. family car/i), {
        target: { value: 'My Bike' },
      });
      submitAddForm();
      await waitFor(() => {
        expect(screen.getByText(/make is required/i)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(/e\.g\. maruti/i), {
        target: { value: 'Honda' },
      });
      submitAddForm();
      await waitFor(() => {
        expect(screen.getByText(/model is required/i)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(/e\.g\. swift/i), {
        target: { value: 'Activa' },
      });
      submitAddForm();
      await waitFor(() => {
        expect(screen.getByText(/registration number is required/i)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(/ka-01-ab-1234/i), {
        target: { value: 'KA-05-XY-9999' },
      });
      submitAddForm();
      await waitFor(() => {
        expect(screen.getByText(/registration type is required/i)).toBeInTheDocument();
      });
      expect(createPhysicalAsset).not.toHaveBeenCalled();
    });

    it('shows an error banner when creating an asset fails', async () => {
      createPhysicalAsset.mockRejectedValue(new Error('create asset failed'));
      await openAddModal();
      fireEvent.change(screen.getByPlaceholderText(/e\.g\. family car/i), {
        target: { value: 'Gold Bond' },
      });
      const typeSelect = screen
        .getAllByRole('combobox')
        .find((s) => s.getAttribute('name') === 'asset_type');
      fireEvent.change(typeSelect, { target: { value: 'GOLD_BOND' } });
      submitAddForm();
      await waitFor(() => {
        expect(screen.getByText('create asset failed')).toBeInTheDocument();
      });
    });

    it('closes the add modal via Cancel without saving', async () => {
      await openAddModal();
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      await waitFor(() => {
        expect(
          screen.queryByRole('heading', { name: 'Add Physical Asset' })
        ).not.toBeInTheDocument();
      });
      expect(createPhysicalAsset).not.toHaveBeenCalled();
    });
  });

  describe('Active filter', () => {
    it('requests assets filtered by Active / Inactive / All', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      fireEvent.click(screen.getByRole('button', { name: 'Active' }));
      await waitFor(() => {
        expect(listPhysicalAssets).toHaveBeenLastCalledWith('p1', null, true, 0, 20);
      });

      fireEvent.click(screen.getByRole('button', { name: 'Inactive' }));
      await waitFor(() => {
        expect(listPhysicalAssets).toHaveBeenLastCalledWith('p1', null, false, 0, 20);
      });

      fireEvent.click(screen.getByRole('button', { name: 'All' }));
      await waitFor(() => {
        expect(listPhysicalAssets).toHaveBeenLastCalledWith('p1', null, null, 0, 20);
      });
    });
  });

  describe('Edit submit flow', () => {
    it('submits the edit form and reloads assets', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      updatePhysicalAsset.mockResolvedValue({});
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);

      const nameInput = await screen.findByDisplayValue('Family Car');
      fireEvent.change(nameInput, { target: { value: 'Family Car Updated' } });
      fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(updatePhysicalAsset).toHaveBeenCalledWith(
          'asset1',
          'p1',
          expect.objectContaining({ asset_name: 'Family Car Updated' })
        );
      });
    });

    it('shows an error banner when updating an asset fails', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      updatePhysicalAsset.mockRejectedValue(new Error('update asset failed'));
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);

      await screen.findByDisplayValue('Family Car');
      fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(screen.getByText('update asset failed')).toBeInTheDocument();
      });
    });

    it('closes the edit modal via Cancel without saving', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);

      await screen.findByDisplayValue('Family Car');
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByDisplayValue('Family Car')).not.toBeInTheDocument();
      });
      expect(updatePhysicalAsset).not.toHaveBeenCalled();
    });

    it('shows an error banner when reactivation fails', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      updatePhysicalAsset.mockRejectedValue(new Error('reactivate asset failed'));
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[1]);

      await waitFor(() => screen.getByText('Reactivate asset'));
      fireEvent.click(screen.getByText('Reactivate asset'));

      await waitFor(() => {
        expect(screen.getByText('reactivate asset failed')).toBeInTheDocument();
      });
    });
  });

  describe('Deactivate confirmation flow', () => {
    it('cancels the deactivate confirmation without calling the API', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);
      await waitFor(() => screen.getByText('Deactivate this asset'));
      fireEvent.click(screen.getByText('Deactivate this asset'));

      await waitFor(() => screen.getByRole('heading', { name: 'Deactivate Asset' }));
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByRole('heading', { name: 'Deactivate Asset' })).not.toBeInTheDocument();
      });
      expect(deactivatePhysicalAsset).not.toHaveBeenCalled();
    });

    it('confirms deactivation and reloads assets', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      deactivatePhysicalAsset.mockResolvedValue({});
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);
      await waitFor(() => screen.getByText('Deactivate this asset'));
      fireEvent.click(screen.getByText('Deactivate this asset'));

      await waitFor(() => screen.getByRole('heading', { name: 'Deactivate Asset' }));
      fireEvent.click(screen.getByRole('button', { name: 'Deactivate' }));

      await waitFor(() => {
        expect(deactivatePhysicalAsset).toHaveBeenCalledWith('asset1', 'p1');
      });
    });

    it('shows an error banner when deactivation fails', async () => {
      listPhysicalAssets.mockResolvedValue({ physical_assets: MOCK_ASSETS });
      deactivatePhysicalAsset.mockRejectedValue(new Error('deactivate asset failed'));
      await selectProfileAndWaitForAssets();

      const editButtons = screen.getAllByRole('button', { name: /edit asset/i });
      fireEvent.click(editButtons[0]);
      await waitFor(() => screen.getByText('Deactivate this asset'));
      fireEvent.click(screen.getByText('Deactivate this asset'));

      await waitFor(() => screen.getByRole('heading', { name: 'Deactivate Asset' }));
      fireEvent.click(screen.getByRole('button', { name: 'Deactivate' }));

      await waitFor(() => {
        expect(screen.getByText('deactivate asset failed')).toBeInTheDocument();
      });
    });
  });

  describe('Profile load failure', () => {
    it('falls back to an empty profile list when listProfiles fails', async () => {
      listProfiles.mockRejectedValue(new Error('profiles down'));
      render(<PhysicalAssets />);
      await waitFor(() => {
        expect(screen.getByText(/Select a profile to view assets/i)).toBeInTheDocument();
      });
      expect(screen.queryByText('Alice')).not.toBeInTheDocument();
    });
  });

  describe('Compliance expiry states', () => {
    it('shows Expired for a past date and a Due-in warning for a near-future date', async () => {
      const now = Date.now();
      const expiredDate = new Date(now - 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
      const dueSoonDate = new Date(now + 10 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
      listPhysicalAssets.mockResolvedValue({
        physical_assets: [
          {
            asset_id: 'asset3',
            asset_name: 'Expiry Test Car',
            asset_type: 'VEHICLE',
            make: 'Maruti',
            model: 'Swift',
            registration_number: 'KA-01-ZZ-0001',
            registration_type: 'PRIVATE',
            is_active: true,
            metadata: { puc_expiry: expiredDate, insurance_expiry: dueSoonDate },
          },
        ],
      });
      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => {
        expect(screen.getByText('Expired')).toBeInTheDocument();
        expect(screen.getByText(/Due in \d+d/)).toBeInTheDocument();
      });
    });

    it('renders a non-VEHICLE asset without vehicle-only fields or compliance rows', async () => {
      listPhysicalAssets.mockResolvedValue({
        physical_assets: [
          {
            asset_id: 'asset4',
            asset_name: 'Gold Bond 2030',
            asset_type: 'GOLD_BOND',
            current_value: 250000,
            valuation_date: '2026-06-01',
            is_active: true,
            metadata: {},
          },
        ],
      });
      render(<PhysicalAssets />);
      await waitFor(() => screen.getByText('Alice'));
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p1' } });

      await waitFor(() => {
        expect(screen.getByText('Gold Bond 2030')).toBeInTheDocument();
      });
      expect(screen.getByText(/Current Value: 2,50,000 \(as of 2026-06-01\)/)).toBeInTheDocument();
      expect(screen.queryByText(/Registration:/)).not.toBeInTheDocument();
      expect(screen.queryByText(/PUC:/)).not.toBeInTheDocument();
    });
  });
});
