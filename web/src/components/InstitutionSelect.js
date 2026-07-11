import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import {
  FINANCIAL_INSTITUTIONS,
  OTHER_INSTITUTION_SENTINEL,
  isKnownInstitution,
} from '../utils/constants';

/**
 * Institution picker: a curated dropdown of standard Indian banks/AMCs, with
 * an "Other" option that reveals a free-text input — needed for cases the
 * closed list can't cover (e.g. an EPF account's institution_name is really
 * an employer name, which isn't enumerable the way bank names are).
 */
export function InstitutionSelect({ name, value, onChange, className }) {
  const [showOther, setShowOther] = useState(value !== '' && !isKnownInstitution(value));

  // Only force "Other" mode ON when an external/initial value doesn't match
  // the list (e.g. editing an account with a custom institution). Never force
  // it back OFF just because value is empty — that happens transiently right
  // after the user picks "Other" (value resets to '' for them to type into)
  // and while they're clearing the text box to retype, and resetting would
  // yank the input away from under them both times.
  useEffect(() => {
    if (value !== '' && !isKnownInstitution(value)) {
      setShowOther(true);
    }
  }, [value]);

  const handleSelectChange = (e) => {
    const selected = e.target.value;
    if (selected === OTHER_INSTITUTION_SENTINEL) {
      setShowOther(true);
      onChange({ target: { name, value: '' } });
    } else {
      setShowOther(false);
      onChange({ target: { name, value: selected } });
    }
  };

  return (
    <>
      <select
        name={name}
        value={showOther ? OTHER_INSTITUTION_SENTINEL : value}
        onChange={handleSelectChange}
        className={className}
      >
        <option value="">Select institution</option>
        {Object.entries(FINANCIAL_INSTITUTIONS).map(([group, options]) => (
          <optgroup key={group} label={group}>
            {options.map((opt) => (
              <option key={opt} value={opt}>
                {opt}
              </option>
            ))}
          </optgroup>
        ))}
        <option value={OTHER_INSTITUTION_SENTINEL}>Other (type below)</option>
      </select>
      {showOther && (
        <input
          type="text"
          name={name}
          value={value}
          onChange={onChange}
          placeholder="e.g. employer name for EPF, or an institution not listed"
          className={`${className} mt-2`}
        />
      )}
    </>
  );
}
InstitutionSelect.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  className: PropTypes.string,
};
InstitutionSelect.defaultProps = { className: '' };

export default InstitutionSelect;
