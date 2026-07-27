import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Setup } from './Setup';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../../api/admins', () => ({
  createAdmin: jest.fn(),
}));

jest.mock('../../api/profiles', () => ({
  createProfile: jest.fn(),
  updateAdminPolicy: jest.fn(),
}));

jest.mock('../../api/wealth', () => ({
  createAccount: jest.fn(),
  updateAccountClassification: jest.fn(),
}));

jest.mock('../../api/health', () => ({
  recordVital: jest.fn(),
}));

const mockUpdateUser = jest.fn();
jest.mock('../../hooks/useAuth', () => ({
  useAuth: jest.fn(),
}));

const { createAdmin } = require('../../api/admins');
const { createProfile, updateAdminPolicy } = require('../../api/profiles');
const { createAccount, updateAccountClassification } = require('../../api/wealth');
const { recordVital } = require('../../api/health');
const { useAuth } = require('../../hooks/useAuth');

function renderSetup() {
  return render(
    <MemoryRouter>
      <Setup />
    </MemoryRouter>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  useAuth.mockReturnValue({
    user: { username: 'alice', role: 'admin' },
    updateUser: mockUpdateUser,
  });
});

describe('Setup wizard', () => {
  it('renders step 1 (identity) first for a brand-new admin', () => {
    renderSetup();
    expect(screen.getByText('1. Your Details')).toBeInTheDocument();
    expect(screen.getByText('Full Name')).toBeInTheDocument();
  });

  it('starts at step 2 when the admin already has an admin_id (revisit)', () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1', profile_id: 'profile-1' },
      updateUser: mockUpdateUser,
    });
    renderSetup();
    expect(screen.getByText('Account Name')).toBeInTheDocument();
  });

  it('step 1 shows an error when full name is missing', () => {
    renderSetup();
    fireEvent.click(screen.getByText('Save and Continue'));
    expect(screen.getByText('Full name is required')).toBeInTheDocument();
    expect(createAdmin).not.toHaveBeenCalled();
  });

  it('step 1 creates admin + SELF profile and advances to step 2', async () => {
    createAdmin.mockResolvedValue({ admin_id: 'admin-1' });
    createProfile.mockResolvedValue({ profile_id: 'profile-1' });
    renderSetup();

    fireEvent.change(screen.getByPlaceholderText('e.g. Ketan Verma'), {
      target: { value: 'Ketan Verma' },
    });
    const dobInput = document.querySelector('input[name="dob"]');
    fireEvent.change(dobInput, { target: { value: '1990-01-01' } });
    fireEvent.click(screen.getByText('Save and Continue'));

    await waitFor(() => expect(screen.getByText('Account Name')).toBeInTheDocument());

    expect(createAdmin).toHaveBeenCalledWith(
      expect.objectContaining({ display_name: 'Ketan Verma' })
    );
    expect(createProfile).toHaveBeenCalledWith(
      expect.objectContaining({
        admin_id: 'admin-1',
        full_name: 'Ketan Verma',
        dob: '1990-01-01',
        relation_to_admin: 'SELF',
      })
    );
    expect(mockUpdateUser).toHaveBeenCalledWith({ admin_id: 'admin-1', profile_id: 'profile-1' });
  });

  it('step 2 adds a liquid account and lists it', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1', profile_id: 'profile-1' },
      updateUser: mockUpdateUser,
    });
    createAccount.mockResolvedValue({
      account_id: 'acc-1',
      account_name: 'HDFC Savings',
      account_type: 'SAVINGS',
    });
    renderSetup();

    fireEvent.change(screen.getByPlaceholderText('e.g. SBI Savings'), {
      target: { value: 'HDFC Savings' },
    });
    fireEvent.change(document.querySelector('select[name="institution_name"]'), {
      target: { value: 'HDFC Bank' },
    });
    fireEvent.click(screen.getByText('+ Add Account'));

    await waitFor(() =>
      expect(createAccount).toHaveBeenCalledWith(
        'profile-1',
        expect.objectContaining({
          account_name: 'HDFC Savings',
          institution_name: 'HDFC Bank',
        })
      )
    );
    expect(await screen.findByText(/HDFC Savings/)).toBeInTheDocument();
    expect(updateAccountClassification).not.toHaveBeenCalled();
  });

  it('step 2 loan account also patches classification', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1', profile_id: 'profile-1' },
      updateUser: mockUpdateUser,
    });
    createAccount.mockResolvedValue({
      account_id: 'acc-2',
      account_name: 'Home Loan',
      account_type: 'HOME_LOAN',
    });
    updateAccountClassification.mockResolvedValue({});
    renderSetup();

    fireEvent.change(screen.getByPlaceholderText('e.g. SBI Savings'), {
      target: { value: 'Home Loan' },
    });
    fireEvent.change(document.querySelector('select[name="account_type"]'), {
      target: { value: 'HOME_LOAN' },
    });
    fireEvent.change(document.querySelector('select[name="institution_name"]'), {
      target: { value: 'HDFC Bank' },
    });
    fireEvent.change(screen.getByPlaceholderText('e.g. 5000000'), {
      target: { value: '4000000' },
    });
    fireEvent.click(screen.getByText('+ Add Account'));

    await waitFor(() =>
      expect(updateAccountClassification).toHaveBeenCalledWith(
        'acc-2',
        'profile-1',
        expect.objectContaining({ loan_original_principal: '4000000' })
      )
    );
  });

  it('step 2 can be skipped straight to step 3', () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1', profile_id: 'profile-1' },
      updateUser: mockUpdateUser,
    });
    renderSetup();
    fireEvent.click(screen.getByText('Skip for now'));
    expect(screen.getByText('Weight')).toBeInTheDocument();
    expect(screen.getByText('Height')).toBeInTheDocument();
  });

  it('step 3 records a weight vital and finish completes setup', async () => {
    useAuth.mockReturnValue({
      user: { username: 'alice', role: 'admin', admin_id: 'admin-1', profile_id: 'profile-1' },
      updateUser: mockUpdateUser,
    });
    recordVital.mockResolvedValue({});
    updateAdminPolicy.mockResolvedValue({});
    renderSetup();
    fireEvent.click(screen.getByText('Skip for now'));

    const [weightDate] = document.querySelectorAll('input[type="date"]');
    fireEvent.change(weightDate, { target: { value: '2026-07-01' } });
    fireEvent.change(screen.getByPlaceholderText('kg'), { target: { value: '70' } });
    fireEvent.click(screen.getAllByText('Add')[0]);

    await waitFor(() =>
      expect(recordVital).toHaveBeenCalledWith(
        expect.objectContaining({
          profile_id: 'profile-1',
          vital_type: 'WEIGHT',
          value_primary: 70,
          unit: 'kg',
        })
      )
    );

    fireEvent.click(screen.getByText('Finish Setup'));

    await waitFor(() =>
      expect(updateAdminPolicy).toHaveBeenCalledWith('admin-1', { setup_completed: 'true' })
    );
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/dashboard'));
  });
});
