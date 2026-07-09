import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { createAdmin } from '../../api/admins';
import { createProfile, updateAdminPolicy } from '../../api/profiles';
import { createAccount, updateAccountClassification } from '../../api/wealth';
import { recordVital } from '../../api/health';
import { Field } from '../../components/Field';

const inputClass =
  'border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500';

const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
const GENDERS = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'];
const ACCOUNT_TYPES = [
  'SAVINGS',
  'CURRENT',
  'CREDIT_CARD',
  'HOME_LOAN',
  'PERSONAL_LOAN',
  'CAR_LOAN',
  'MUTUAL_FUND',
  'NPS',
  'PPF',
  'FD',
];
const LOAN_TYPES = new Set(['HOME_LOAN', 'PERSONAL_LOAN', 'CAR_LOAN']);

const EMPTY_IDENTITY = { fullName: '', dob: '', email: '', gender: '', bloodType: '' };
const EMPTY_ACCOUNT = {
  account_name: '',
  account_type: 'SAVINGS',
  institution_name: '',
  opening_balance: '',
  loan_original_principal: '',
  loan_start_date: '',
  loan_tenure_months: '',
};
const EMPTY_VITAL_ENTRY = { reading_date: '', value: '' };

function StepIndicator({ step }) {
  const steps = ['Your Details', 'Wealth (optional)', 'Health (optional)'];
  return (
    <div className="flex gap-2 mb-6">
      {steps.map((label, idx) => {
        const num = idx + 1;
        const active = step === num;
        const done = step > num;
        let stepClass = 'bg-gray-100 text-gray-500';
        if (active) {
          stepClass = 'bg-blue-600 text-white';
        } else if (done) {
          stepClass = 'bg-green-100 text-green-700';
        }
        return (
          <div
            key={label}
            className={`flex-1 text-center py-2 rounded text-sm font-medium ${stepClass}`}
          >
            {num}. {label}
          </div>
        );
      })}
    </div>
  );
}
StepIndicator.propTypes = {
  step: PropTypes.number.isRequired,
};

export const Setup = () => {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState(user?.admin_id ? 2 : 1);
  const [adminId, setAdminId] = useState(user?.admin_id ?? null);
  const [profileId, setProfileId] = useState(user?.profile_id ?? null);

  // Step 1 — identity
  const [identity, setIdentity] = useState(EMPTY_IDENTITY);
  const [identityError, setIdentityError] = useState(null);
  const [identitySaving, setIdentitySaving] = useState(false);

  const handleIdentityChange = useCallback((e) => {
    const { name, value } = e.target;
    setIdentity((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleIdentitySubmit = useCallback(
    async (e) => {
      e.preventDefault();
      if (!identity.fullName.trim()) {
        setIdentityError('Full name is required');
        return;
      }
      if (!identity.dob) {
        setIdentityError('Date of birth is required');
        return;
      }
      setIdentitySaving(true);
      setIdentityError(null);
      try {
        const admin = await createAdmin({
          display_name: identity.fullName.trim(),
          email_address: identity.email || null,
        });
        const profile = await createProfile({
          admin_id: admin.admin_id,
          full_name: identity.fullName.trim(),
          dob: identity.dob,
          relation_to_admin: 'SELF',
          email_address: identity.email || null,
          gender: identity.gender || null,
          blood_type: identity.bloodType || null,
        });
        setAdminId(admin.admin_id);
        setProfileId(profile.profile_id);
        updateUser({ admin_id: admin.admin_id, profile_id: profile.profile_id });
        setStep(2);
      } catch (err) {
        setIdentityError(err.message || 'Failed to save your details');
      } finally {
        setIdentitySaving(false);
      }
    },
    [identity, updateUser]
  );

  // Step 2 — wealth (optional, repeatable)
  const [accountForm, setAccountForm] = useState(EMPTY_ACCOUNT);
  const [accountError, setAccountError] = useState(null);
  const [accountSaving, setAccountSaving] = useState(false);
  const [addedAccounts, setAddedAccounts] = useState([]);
  const isLoanType = LOAN_TYPES.has(accountForm.account_type);

  const handleAccountChange = useCallback((e) => {
    const { name, value } = e.target;
    setAccountForm((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleAddAccount = useCallback(
    async (e) => {
      e.preventDefault();
      if (!accountForm.account_name.trim()) {
        setAccountError('Account name is required');
        return;
      }
      if (!accountForm.institution_name.trim()) {
        setAccountError('Institution name is required');
        return;
      }
      setAccountSaving(true);
      setAccountError(null);
      try {
        const created = await createAccount(profileId, {
          account_name: accountForm.account_name.trim(),
          account_type: accountForm.account_type,
          institution_name: accountForm.institution_name.trim(),
          opening_balance:
            accountForm.opening_balance === '' ? 0 : Number(accountForm.opening_balance),
        });
        if (isLoanType) {
          const loanMeta = {};
          if (accountForm.loan_original_principal) {
            loanMeta.loan_original_principal = accountForm.loan_original_principal;
          }
          if (accountForm.loan_start_date) loanMeta.loan_start_date = accountForm.loan_start_date;
          if (accountForm.loan_tenure_months) {
            loanMeta.loan_tenure_months = accountForm.loan_tenure_months;
          }
          if (Object.keys(loanMeta).length > 0) {
            await updateAccountClassification(created.account_id, profileId, loanMeta);
          }
        }
        setAddedAccounts((prev) => [...prev, created]);
        setAccountForm(EMPTY_ACCOUNT);
      } catch (err) {
        setAccountError(err.message || 'Failed to add account');
      } finally {
        setAccountSaving(false);
      }
    },
    [accountForm, profileId, isLoanType]
  );

  // Step 3 — health (optional)
  const [weightForm, setWeightForm] = useState(EMPTY_VITAL_ENTRY);
  const [heightForm, setHeightForm] = useState(EMPTY_VITAL_ENTRY);
  const [vitalError, setVitalError] = useState(null);
  const [vitalSaving, setVitalSaving] = useState(false);
  const [addedVitals, setAddedVitals] = useState([]);

  const handleAddVital = useCallback(
    async (vitalType, form, setForm) => {
      if (!form.reading_date) {
        setVitalError('Reading date is required');
        return;
      }
      if (!form.value || Number(form.value) <= 0) {
        setVitalError('Value must be positive');
        return;
      }
      setVitalSaving(true);
      setVitalError(null);
      try {
        const unit = vitalType === 'WEIGHT' ? 'kg' : 'cm';
        await recordVital({
          profile_id: profileId,
          vital_type: vitalType,
          reading_date: form.reading_date,
          value_primary: Number(form.value),
          value_secondary: null,
          unit,
        });
        setAddedVitals((prev) => [...prev, { vitalType, value: form.value, unit }]);
        setForm(EMPTY_VITAL_ENTRY);
      } catch (err) {
        setVitalError(err.message || 'Failed to record vital');
      } finally {
        setVitalSaving(false);
      }
    },
    [profileId]
  );

  // Finish
  const [finishing, setFinishing] = useState(false);
  const [finishError, setFinishError] = useState(null);

  const handleFinish = useCallback(async () => {
    setFinishing(true);
    setFinishError(null);
    try {
      await updateAdminPolicy(adminId, { setup_completed: 'true' });
      navigate('/dashboard');
    } catch (err) {
      setFinishError(err.message || 'Failed to complete setup');
    } finally {
      setFinishing(false);
    }
  }, [adminId, navigate]);

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Household Setup</h1>
        <p className="text-gray-500 mt-1">
          One-time setup for your household. Your details are required; wealth and health entries
          are optional and can always be added later from this page or their normal pages.
        </p>
      </div>

      <StepIndicator step={step} />

      {step === 1 && (
        <form
          onSubmit={handleIdentitySubmit}
          className="space-y-4 bg-white border border-gray-200 rounded-lg p-5"
        >
          <Field label="Full Name" required>
            <input
              name="fullName"
              type="text"
              value={identity.fullName}
              onChange={handleIdentityChange}
              placeholder="e.g. Ketan Verma"
              className={inputClass}
            />
          </Field>
          <Field label="Date of Birth" required>
            <input
              name="dob"
              type="date"
              value={identity.dob}
              onChange={handleIdentityChange}
              className={inputClass}
            />
          </Field>
          <Field label="Blood Group">
            <select
              name="bloodType"
              value={identity.bloodType}
              onChange={handleIdentityChange}
              className={inputClass}
            >
              <option value="">Select (optional)</option>
              {BLOOD_TYPES.map((bt) => (
                <option key={bt} value={bt}>
                  {bt}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Gender">
            <select
              name="gender"
              value={identity.gender}
              onChange={handleIdentityChange}
              className={inputClass}
            >
              <option value="">Select (optional)</option>
              {GENDERS.map((g) => (
                <option key={g} value={g}>
                  {g.replaceAll('_', ' ')}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Email">
            <input
              name="email"
              type="email"
              value={identity.email}
              onChange={handleIdentityChange}
              placeholder="Optional"
              className={inputClass}
            />
          </Field>
          {identityError && <p className="text-red-600 text-sm">{identityError}</p>}
          <button
            type="submit"
            disabled={identitySaving}
            className="w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {identitySaving ? 'Saving...' : 'Save and Continue'}
          </button>
        </form>
      )}

      {step === 2 && (
        <div className="space-y-4">
          <form
            onSubmit={handleAddAccount}
            className="space-y-4 bg-white border border-gray-200 rounded-lg p-5"
          >
            <Field label="Account Name" required>
              <input
                name="account_name"
                type="text"
                value={accountForm.account_name}
                onChange={handleAccountChange}
                placeholder="e.g. SBI Savings"
                className={inputClass}
              />
            </Field>
            <Field label="Account Type" required>
              <select
                name="account_type"
                value={accountForm.account_type}
                onChange={handleAccountChange}
                className={inputClass}
              >
                {ACCOUNT_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.replaceAll('_', ' ')}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Institution Name" required>
              <input
                name="institution_name"
                type="text"
                value={accountForm.institution_name}
                onChange={handleAccountChange}
                placeholder="e.g. State Bank of India"
                className={inputClass}
              />
            </Field>
            <Field label={isLoanType ? 'Outstanding Amount' : 'Opening Balance'}>
              <input
                name="opening_balance"
                type="number"
                value={accountForm.opening_balance}
                onChange={handleAccountChange}
                placeholder="0"
                className={inputClass}
              />
            </Field>
            {isLoanType && (
              <>
                <Field label="Original Principal (₹)">
                  <input
                    name="loan_original_principal"
                    type="number"
                    value={accountForm.loan_original_principal}
                    onChange={handleAccountChange}
                    placeholder="e.g. 5000000"
                    className={inputClass}
                  />
                </Field>
                <Field label="Loan Start Date">
                  <input
                    name="loan_start_date"
                    type="date"
                    value={accountForm.loan_start_date}
                    onChange={handleAccountChange}
                    className={inputClass}
                  />
                </Field>
                <Field label="Tenure (months)">
                  <input
                    name="loan_tenure_months"
                    type="number"
                    min="1"
                    value={accountForm.loan_tenure_months}
                    onChange={handleAccountChange}
                    placeholder="e.g. 240"
                    className={inputClass}
                  />
                </Field>
              </>
            )}
            {accountError && <p className="text-red-600 text-sm">{accountError}</p>}
            <button
              type="submit"
              disabled={accountSaving}
              className="w-full border border-blue-600 text-blue-600 py-2 rounded-lg font-medium hover:bg-blue-50 disabled:opacity-50"
            >
              {accountSaving ? 'Adding...' : '+ Add Account'}
            </button>
          </form>

          {addedAccounts.length > 0 && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-sm text-green-800">
              <p className="font-semibold mb-1">Added so far:</p>
              <ul className="list-disc list-inside">
                {addedAccounts.map((a) => (
                  <li key={a.account_id}>
                    {a.account_name} ({a.account_type.replaceAll('_', ' ')})
                  </li>
                ))}
              </ul>
            </div>
          )}

          <button
            type="button"
            onClick={() => setStep(3)}
            className="w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700"
          >
            {addedAccounts.length > 0 ? 'Continue' : 'Skip for now'}
          </button>
        </div>
      )}

      {step === 3 && (
        <div className="space-y-4">
          <div className="bg-white border border-gray-200 rounded-lg p-5 space-y-3">
            <p className="font-semibold text-gray-800">Weight</p>
            <div className="flex gap-3">
              <input
                type="date"
                value={weightForm.reading_date}
                onChange={(e) => setWeightForm((p) => ({ ...p, reading_date: e.target.value }))}
                className={inputClass}
              />
              <input
                type="number"
                placeholder="kg"
                value={weightForm.value}
                onChange={(e) => setWeightForm((p) => ({ ...p, value: e.target.value }))}
                className={inputClass}
              />
              <button
                type="button"
                onClick={() => handleAddVital('WEIGHT', weightForm, setWeightForm)}
                disabled={vitalSaving}
                className="border border-blue-600 text-blue-600 px-4 rounded-lg font-medium hover:bg-blue-50 disabled:opacity-50"
              >
                Add
              </button>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-5 space-y-3">
            <p className="font-semibold text-gray-800">Height</p>
            <div className="flex gap-3">
              <input
                type="date"
                value={heightForm.reading_date}
                onChange={(e) => setHeightForm((p) => ({ ...p, reading_date: e.target.value }))}
                className={inputClass}
              />
              <input
                type="number"
                placeholder="cm"
                value={heightForm.value}
                onChange={(e) => setHeightForm((p) => ({ ...p, value: e.target.value }))}
                className={inputClass}
              />
              <button
                type="button"
                onClick={() => handleAddVital('HEIGHT', heightForm, setHeightForm)}
                disabled={vitalSaving}
                className="border border-blue-600 text-blue-600 px-4 rounded-lg font-medium hover:bg-blue-50 disabled:opacity-50"
              >
                Add
              </button>
            </div>
          </div>

          {vitalError && <p className="text-red-600 text-sm">{vitalError}</p>}

          {addedVitals.length > 0 && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-sm text-green-800">
              <p className="font-semibold mb-1">Recorded so far:</p>
              <ul className="list-disc list-inside">
                {addedVitals.map((v, idx) => (
                  <li key={`${v.vitalType}-${idx}`}>
                    {v.vitalType}: {v.value} {v.unit}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {finishError && <p className="text-red-600 text-sm">{finishError}</p>}
          <button
            type="button"
            onClick={handleFinish}
            disabled={finishing}
            className="w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {finishing ? 'Finishing...' : 'Finish Setup'}
          </button>
        </div>
      )}
    </div>
  );
};

export default Setup;
