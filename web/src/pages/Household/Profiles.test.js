import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Profiles } from './Profiles';

jest.mock('../../api/profiles');
jest.mock('../../api/admins');

const profilesApi = require('../../api/profiles');
const adminsApi = require('../../api/admins');

const MOCK_PROFILES = [
  {
    profile_id: 'p1',
    full_name: 'Alice Smith',
    relation_to_admin: 'SELF',
    dob: '1990-01-15',
    email_address: 'alice@example.com',
    gender: 'FEMALE',
    blood_type: 'A+',
    is_active: true,
  },
  {
    profile_id: 'p2',
    full_name: 'Bob Smith',
    relation_to_admin: 'SPOUSE',
    dob: '1988-06-20',
    email_address: null,
    gender: 'MALE',
    blood_type: null,
    is_active: false,
  },
];

const MOCK_ADMINS = [{ admin_id: 'a1', display_name: 'Admin One' }];

describe('Profiles page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows loading state initially', () => {
    profilesApi.listProfiles.mockReturnValue(new Promise(() => {}));
    adminsApi.listAdmins.mockReturnValue(new Promise(() => {}));
    render(<Profiles />);
    expect(screen.getByText('Loading profiles...')).toBeInTheDocument();
  });

  it('renders profile cards after data loads', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });
    expect(screen.getByText('Bob Smith')).toBeInTheDocument();
  });

  it('shows error message on API failure', async () => {
    profilesApi.listProfiles.mockRejectedValue(new Error('Network error'));
    adminsApi.listAdmins.mockResolvedValue({ admins: [] });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('shows empty state with Add Profile link when no profiles', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: [] });
    adminsApi.listAdmins.mockResolvedValue({ admins: [] });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText(/No profiles found/)).toBeInTheDocument();
    });
  });

  it('"+ Add Profile" button opens the create modal', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: '+ Add Profile' }));
    expect(screen.getByRole('heading', { name: 'Add Profile' })).toBeInTheDocument();
  });

  it('modal contains required form fields', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: [] });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.queryByText('Loading profiles...')).not.toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: '+ Add Profile' }));

    expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument();
    expect(screen.getByText('Full Name')).toBeInTheDocument();
    expect(screen.getByText('Date of Birth')).toBeInTheDocument();
    expect(screen.getByText('Relation to Admin')).toBeInTheDocument();
  });

  it('modal closes when Cancel is clicked', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: [] });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.queryByText('Loading profiles...')).not.toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: '+ Add Profile' }));
    expect(screen.getByRole('heading', { name: 'Add Profile' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByPlaceholderText('Full name')).not.toBeInTheDocument();
  });

  it('shows validation error when required fields missing on submit', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: [] });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.queryByText('Loading profiles...')).not.toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: '+ Add Profile' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add Profile' }));

    await waitFor(() => {
      expect(screen.getByText('Please select an admin')).toBeInTheDocument();
    });
  });

  it('Edit button opens edit modal with profile data', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    await userEvent.click(editButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Edit — Alice Smith/i })).toBeInTheDocument();
    });
  });

  it('edit modal closes when X button clicked', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    await userEvent.click(editButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Edit — Alice Smith/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: '×' }));
    expect(screen.queryByRole('heading', { name: /Edit — Alice Smith/i })).not.toBeInTheDocument();
  });

  it('saves edit successfully and reloads', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    profilesApi.updateProfile.mockResolvedValue({});
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    await userEvent.click(editButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Edit — Alice Smith/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(profilesApi.updateProfile).toHaveBeenCalledWith(
        'p1',
        expect.objectContaining({ email_address: 'alice@example.com' })
      );
    });
  });

  it('Deactivate button opens confirmation dialog', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    const deactivateButtons = screen.getAllByRole('button', { name: 'Deactivate' });
    await userEvent.click(deactivateButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Deactivate Profile')).toBeInTheDocument();
    });
  });

  it('confirms deactivation and reloads profiles', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    profilesApi.deactivateProfile.mockResolvedValue(null);
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    const deactivateButtons = screen.getAllByRole('button', { name: 'Deactivate' });
    await userEvent.click(deactivateButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Deactivate Profile')).toBeInTheDocument();
    });

    // Click the Deactivate button inside the confirmation dialog (not the profile card button)
    const allDeactivateButtons = screen.getAllByRole('button', { name: 'Deactivate' });
    const confirmBtn = allDeactivateButtons[allDeactivateButtons.length - 1];
    await userEvent.click(confirmBtn);

    await waitFor(() => {
      expect(profilesApi.deactivateProfile).toHaveBeenCalledWith('p1');
    });
  });

  it('filter buttons change the active filter', async () => {
    profilesApi.listProfiles.mockResolvedValue({ profiles: MOCK_PROFILES });
    adminsApi.listAdmins.mockResolvedValue({ admins: MOCK_ADMINS });
    render(<Profiles />);

    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Active' }));
    expect(profilesApi.listProfiles).toHaveBeenCalledWith(null, true);
  });
});
